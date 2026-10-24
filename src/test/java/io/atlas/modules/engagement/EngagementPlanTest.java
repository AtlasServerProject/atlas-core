package io.atlas.modules.engagement;
import io.atlas.modules.engagement.model.EngagementPlan;
import java.time.*;
public final class EngagementPlanTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  var d=LocalDate.of(2026,10,21);
  check(EngagementPlan.nextDay(null,0,d)==1,"first login");
  check(EngagementPlan.nextDay(d,12,d)==0,"only once per calendar day");
  check(EngagementPlan.nextDay(d,12,d.plusDays(1))==13,"consecutive login");
  check(EngagementPlan.nextDay(d,12,d.plusDays(2))==1,"missed day resets");
  check(EngagementPlan.nextDay(d,30,d.plusDays(1))==1,"cycle wraps after 30");
  boolean rejected=false;try{EngagementPlan.nextDay(d,12,d.minusDays(1));}catch(IllegalArgumentException e){rejected=true;}check(rejected,"clock rollback does not award");
  check(Instant.parse("2026-10-22T02:59:59Z").atZone(EngagementPlan.ZONE).toLocalDate().equals(d),"Brasilia before midnight");
  check(Instant.parse("2026-10-22T03:00:00Z").atZone(EngagementPlan.ZONE).toLocalDate().equals(d.plusDays(1)),"Brasilia rollover");
  check(EngagementPlan.level(99)==0&&EngagementPlan.level(100)==1&&EngagementPlan.level(400)==2&&EngagementPlan.level(1000000)==100,"level thresholds");
  check(EngagementPlan.earnings(0,3)==600&&EngagementPlan.earnings(1,3)==500,"incremental level payout");
  for(int day=1;day<=30;day++){var p=EngagementPlan.prize(day);check(p.amount()>0&&p.amount()<=64,"valid daily prize");check(p.kind().equals("HA")== (day==21),"only day 21 guarantees HA");check(p.kind().equals("LEGENDARY_RANDOM")== (day==30),"only day 30 legendary");}
  System.out.println("EngagementPlanTest PASS: 30-day cycle, missed days, once/day, timezone, levels and reward policy");
 }
}
