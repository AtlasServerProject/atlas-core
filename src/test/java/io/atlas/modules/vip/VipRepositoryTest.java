package io.atlas.modules.vip;
import io.atlas.modules.vip.repository.VipRepository;
import io.atlas.modules.vip.model.VipLedger;
import io.atlas.modules.vip.model.VipLedger.*;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
public final class VipRepositoryTest {
 private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  String url=System.getenv("ATLAS_VIP_TEST_URL"),user=System.getenv("ATLAS_VIP_TEST_USER"),password=System.getenv("ATLAS_VIP_TEST_PASSWORD");
  if(url==null||!"yes".equals(System.getenv("ATLAS_VIP_TEST_ISOLATED")))throw new IllegalStateException("Disposable cluster required");
  var repo=new VipRepository(url,user,password);UUID subject=UUID.randomUUID(),minecraft=UUID.randomUUID();
  try(var c=DriverManager.getConnection(url,user,password);var s=c.prepareStatement("INSERT INTO players(id,uuid,username) VALUES(1,?,'VipFixture'); INSERT INTO site_identities(subject,player_id) VALUES(?,1)")){s.setObject(1,minecraft);s.setObject(2,subject);s.execute();}
  var t=Instant.parse("2026-01-01T00:00:00Z");UUID id=UUID.randomUUID();var d=new Delivery(id,UUID.randomUUID(),"emerald","production",subject,1,"vip-1",30);
  var pool=Executors.newFixedThreadPool(2);Grant a,b;try{var go=new CountDownLatch(1);Callable<Grant> task=()->{go.await();return repo.grant(d,t);};var x=pool.submit(task);var y=pool.submit(task);go.countDown();a=x.get(10,TimeUnit.SECONDS);b=y.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  check(a.receipt().equals(b.receipt())&&a.balance().vip1Ms()==30*VipLedger.DAY_MS,"concurrent duplicate receipt grants only once");
  var duplicate=repo.grant(new Delivery(id,UUID.randomUUID(),d.server(),d.mode(),d.subject(),1,d.plan(),30),t.plusSeconds(10*86400));check(duplicate.receipt().equals(a.receipt())&&duplicate.balance().vip1Ms()==20*VipLedger.DAY_MS,"lost ACK preserves activation and remaining time");
  var up=repo.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","production",subject,1,"vip-3",30),t.plusSeconds(10*86400));check(up.balance().vip1Ms()==20*VipLedger.DAY_MS,"upgrade pauses lower");
  var refill=repo.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","production",subject,1,"vip-3",30),t.plusSeconds(15*86400));check(refill.balance().vip3Ms()==55*VipLedger.DAY_MS,"refill extends higher");
  check(VipLedger.advance(repo.find(minecraft).orElseThrow(),t.plusSeconds(75*86400)).vip1Ms()==15*VipLedger.DAY_MS,"offline expiry resumes lower");
  var before=repo.find(minecraft).orElseThrow();try{repo.grant(new Delivery(id,UUID.randomUUID(),"emerald","production",subject,1,"vip-2",30),t.plusSeconds(15*86400));throw new AssertionError("tampered receipt accepted");}catch(IllegalArgumentException expected){}
  try{repo.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","production",UUID.randomUUID(),1,"vip-1",30),t);throw new AssertionError("wrong identity accepted");}catch(IllegalArgumentException expected){}
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute("CREATE FUNCTION fail_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'isolated receipt fault'; END $$; CREATE TRIGGER fail_receipt BEFORE INSERT ON commercial_vip_receipts FOR EACH ROW EXECUTE FUNCTION fail_receipt()");}
  try{repo.grant(new Delivery(UUID.randomUUID(),UUID.randomUUID(),"emerald","production",subject,1,"vip-1",30),t.plusSeconds(16*86400));throw new AssertionError("receipt failure ignored");}catch(IllegalStateException expected){}
  check(repo.find(minecraft).orElseThrow().equals(before),"failed receipt rolls back VIP time");
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM commercial_vip_receipts")){r.next();check(r.getInt(1)==3,"exactly three purchases granted");}
  System.out.println("PASS: Core receipt concurrency, lost ACK, offline recipient, upgrades, refills, identity guard and transaction rollback.");
 }
}
