package io.atlas.modules.vip;
import io.atlas.modules.vip.repository.*;
import io.atlas.modules.vip.model.VipLedger.*;
import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import com.google.gson.*;import java.nio.file.*;
public final class VipRewardRepositoryTest {
 static void check(boolean v,String text){if(!v)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Dev only");
  var fixture=JsonParser.parseString(Files.readString(Path.of("/tmp/atlas-vip-reward-fixture.json"))).getAsJsonObject();UUID player=UUID.fromString(fixture.get("player").getAsString()),subject=UUID.fromString(fixture.get("subject").getAsString());long id;
  try(var c=DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);var s=c.prepareStatement("SELECT id FROM players WHERE uuid=?")){s.setObject(1,player);try(var rs=s.executeQuery()){check(rs.next(),"fixture exists");id=rs.getLong(1);}}
  var vip=new VipRepository();var rewards=new VipLegendaryRewardRepository();var d=new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","test",subject,id,"vip-3",30);
  var executor=Executors.newFixedThreadPool(2);try{var a=executor.submit(()->vip.grant(d,Instant.now()));var b=executor.submit(()->vip.grant(d,Instant.now()));check(a.get().receipt().receiptId().equals(b.get().receipt().receiptId()),"concurrent duplicate receipt");}finally{executor.shutdownNow();}
  var rows=rewards.pending().stream().filter(r->r.player().equals(player)).toList();check(rows.size()==1,"one reward per VIP3 receipt");var first=rows.getFirst();
  vip.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","test",subject,id,"vip-1",30),Instant.now());check(rewards.pending().stream().filter(r->r.player().equals(player)).count()==1,"VIP1 has no legendary");
  vip.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","test",subject,id,"vip-3",30),Instant.now());check(rewards.pending().stream().filter(r->r.player().equals(player)).count()==2,"another VIP3 purchase gives another reward");
  check(rewards.prepare(first.delivery(),"{\"species\":\"mew\",\"shiny\":true}"),"persist random result");check(!rewards.prepare(first.delivery(),"{}"),"retry cannot reroll");
  check(rewards.transition(first.delivery(),"READY","DELIVERING"),"delivery reservation");check(!rewards.transition(first.delivery(),"READY","DELIVERING"),"duplicate claim blocked");
  check(new VipLegendaryRewardRepository().pending().stream().anyMatch(r->r.delivery().equals(first.delivery())&&r.state().equals("DELIVERING")&&r.pokemon().equals(first.pokemon())&&r.data().contains("mew")),"restart preserves result and UUID");
  check(rewards.transition(first.delivery(),"DELIVERING","DELIVERED"),"completion");check(!rewards.transition(first.delivery(),"DELIVERING","DELIVERED"),"duplicate completion blocked");
  System.out.println("VipRewardRepositoryTest PASS: concurrent retry, VIP1 exclusion, multiple VIP3 purchases, immutable result, recovery UUID and conditional completion");
 }
}
