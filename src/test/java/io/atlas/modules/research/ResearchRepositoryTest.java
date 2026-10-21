package io.atlas.modules.research;
import io.atlas.modules.research.repository.ResearchRepository;
import io.atlas.modules.research.repository.ResearchRepository.Capture;
import io.atlas.modules.research.model.ResearchPlan;
import io.atlas.modules.database.DatabaseConfig;
import java.util.*;
public final class ResearchRepositoryTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Isolated database required");UUID player=UUID.randomUUID(),other=UUID.randomUUID();var repository=new ResearchRepository();long now=System.currentTimeMillis();
  try{
   check(repository.claim(player,"captures",1).isEmpty(),"unmet objective cannot claim");UUID pokemon=UUID.randomUUID();check(repository.record(new Capture(pokemon,player,"magikarp",List.of("water"),now)),"real capture counted");check(!repository.record(new Capture(pokemon,other,"magikarp",List.of("water"),now)),"same Pokémon cannot be counted for a second player");for(int i=0;i<4;i++)check(repository.record(new Capture(UUID.randomUUID(),player,"magikarp",List.of("water"),now)),"distinct Pokémon count");var summary=repository.summary(player);check(summary.captures()==5&&summary.species()==1&&summary.types()==1,"captures versus distinct species/types");
   var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var a=executor.submit(()->repository.claim(player,"captures",1));var bb=executor.submit(()->new ResearchRepository().claim(player,"captures",1));var first=a.get();var second=bb.get();executor.shutdown();check(first.isPresent()!=second.isPresent(),"one concurrent claim");var pay=first.orElseGet(()->second.orElseThrow());check(pay.amount()==250,"approved amount");check(repository.transition(pay.id(),"PENDING","PAYING")&&!repository.transition(pay.id(),"PENDING","PAYING"),"payment CAS");check(repository.transition(pay.id(),"PAYING","PAID"),"confirmed credit");check(repository.claim(player,"captures",1).isEmpty(),"paid cannot reclaim");check(new ResearchRepository().summary(player).payments().getFirst().state().equals("PAID"),"payment durable across connections");
   for(int i=0;i<100;i++)repository.record(new Capture(UUID.randomUUID(),player,"fixture:"+i,List.of("dragon","ground"),now));summary=repository.summary(player);check(summary.types()==3,"dual types count independently");check(repository.claim(player,"captures",3).isEmpty(),"cannot skip stage 2");check(repository.claim(player,"species",1).isPresent(),"independent track");
   for(String track:ResearchPlan.TRACKS)for(var stage:ResearchPlan.stages(track))check(stage.money()>=250&&stage.money()<=10000,"approved payout range");
   System.out.println("ResearchRepositoryTest PASS: capture UUID dedupe, distinct species/dual types, eligibility, concurrent claim, sequential stages, one-time payment and persistence");
  }finally{try(var c=java.sql.DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD)){for(String table:List.of("research_payments","research_captures"))try(var p=c.prepareStatement("DELETE FROM "+table+" WHERE uuid IN (?,?)")){p.setObject(1,player);p.setObject(2,other);p.executeUpdate();}}}
 }
}
