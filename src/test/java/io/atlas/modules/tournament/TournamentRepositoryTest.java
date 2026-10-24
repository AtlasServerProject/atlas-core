package io.atlas.modules.tournament;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.tournament.repository.TournamentRepository;
import io.atlas.modules.tournament.service.TournamentOptions;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
public final class TournamentRepositoryTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static Connection connection()throws Exception{return DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
 static void clear(UUID event)throws Exception{try(var db=connection()){
  for(String table:new String[]{"atlas_tournament_announcements","atlas_tournament_entries"})db.createStatement().executeUpdate("DELETE FROM "+table+" WHERE tournament_id='"+event+"'");
  db.createStatement().executeUpdate("DELETE FROM atlas_tournaments WHERE id='"+event+"'");
 }}
 public static void main(String[] args)throws Exception{
  if(DatabaseConfig.PORT!=25432||!DatabaseConfig.DATABASE.equals("atlas_compat_182"))throw new IllegalStateException("Isolated database required");
  check(TournamentOptions.seconds("1h30m")==5400,"compound duration");check(TournamentOptions.seconds("24h")==86400,"max duration");check(TournamentOptions.tier("monotype").equals("MONOTYPE"),"tier normalization");
  for(String invalid:new String[]{"0m","59s","25h","5","10mx","99999999999999999999999999h"}){boolean rejected=false;try{TournamentOptions.seconds(invalid);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid time "+invalid);}
  for(int capacity:new int[]{8,16})exercise(capacity);
  var repo=new TournamentRepository();UUID event=repo.create("ExpiryTest","UU",8,60);
  try(var db=connection()){
   db.createStatement().executeUpdate("UPDATE atlas_tournaments SET state='OPEN',registration_deadline=clock_timestamp()-interval '1 second' WHERE id='"+event+"'");
   check(repo.enter(UUID.randomUUID(),"Late").contains("Não há"),"late join denied");repo.expire();
   check(repo.active().isEmpty(),"expiry persisted and no active event after restart");
   try(var r=db.createStatement().executeQuery("SELECT count(*) FROM atlas_tournament_announcements WHERE tournament_id='"+event+"' AND NOT delivered")){r.next();check(r.getInt(1)==1,"one cancellation notice despite repeated expiration");}
  }finally{clear(event);}
  System.out.println("TournamentRepositoryTest PASS: 8/16 capacities, tiers, time parsing, concurrent 40 joins, duplicates, expiry, idempotent cancellation, and full event survives deadline");
 }
 static void exercise(int capacity)throws Exception{
  var repo=new TournamentRepository();UUID event=repo.create("RepositoryTest","UBERS",capacity,1800);var pool=Executors.newFixedThreadPool(8);
  try(var db=connection()){
   check(repo.enter(UUID.randomUUID(),"Early").contains("não estão abertas"),"creating closed");
   db.createStatement().executeUpdate("UPDATE atlas_tournaments SET state='OPEN' WHERE id='"+event+"'");
   UUID player=UUID.randomUUID();repo.enter(player,"Duplicate");repo.enter(player,"Duplicate");check(repo.active().orElseThrow().count()==1,"duplicate rejected");
   repo.leave(player);repo.leave(player);check(repo.active().orElseThrow().count()==0,"leave idempotent");
   var gate=new CountDownLatch(1);List<Future<String>> futures=new ArrayList<>();
   for(int i=0;i<40;i++){final int n=i;futures.add(pool.submit(()->{gate.await();return new TournamentRepository().enter(UUID.randomUUID(),"Player"+n);}));}
   gate.countDown();for(var future:futures)future.get(30,TimeUnit.SECONDS);
   var active=new TournamentRepository().active().orElseThrow();check(active.count()==capacity&&active.capacity()==capacity&&active.state().equals("FULL"),"concurrent capacity and durability");
   check(active.title().equals("Torneio UBERS GEN 9"),"dynamic tier title");
   db.createStatement().executeUpdate("UPDATE atlas_tournaments SET registration_deadline=clock_timestamp()-interval '1 second' WHERE id='"+event+"'");repo.expire();
   check(repo.active().orElseThrow().state().equals("FULL"),"full event must not expire");
   repo.leave(player);check(repo.active().orElseThrow().count()==capacity,"full frozen");
   boolean rejected=false;try{repo.create("DuplicateEvent");}catch(IllegalArgumentException e){rejected=true;}check(rejected,"one live event");
   repo.cancel();check(repo.active().isEmpty(),"cancel clears active");
  }finally{pool.shutdownNow();clear(event);}
 }
}
