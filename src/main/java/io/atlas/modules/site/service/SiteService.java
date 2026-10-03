package io.atlas.modules.site.service;
import io.atlas.modules.site.repository.SiteRepository;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.AtlasMod;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
public final class SiteService {
 private final SiteRepository repository=new SiteRepository();
 private final Map<UUID,Long> attempts=new HashMap<>();
 private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{var t=new Thread(r,"atlas-site-link");t.setDaemon(true);return t;});
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
 private URI endpoint;private String key;
 public void start(){
  var file=Path.of("config/atlas-site.properties");if(!Files.isRegularFile(file)){AtlasMod.LOGGER.info("Vinculação ao site desativada: configuração ausente.");return;}
  var props=new Properties();try(var input=Files.newInputStream(file)){props.load(input);var uri=URI.create(props.getProperty("api-url","").trim());var secret=props.getProperty("key","").trim();if(!uri.toString().equals("http://127.0.0.1:8080")||secret.length()<43)throw new IllegalArgumentException();endpoint=uri.resolve("/internal/v1/minecraft/proofs");key=secret;AtlasMod.LOGGER.info("Vinculação ao site configurada (Emerald).");}catch(Exception e){AtlasMod.LOGGER.error("Configuração de vinculação ao site inválida.");}
 }
 public void stop(){executor.shutdownNow();attempts.clear();}
 public int link(ServerPlayer player,String code){
  if(endpoint==null){player.sendSystemMessage(Component.literal("§eA vinculação ao site está temporariamente indisponível."));return 0;}
  if(!AuthModule.getAuthService().isAuthenticated(player.getUUID())){player.sendSystemMessage(Component.literal("§cFaça login no servidor antes de vincular sua conta."));return 0;}
  if(!(LobbyWorlds.isEmerald(player.serverLevel())||LobbyWorlds.isSurvivalArea(player.serverLevel()))){player.sendSystemMessage(Component.literal("§eEntre no Emerald antes de vincular sua conta."));return 0;}
  if(!code.matches("[A-Za-z0-9_-]{43}")){player.sendSystemMessage(Component.literal("§cCódigo inválido. Copie o comando completo da página Minha conta."));return 0;}
  long now=System.currentTimeMillis();var uuid=player.getUUID();if(now-attempts.getOrDefault(uuid,0L)<10000){player.sendSystemMessage(Component.literal("§eAguarde alguns segundos antes de tentar novamente."));return 0;}
  attempts.put(uuid,now);attempts.entrySet().removeIf(e->now-e.getValue()>600000);
  final SiteRepository.Identity identity;try{identity=repository.identity(uuid);}catch(Exception e){player.sendSystemMessage(Component.literal("§cNão foi possível consultar sua identidade. Tente novamente mais tarde."));return 0;}
  var body=new JsonObject();body.addProperty("code",code);body.addProperty("subject",identity.subject().toString());body.addProperty("corePlayerId",identity.playerId());body.addProperty("minecraftUuid",identity.minecraftUuid().toString());body.addProperty("nickname",identity.nickname());body.addProperty("server","emerald");
  var server=player.getServer();try{executor.execute(()->{
   String message;
   try{var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5)).header("Content-Type","application/json").header("X-Atlas-Key",key).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();var response=http.send(request,HttpResponse.BodyHandlers.discarding());message=switch(response.statusCode()){case 204->"§aJogador comprovado! Volte à página Minha conta e confirme o vínculo.";case 409->"§eCódigo inválido, expirado ou jogador já vinculado. Confira Minha conta no site.";case 429->"§eMuitas tentativas. Aguarde antes de tentar novamente.";default->"§cVinculação indisponível. Tente novamente mais tarde.";};}catch(Exception e){message="§eNão foi possível verificar a resposta. Confira Minha conta no site antes de tentar novamente.";}
   var result=message;if(server!=null)server.execute(()->{var connected=server.getPlayerList().getPlayer(uuid);if(connected!=null)connected.sendSystemMessage(Component.literal(result));});
  });}catch(RejectedExecutionException e){player.sendSystemMessage(Component.literal("§eVinculação ocupada. Tente novamente em alguns segundos."));return 0;}
  player.sendSystemMessage(Component.literal("§7Comprovando jogador no site…"));return 1;
 }
}
