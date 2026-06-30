package io.atlas.modules.admin.service;

import io.atlas.AtlasMod;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AdminConsoleService {

    private static final int MAX_COMMAND_LENGTH = 2_048;
    private static final Path SOCKET_PATH = Path.of(
            System.getProperty("user.dir"),
            "atlas-admin.sock"
    );
    private static final Set<PosixFilePermission> SOCKET_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    private volatile boolean running;
    private ServerSocketChannel serverChannel;

    public synchronized void start(MinecraftServer server) {
        if (running) {
            return;
        }

        try {
            Files.deleteIfExists(SOCKET_PATH);
            serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel.bind(UnixDomainSocketAddress.of(SOCKET_PATH));
            Files.setPosixFilePermissions(SOCKET_PATH, SOCKET_PERMISSIONS);
            running = true;

            Thread.ofPlatform()
                    .name("atlas-admin-console")
                    .daemon(true)
                    .start(() -> acceptConnections(server));
            AtlasMod.LOGGER.info(
                    "Console administrativo Atlas disponível em {}.",
                    SOCKET_PATH
            );
        } catch (IOException exception) {
            stop();
            throw new IllegalStateException(
                    "Não foi possível iniciar o console administrativo Atlas.",
                    exception
            );
        }
    }

    public synchronized void stop() {
        running = false;
        if (serverChannel != null) {
            try {
                serverChannel.close();
            } catch (IOException exception) {
                AtlasMod.LOGGER.warn(
                        "Erro ao fechar o console administrativo Atlas.",
                        exception
                );
            } finally {
                serverChannel = null;
            }
        }

        try {
            Files.deleteIfExists(SOCKET_PATH);
        } catch (IOException exception) {
            AtlasMod.LOGGER.warn(
                    "Erro ao remover o socket administrativo Atlas.",
                    exception
            );
        }
    }

    private void acceptConnections(MinecraftServer server) {
        while (running) {
            try {
                SocketChannel client = serverChannel.accept();
                Thread.ofVirtual()
                        .name("atlas-admin-client")
                        .start(() -> handleClient(server, client));
            } catch (IOException exception) {
                if (running) {
                    AtlasMod.LOGGER.error(
                            "Erro ao aceitar conexão administrativa Atlas.",
                            exception
                    );
                }
            }
        }
    }

    private void handleClient(MinecraftServer server, SocketChannel client) {
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     Channels.newInputStream(client),
                     StandardCharsets.UTF_8
             ));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                     Channels.newOutputStream(client),
                     StandardCharsets.UTF_8
             ))) {
            String command = reader.readLine();
            if (command == null || command.isBlank()) {
                writeResponse(writer, "Comando vazio.");
                return;
            }
            if (command.length() > MAX_COMMAND_LENGTH) {
                writeResponse(writer, "Comando excede 2048 caracteres.");
                return;
            }

            CompletableFuture<String> response = new CompletableFuture<>();
            server.execute(() -> executeCommand(server, command, response));
            writeResponse(writer, response.get(15, TimeUnit.SECONDS));
        } catch (Exception exception) {
            AtlasMod.LOGGER.warn(
                    "Falha ao processar comando administrativo Atlas.",
                    exception
            );
        }
    }

    private void executeCommand(
            MinecraftServer server,
            String command,
            CompletableFuture<String> response
    ) {
        AdminCommandSource source = new AdminCommandSource();
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack()
                            .withSource(source)
                            .withPermission(4),
                    command
            );
            response.complete(source.output());
        } catch (Exception exception) {
            response.complete("Erro ao executar comando: " + exception.getMessage());
        }
    }

    private void writeResponse(BufferedWriter writer, String response)
            throws IOException {
        writer.write(response);
        writer.newLine();
        writer.flush();
    }
}
