package io.atlas.modules.pokedex;
import io.atlas.modules.pokedex.repository.DexRewardRepository;
import io.atlas.modules.pokedex.model.DexRewardPlan;
import io.atlas.modules.database.DatabaseConfig;
import java.util.*;
public final class DexRewardRepositoryTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Isolated database required");
  UUID id=UUID.randomUUID();var repository=new DexRewardRepository();
  try{
   repository.unlock(id,"gen1",100);repository.unlock(id,"gen2",100);
   check(repository.list(id,"gen1").size()==5&&repository.list(id,"gen2").size()==5,"independent generation routes");repository.claim(id,"gen1",100);check(repository.list(id,"gen2").stream().filter(x->x.milestone()==100).findFirst().orElseThrow().state().equals("AVAILABLE"),"generation claim isolation");
   repository.unlock(id,"dragon",100);repository.unlock(id,"fairy",100);
   check(repository.list(id,"dragon").size()==5&&repository.list(id,"fairy").size()==5,"independent type routes");
   check(repository.list(id,"dragon").stream().anyMatch(r->r.milestone()==25)&&repository.list(id,"dragon").stream().anyMatch(r->r.milestone()==75),"quarter milestones");
   repository.claim(id,"dragon",25);check(repository.list(id,"fairy").stream().filter(r->r.milestone()==25).findFirst().orElseThrow().state().equals("AVAILABLE"),"claim cannot consume another route");
   check(repository.choice(id,"dragon",75,"MINT","cobblemon:modest_mint")&&!repository.choice(id,"dragon",75,"MINT","cobblemon:jolly_mint")&&repository.choice(id,"fairy",75,"MINT","cobblemon:jolly_mint"),"mint credit per route");
   repository.unlock(id,100);var before=repository.list(id);int expected=0;for(int m=10;m<=100;m+=10)expected+=DexRewardPlan.parts(m).size();check(before.size()==expected,"all ten approved tiers");
   repository.unlock(id,100);check(repository.list(id).size()==expected,"unlock retry has no duplicate");
   check(before.stream().filter(r->r.milestone()==80).allMatch(r->r.kind().equals("SHINY"))&&before.stream().filter(r->r.milestone()==80).count()==3,"80 exactly three shinies");
   check(repository.choice(id,100,"LEGENDARY_CHOICE","cobblemon:mew"),"one choice accepted");check(!repository.choice(id,100,"LEGENDARY_CHOICE","cobblemon:lugia"),"choice cannot be changed");
   check(repository.choice(id,70,"MINT","cobblemon:modest_mint")&&repository.choice(id,70,"MINT","cobblemon:jolly_mint")&&!repository.choice(id,70,"MINT","cobblemon:adamant_mint"),"exact two mint credits");
   repository.claim(id,10);check(repository.list(id,"dragon").stream().filter(r->r.milestone()==10).findFirst().orElseThrow().state().equals("AVAILABLE"),"global claim cannot consume type prize");var reward=repository.list(id).stream().filter(r->r.milestone()==10).findFirst().orElseThrow();
   check(repository.prepare(reward.id(),"{\"item\":\"cobblemon:poke_ball\",\"amount\":16}"),"payload persisted");check(!repository.prepare(reward.id(),"{}"),"cannot reroll prepared reward");
   check(repository.transition(reward.id(),"READY","DELIVERING"),"one delivery claim");check(!new DexRewardRepository().transition(reward.id(),"READY","DELIVERING"),"second worker blocked");
   check(repository.transition(reward.id(),"DELIVERING","DELIVERED"),"durable ack");repository.claim(id,10);repository.unlock(id,100);check(repository.list(id).stream().filter(r->r.id().equals(reward.id())).findFirst().orElseThrow().state().equals("DELIVERED"),"reconnect/reclaim cannot reset delivered");
   System.out.println("DexRewardRepositoryTest PASS: all tiers, unique unlock, immutable choices/payload, mint credits, delivery CAS, restart/reclaim no duplication");
  }finally{try(var c=java.sql.DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);var p=c.prepareStatement("DELETE FROM pokedex_rewards WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}}
 }
}
