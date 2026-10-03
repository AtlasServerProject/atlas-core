package io.atlas.modules.vip;
import io.atlas.modules.vip.model.VipLedger;
import io.atlas.modules.vip.model.VipLedger.*;
import java.time.*;
import java.util.*;
public final class VipLedgerTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  var t=Instant.parse("2026-01-01T00:00:00Z");long day=VipLedger.DAY_MS;var empty=new Balance(UUID.randomUUID(),1,"emerald",0,0,0,t);
  var one=VipLedger.add(empty,1,30,t);check(one.level()==1&&one.vip1Ms()==30*day,"initial activation");
  var higher=VipLedger.add(one,3,30,t.plusSeconds(10*86400));check(higher.vip1Ms()==20*day&&higher.vip3Ms()==30*day,"higher pauses remaining lower");
  var resumed=VipLedger.advance(higher,t.plusSeconds(40*86400));check(resumed.level()==1&&resumed.vip1Ms()==20*day,"exact boundary resumes lower");
  check(VipLedger.advance(higher,t.plusSeconds(45*86400)).vip1Ms()==15*day,"offline consumes resumed balance");
  var middle=VipLedger.add(higher,2,30,t.plusSeconds(11*86400));check(middle.vip1Ms()==20*day&&middle.vip3Ms()==29*day&&middle.vip2Ms()==30*day,"lower purchase queues while higher runs");
  var offline=VipLedger.advance(middle,t.plusSeconds(75*86400));check(offline.level()==1&&offline.vip1Ms()==15*day,"offline crosses multiple paused levels");
  var refill=VipLedger.add(middle,3,30,t.plusSeconds(12*86400));check(refill.vip3Ms()==58*day&&refill.vip2Ms()==30*day,"same level adds time");
  check(VipLedger.advance(one,t.minusSeconds(30)).equals(one),"clock rewind does not add balance");
  check(VipLedger.advance(one,t.plusMillis(1)).vip1Ms()==30*day-1,"millisecond precision");
  check(VipLedger.advance(middle,t.plusSeconds(100*86400)).level()==0,"all expire offline");
  System.out.println("PASS: VIP activation, pause/resume, refills, offline cascade and exact expiry.");
 }
}
