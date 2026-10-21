package io.atlas.modules.lobby.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import io.atlas.AtlasMod;
import io.atlas.mixin.ServerCommonConnectionAccessor;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DevTransferService {
    public static final ResourceLocation COOKIE = ResourceLocation.fromNamespaceAndPath("atlas", "dev_access");
    private static final Gson GSON = new Gson();
    private static final DevTicketLedger TICKETS = new DevTicketLedger(Clock.systemUTC());
    private static final Map<Connection, String> HOSTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Connection, DevTicketLedger.Identity> ADMISSIONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static HttpServer broker;
    private static ExecutorService executor;
    private static int ticks;
    private static boolean checking;
    private static int idleTicks;
    private static CompletableFuture<Void> starting;

    private DevTransferService() {}

    public static void rememberHost(Connection connection, String host) {
        String clean = host.split("\u0000", 2)[0];
        if (!clean.isBlank() && clean.length() <= 253) HOSTS.put(connection, clean);
    }

    public static void admit(Connection connection, DevTicketLedger.Identity identity) {
        ADMISSIONS.put(connection, identity);
    }

    public static DevTicketLedger.Identity identity(UUID uuid) {
        synchronized (ADMISSIONS) {
            return ADMISSIONS.entrySet().stream().filter(entry -> entry.getKey().isConnected())
                    .map(Map.Entry::getValue).filter(identity -> identity.uuid().equals(uuid)).findFirst().orElse(null);
        }
    }

    public static void forget(ServerPlayer player) { ADMISSIONS.remove(connection(player)); }

    public static Connection connection(ServerPlayer player) {
        return ((ServerCommonConnectionAccessor) player.connection).atlas$getConnection();
    }

    public static CompletableFuture<DevTicketLedger.Identity> claim(byte[] cookie, String name) {
        if (cookie == null || cookie.length != 43) {
            AtlasMod.LOGGER.warn("Entrada dev de {} recusada: cookie {}.", name, cookie == null ? "ausente" : "com tamanho inválido");
            return CompletableFuture.failedFuture(new IllegalArgumentException());
        }
        JsonObject request = new JsonObject();
        request.addProperty("ticket", new String(cookie, StandardCharsets.US_ASCII));
        request.addProperty("username", name);
        return call("claim", request).thenApply(json -> new DevTicketLedger.Identity(
                UUID.fromString(json.get("uuid").getAsString()), json.get("username").getAsString(), json.get("role").getAsString()));
    }

    private static CompletableFuture<JsonObject> call(String action, JsonObject body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + DevEnvironment.BROKER_PORT + "/" + action))
                .timeout(Duration.ofSeconds(4)).header("Authorization", DevEnvironment.KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("Autorização dev recusada.");
            return GSON.fromJson(response.body(), JsonObject.class);
        });
    }

    public static void start(MinecraftServer server) {
        if (!DevEnvironment.enabled()) return;
        if (DevEnvironment.DEVELOPMENT) {
            var world = server.getLevel(LobbyWorlds.EMERALD);
            if (world == null) throw new IllegalStateException("Homologação sem dimensão do lobby.");
            // Criar uma plataforma inicial apenas no mundo novo de testes.
            if (world.getBlockState(new BlockPos(0, 63, 0)).isAir()) {
                for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                    world.setBlockAndUpdate(new BlockPos(x, 63, z), Blocks.STONE_BRICKS.defaultBlockState());
                }
                world.setBlockAndUpdate(new BlockPos(0, 62, 0), Blocks.BEDROCK.defaultBlockState());
            }
            AtlasMod.LOGGER.info("Lobby de testes ativo: dados locais, acesso por autorização do servidor principal.");
            return;
        }
        try {
            broker = HttpServer.create(new InetSocketAddress("127.0.0.1", DevEnvironment.BROKER_PORT), 16);
            executor = Executors.newFixedThreadPool(2);
            broker.setExecutor(executor);
            broker.createContext("/", exchange -> {
                int status = 403;
                byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
                try {
                    String key = exchange.getRequestHeaders().getFirst("Authorization");
                    if (!"POST".equals(exchange.getRequestMethod()) || key == null
                            || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), DevEnvironment.KEY.getBytes(StandardCharsets.UTF_8))) {
                        exchange.sendResponseHeaders(status, response.length);
                        exchange.getResponseBody().write(response);
                        return;
                    }
                    byte[] bytes = exchange.getRequestBody().readNBytes(2049);
                    if (bytes.length > 2048) throw new IllegalArgumentException();
                    JsonObject request = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
                    CompletableFuture<JsonObject> result = new CompletableFuture<>();
                    String action = exchange.getRequestURI().getPath();
                    server.execute(() -> {
                        try {
                            JsonObject json = new JsonObject();
                            if (action.equals("/claim")) {
                                var identity = TICKETS.consume(request.get("ticket").getAsString(), request.get("username").getAsString(),
                                        DevLobbyAccessService::isAuthorized).orElseThrow();
                                json.addProperty("uuid", identity.uuid().toString());
                                json.addProperty("username", identity.username());
                                json.addProperty("role", role(identity.uuid()));
                            } else if (action.equals("/verify")) {
                                var identities = request.getAsJsonArray("players");
                                if (identities.size() > 20) throw new IllegalArgumentException();
                                for (var value : identities) {
                                    UUID uuid = UUID.fromString(value.getAsString());
                                    json.addProperty(uuid.toString(), DevLobbyAccessService.isAuthorized(uuid) ? role(uuid) : "denied");
                                }
                            } else throw new IllegalArgumentException();
                            result.complete(json);
                        } catch (Exception error) { result.completeExceptionally(error); }
                    });
                    response = GSON.toJson(result.get(3, TimeUnit.SECONDS)).getBytes(StandardCharsets.UTF_8);
                    status = 200;
                } catch (Exception error) {
                    AtlasMod.LOGGER.warn("Broker dev recusou {}: {}.", exchange.getRequestURI().getPath(), error.getClass().getSimpleName());
                    status = 403;
                }
                finally {
                    if (exchange.getResponseCode() == -1) {
                        exchange.sendResponseHeaders(status, response.length);
                        exchange.getResponseBody().write(response);
                    }
                    exchange.close();
                }
            });
            broker.start();
            AtlasMod.LOGGER.info("Broker de acesso ao lobby dev ativo apenas em loopback.");
        } catch (Exception error) {
            stop();
            throw new IllegalStateException("Não foi possível iniciar o broker dev.", error);
        }
    }

    private static String role(UUID uuid) {
        if (RankModule.getRankService().isOwner(uuid)) return "owner";
        return RankModule.getRankService().canManageRanks(uuid) ? "admin" : "member";
    }

    public static int enter(ServerPlayer player) {
        if (!DevEnvironment.enabled()) {
            player.sendSystemMessage(Component.literal("§eO lobby de testes ainda está em preparação."));
            return 0;
        }
        if (DevEnvironment.DEVELOPMENT) {
            player.sendSystemMessage(Component.literal("§eVocê já está no ambiente de testes. Use /lobby emerald para voltar."));
            return 0;
        }
        String host = DevEnvironment.DEV_HOST.isBlank() ? HOSTS.get(connection(player)) : DevEnvironment.DEV_HOST;
        if (host == null || host.isBlank()) {
            player.sendSystemMessage(Component.literal("§cEndereço do lobby de testes indisponível. Reconecte e tente novamente."));
            return 0;
        }
        var server = player.getServer();
        player.sendSystemMessage(Component.literal("§ePreparando o lobby de testes. A primeira entrada pode levar até dois minutos."));
        ensureReady().whenComplete((ignored, error) -> server.execute(() -> {
            if (!connection(player).isConnected()) return;
            if (error != null) {
                player.sendSystemMessage(Component.literal("§cO lobby de testes está temporariamente indisponível."));
                return;
            }
            if (!new DevLobbyAccessService().canAccess(player)) return;
            String token = TICKETS.issue(new DevTicketLedger.Identity(player.getUUID(), player.getGameProfile().getName(), role(player.getUUID())));
            AtlasMod.LOGGER.info("Transferência dev autorizada para {}; destino {}:{}.", player.getGameProfile().getName(), host, DevEnvironment.DEV_PUBLIC_PORT);
            player.closeContainer();
            player.sendSystemMessage(Component.literal("§aConectando ao lobby de testes. Seus dados do Emerald ficam no servidor principal."));
            player.connection.send(new ClientboundStoreCookiePacket(COOKIE, token.getBytes(StandardCharsets.US_ASCII)));
            player.connection.send(new ClientboundTransferPacket(host, DevEnvironment.DEV_PUBLIC_PORT));
        }));
        return 1;
    }

    private static synchronized CompletableFuture<Void> ensureReady() {
        if (starting != null && !starting.isDone()) return starting;
        starting = CompletableFuture.runAsync(() -> {
            try {
                var builder = new ProcessBuilder("systemctl", "--user", "start", "atlas-dev.service")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
                builder.environment().put("XDG_RUNTIME_DIR", DevEnvironment.USER_RUNTIME_DIR);
                builder.environment().put("DBUS_SESSION_BUS_ADDRESS", "unix:path=" + DevEnvironment.USER_RUNTIME_DIR + "/bus");
                var process = builder.start();
                if (!process.waitFor(15, TimeUnit.SECONDS) || process.exitValue() != 0) {
                    process.destroy();
                    throw new IllegalStateException("Serviço de testes não iniciou.");
                }
                for (int attempt = 0; attempt < 120; attempt++) {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("127.0.0.1", DevEnvironment.DEV_PORT), 500);
                        return;
                    } catch (java.io.IOException notReady) { Thread.sleep(1000); }
                }
                throw new IllegalStateException("Tempo de inicialização de testes excedido.");
            } catch (Exception error) { throw new IllegalStateException("Lobby de testes indisponível.", error); }
        });
        return starting;
    }

    public static int returnToMain(ServerPlayer player) {
        String host = DevEnvironment.MAIN_HOST.isBlank() ? HOSTS.get(connection(player)) : DevEnvironment.MAIN_HOST;
        if (host == null || host.isBlank()) return 0;
        player.closeContainer();
        player.connection.send(new ClientboundStoreCookiePacket(COOKIE, new byte[0]));
        player.connection.send(new ClientboundTransferPacket(host, DevEnvironment.MAIN_PUBLIC_PORT));
        return 1;
    }

    public static void tick(MinecraftServer server) {
        if (!DevEnvironment.DEVELOPMENT) return;
        if (server.getPlayerList().getPlayerCount() == 0) {
            if (++idleTicks >= 2400) {
                AtlasMod.LOGGER.info("Lobby de testes vazio por dois minutos; salvando e encerrando a instância.");
                server.halt(false);
            }
            return;
        }
        idleTicks = 0;
        if (++ticks % 200 != 0 || checking) return;
        var players = java.util.List.copyOf(server.getPlayerList().getPlayers());
        var list = new com.google.gson.JsonArray();
        players.forEach(player -> list.add(player.getUUID().toString()));
        JsonObject request = new JsonObject();
        request.add("players", list);
        checking = true;
        call("verify", request).whenComplete((roles, error) -> server.execute(() -> {
            checking = false;
            for (var player : players) {
                if (server.getPlayerList().getPlayer(player.getUUID()) != player) continue;
                String role = error == null && roles.has(player.getUUID().toString()) ? roles.get(player.getUUID().toString()).getAsString() : "denied";
                if (role.equals("denied")) {
                    player.connection.disconnect(Component.literal("Acesso ao lobby de testes indisponível ou autorização removida. Volte ao servidor principal."));
                } else {
                    var previous = ADMISSIONS.get(connection(player));
                    if (previous != null && !previous.role().equals(role)) {
                        ADMISSIONS.put(connection(player), new DevTicketLedger.Identity(previous.uuid(), previous.username(), role));
                        syncRole(player.getUUID(), role);
                        server.getCommands().sendCommands(player);
                    }
                }
            }
        }));
    }

    public static void syncRole(UUID uuid, String role) {
        var service = RankModule.getRankService();
        var repo = new io.atlas.modules.rank.repository.RankRepository();
        for (String identifier : java.util.List.of("owner", "admin", "member")) {
            service.getRankByIdentifier(identifier).ifPresent(rank -> {
                if (identifier.equals(role)) repo.assignRank(uuid, rank.getId());
                else repo.removeRank(uuid, rank.getId());
            });
        }
    }

    public static void stop() {
        if (broker != null) broker.stop(0);
        if (executor != null) executor.shutdownNow();
        broker = null;
        executor = null;
        HOSTS.clear();
        ADMISSIONS.clear();
    }
}
