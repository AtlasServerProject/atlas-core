package io.atlas.modules.competitive.repository;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.TimeUnit;
public final class CompetitiveValidator {
 private final Path root=Path.of("config","atlas-competitive");
 public synchronized void install()throws java.io.IOException{
  Files.createDirectories(root);Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
  for(String name:new String[]{"validator.cjs","manifest.json"})try(var in=getClass().getResourceAsStream("/atlas-competitive/"+name)){
   if(in==null)throw new java.io.IOException("Regras ausentes");Files.write(root.resolve(name),in.readAllBytes());Files.setPosixFilePermissions(root.resolve(name),PosixFilePermissions.fromString("rw-------"));
  }
 }
 public JsonObject validate(String tier,String a,String b){
  Process process=null;try{
   install();JsonObject request=new JsonObject();request.addProperty("tier",tier);JsonArray teams=new JsonArray();teams.add(a);teams.add(b);request.add("teams",teams);
   String node=Path.of(System.getProperty("user.home"),".local/bin/node").toString();
   process=new ProcessBuilder(node,"--max-old-space-size=128",root.resolve("validator.cjs").toAbsolutePath().toString(),Path.of("showdown").toAbsolutePath().toString(),root.resolve("manifest.json").toAbsolutePath().toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
   try(var output=process.getOutputStream()){output.write(request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
   if(!process.waitFor(8,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("Validador excedeu o prazo");}
   byte[] bytes=process.getInputStream().readNBytes(65536);JsonObject response=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
   if(process.exitValue()!=0||!response.has("version")||!io.atlas.modules.competitive.service.CompetitivePolicy.VERSION.equals(response.get("version").getAsString()))throw new IllegalStateException("Validador indisponível ou motor incompatível");
   return response;
  }catch(Exception e){throw new IllegalStateException("Não foi possível validar as regras competitivas",e);}finally{if(process!=null&&process.isAlive())process.destroyForcibly();}
 }
}
