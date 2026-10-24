package io.atlas.modules.ranked;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.ranked.repository.*;
import io.atlas.modules.ranked.service.EloRules;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
public final class RankedRepositoryTest {
 static void check(boolean c,String message){if(!c)throw new AssertionError(message);}
 static Connection connection()throws Exception{return DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
 public static void main(String[] args)throws Exception{
  if(DatabaseConfig.PORT!=25432||!DatabaseConfig.DATABASE.equals("atlas_compat_182"))throw new IllegalStateException("Isolated database required");
  check(EloRules.delta(1000,1000)==16,"equal opponents");check(EloRules.delta(700,1400)>EloRules.delta(1400,700),"upset rewards more");check(EloRules.delta(0,0)==0,"floor zero sum");
  var repo=new RankedRepository();var a=UUID.randomUUID();var b=UUID.randomUUID();List<UUID> matches=new ArrayList<>();Path root=Files.createTempDirectory("ranked-journal-test");
  var pool=Executors.newFixedThreadPool(6);
  try{
   UUID match=UUID.randomUUID(),battle=UUID.randomUUID();matches.add(match);repo.prepare(match,"OU",a,"RankedA",b,"RankedB");
   boolean blocked=false;try{repo.prepare(UUID.randomUUID(),"UU",b,"B",a,"A");}catch(IllegalArgumentException e){blocked=true;}check(blocked,"overlapping participant reservation rejected");
   repo.activate(match,battle);
   boolean rejected=false;try{repo.settle(match,UUID.randomUUID(),a,b);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"foreign battle rejected");
   var journal=new RankedResultJournal(root);journal.save(new RankedResultJournal.Result(match,battle,a,b));
   var persisted=new RankedResultJournal(root).pending().getFirst();check(persisted.winner().equals(a),"winner durable across journal reopen");
   var gate=new CountDownLatch(1);var tasks=new ArrayList<Future<Boolean>>();for(int i=0;i<12;i++)tasks.add(pool.submit(()->{gate.await();return new RankedRepository().settle(match,battle,a,b).isPresent();}));
   gate.countDown();int count=0;for(var t:tasks)if(t.get(30,TimeUnit.SECONDS))count++;check(count==1,"concurrent duplicate events settled once");
   journal.done(match);check(journal.pending().isEmpty(),"outbox acknowledged");
   var winner=new RankedRepository().profile("OU",a).orElseThrow();var loser=repo.profile("OU",b).orElseThrow();
   check(winner.rating()==1016&&winner.wins()==1&&loser.rating()==984&&loser.losses()==1,"atomic ELO and stats");
   check(repo.profile("UU",a).isEmpty(),"tier isolation");check(winner.position()<loser.position(),"ranking order");
   UUID interrupted=UUID.randomUUID();matches.add(interrupted);repo.prepare(interrupted,"OU",a,"RankedA",b,"RankedB");repo.cancel(interrupted,"Interrompida");
   check(repo.settle(interrupted,UUID.randomUUID(),b,a).isEmpty(),"void match cannot score");check(repo.profile("OU",a).orElseThrow().rating()==1016,"void preserves score");
   UUID pending=UUID.randomUUID(),nativeId=UUID.randomUUID();matches.add(pending);repo.prepare(pending,"UU",a,"A",b,"B");
   journal.save(new RankedResultJournal.Result(pending,nativeId,b,a));
   var replay=new RankedResultJournal(root).pending().getFirst();repo.settle(replay.match(),replay.battle(),replay.winner(),replay.loser());journal.done(replay.match());
   check(repo.profile("UU",b).orElseThrow().rating()==1016,"result before LIVE acknowledgement recovered");
  }finally{
   pool.shutdownNow();try(var db=connection()){
    for(UUID id:matches){try(var p=db.prepareStatement("DELETE FROM ranked_matches WHERE id=?")){p.setObject(1,id);p.executeUpdate();}}
    try(var p=db.prepareStatement("DELETE FROM ranked_ratings WHERE player_uuid IN (?,?)")){p.setObject(1,a);p.setObject(2,b);p.executeUpdate();}
   }
   try(var files=Files.walk(root)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
  }
  System.out.println("RankedRepositoryTest PASS: Elo, pair reservations, battle identity, durable outbox, 12 concurrent replays, atomic score/stats, tier isolation, void and pre-LIVE recovery");
 }
}
