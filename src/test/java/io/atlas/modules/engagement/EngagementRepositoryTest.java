package io.atlas.modules.engagement;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.engagement.repository.EngagementRepository;
import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
public final class EngagementRepositoryTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  if(!DatabaseConfig.DATABASE.equals("atlas_compat_182")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Isolated compatibility database required");
  var id=UUID.randomUUID();var repo=new EngagementRepository();var today=LocalDate.of(2026,10,21);var world="fixture:"+id;
  try(var c=DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD)){
   try{
    try(var p=c.prepareStatement("INSERT INTO players(uuid,username,is_online) VALUES(?, 'JobFixture',true)")){p.setObject(1,id);p.executeUpdate();}
    var executor=Executors.newFixedThreadPool(2);
    try{var a=executor.submit(()->repo.claimDaily(id,today));var b=executor.submit(()->repo.claimDaily(id,today));int one=a.get(),two=b.get();check((one==1&&two==0)||(one==0&&two==1),"one concurrent daily claim");}finally{executor.shutdown();}
    check(repo.claimDaily(id,today.plusDays(1))==2,"consecutive claim");check(repo.claimDaily(id,today.plusDays(3))==1,"miss resets");
    check(new EngagementRepository().daily(id).day()==1,"daily durable across connections");
    repo.placed(world,100);check(!repo.activity(id,"miner","placed:"+id,12,today,world,100L),"placed blocks do not count");
    check(repo.activity(id,"miner","natural:"+id,100,today,world,101L),"natural block counts");
    check(!repo.activity(id,"miner","natural:"+id,100,today,world,101L),"block cannot pay twice");
    check(repo.activity(id,"trainer","capture:"+id,100,today,null,null),"second profession simultaneously");
    for(int i=0;i<49;i++)check(repo.activity(id,"miner","ore:"+id+":"+i,100,today,null,null),"within daily XP cap");
    check(!repo.activity(id,"miner","overcap:"+id,1,today,null,null),"daily XP cap enforced");
    check(repo.activity(id,"miner","nextday:"+id,100,today.plusDays(1),null,null),"cap resets next day");
    check(repo.claimJobs(id)==2900,"levels 1..7 and trainer level1 payout");check(repo.claimJobs(id)==0,"job payout cannot duplicate");
    check(new EngagementRepository().progress(id).size()==2,"independent profession progress durable");
    var rewards=repo.pending().stream().filter(r->r.player().equals(id)).toList();check(rewards.size()==5,"atomic claims create delivery queue");
    var reward=rewards.getFirst();check(repo.prepare(reward.id(),"{}"),"payload persisted");check(!repo.prepare(reward.id(),"{}"),"payload cannot reroll");
    check(repo.state(reward.id(),"READY","DELIVERING"),"delivery compare and set");check(!repo.state(reward.id(),"READY","DELIVERING"),"no concurrent duplicate delivery");
    check(repo.state(reward.id(),"DELIVERING","REVIEW"),"uncertain delivery retained for review");
   }finally{
    for(String table:List.of("engagement_rewards","daily_progress","profession_activity","profession_progress"))try(var p=c.prepareStatement("DELETE FROM "+table+" WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}
    try(var p=c.prepareStatement("DELETE FROM profession_placed_blocks WHERE world=?")){p.setString(1,world);p.executeUpdate();}
    try(var p=c.prepareStatement("DELETE FROM players WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}
   }
  }
  System.out.println("EngagementRepositoryTest PASS: concurrent daily claim, missed days, durability, caps, provenance, payouts and outbox CAS");
 }
}
