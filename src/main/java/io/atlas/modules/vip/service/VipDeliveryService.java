package io.atlas.modules.vip.service;
import io.atlas.AtlasMod;
import io.atlas.modules.vip.model.VipLedger;
import io.atlas.modules.vip.model.VipLedger.*;
import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
public final class VipDeliveryService {
 private final VipService vip;
 private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-vip-deliveries");t.setDaemon(true);return t;});
 private final Gson json=new GsonBuilder().registerTypeAdapter(Instant.class,(JsonSerializer<Instant>)(src,type,context)->new JsonPrimitive(src.toString())).registerTypeAdapter(Instant.class,(JsonDeserializer<Instant>)(element,type,context)->Instant.parse(element.getAsString())).create();
 private HttpClient client;private URI base;private String key;private long cursor;private int polls;
 public VipDeliveryService(VipService vip){this.vip=vip;}
 public void start(){
  var file=Path.of("config/atlas-vip.properties");if(!Files.isRegularFile(file)){AtlasMod.LOGGER.info("Entregas VIP desativadas: configuração ausente.");return;}
  try{
   Properties p=new Properties();try(var input=Files.newInputStream(file)){p.load(input);}
   base=URI.create(p.getProperty("api-url","").trim());key=p.getProperty("key","").trim();if(!base.toString().equals("https://127.0.0.1:4202")||key.length()<43)throw new IllegalArgumentException();
   KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null,null);try(var input=Files.newInputStream(Path.of(p.getProperty("trusted-certificate")))){trust.setCertificateEntry("atlas-internal",CertificateFactory.getInstance("X.509").generateCertificate(input));}
   var manager=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());manager.init(trust);var ssl=SSLContext.getInstance("TLS");ssl.init(null,manager.getTrustManagers(),null);
   client=HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
   executor.scheduleWithFixedDelay(this::poll,5,15,TimeUnit.SECONDS);AtlasMod.LOGGER.info("Entregas VIP configuradas: Emerald, HTTPS privado, somente produção.");
  }catch(Exception e){base=null;AtlasMod.LOGGER.error("Configuração de entregas VIP inválida; consumidor permanece desativado.");}
 }
 private <T>T post(String path,Object payload,Class<T> type)throws Exception{
  var request=HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(8)).header("X-Atlas-Key",key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.toJson(payload))).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());try(var input=response.body()){if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Delivery API unavailable");var bytes=input.readNBytes(65537);if(bytes.length>65536)throw new IllegalStateException("Delivery response too large");return type==Void.class?null:json.fromJson(new String(bytes,java.nio.charset.StandardCharsets.UTF_8),type);}
 }
 private void poll(){
  try{
   var deliveries=post("/internal/v1/deliveries/claim",Map.of("server","emerald"),Delivery[].class);if(deliveries==null||deliveries.length>10)throw new IllegalStateException("Invalid delivery batch");
   for(var d:deliveries){
    try{
     if(!"production".equals(d.mode())||!"emerald".equals(d.server())||d.days()!=30){post("/internal/v1/deliveries/"+d.id()+"/failed",Map.of("leaseToken",d.leaseToken(),"code","UNSUPPORTED_PLAN"),Void.class);continue;}
     VipLedger.plan(d.plan());var result=vip.grant(d);post("/internal/v1/deliveries/"+d.id()+"/ack",Map.of("leaseToken",d.leaseToken(),"receipt",result.receipt(),"balance",result.balance()),Void.class);
    }catch(IllegalArgumentException e){post("/internal/v1/deliveries/"+d.id()+"/failed",Map.of("leaseToken",d.leaseToken(),"code","IDENTITY_MISMATCH"),Void.class);}
    catch(Exception e){/* Commit may already exist. Leave lease for retry using the identical delivery id. */}
   }
   if(++polls%2==0){var balances=vip.batch(cursor);if(!balances.isEmpty()){post("/internal/v1/vip/statuses",balances,Void.class);cursor=balances.getLast().corePlayerId();}if(balances.size()<100)cursor=0;}
  }catch(Exception e){AtlasMod.LOGGER.warn("Fila VIP temporariamente indisponível; recibos e prazos preservados.");}
 }
 public void stop(){executor.shutdownNow();}
}
