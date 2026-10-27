package io.atlas.modules.crates;
import io.atlas.modules.crates.repository.CrateRepository;
import io.atlas.modules.crates.repository.CrateRepository.Delivery;
import java.sql.*;import java.util.*;
public final class CrateRepositoryTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception {
  String url=args[0];if(!url.contains(":55458/"))throw new IllegalArgumentException("Isolated test DB required");
  var repo=new CrateRepository(url,"synthetic_operator","");var player=UUID.randomUUID();UUID id=UUID.randomUUID();
  try(var c=DriverManager.getConnection(url,"synthetic_operator","");var q=c.createStatement()){
   q.execute("INSERT INTO players VALUES(1,'"+player+"')");var month=java.time.YearMonth.now(java.time.ZoneId.of("America/Fortaleza"));
   check(repo.rotateMonth(month));check(!repo.rotateMonth(month));
   var boxes=repo.boxes();check(boxes.size()==10);check(boxes.stream().mapToInt(b->b.prizes().size()).sum()==192);
   var monthly=boxes.stream().filter(b->b.name().contains("Lendários do mês")).findFirst().orElseThrow();check(monthly.prizes().size()==20);
   for(var box:boxes)for(var prize:box.prizes())if(prize.properties().endsWith("true")){
    var normal=box.prizes().stream().filter(p->p.properties().equals(prize.properties().replace("shiny=true","shiny=false"))).findFirst().orElseThrow();check(Math.abs(prize.weight()*2-normal.weight())<0.000001);
   }
   var kanto=boxes.stream().filter(b->b.prizes().stream().anyMatch(p->p.properties().equals("mew shiny=false"))).findFirst().orElseThrow();
   check(kanto.prizes().stream().filter(p->p.properties().equals("mew shiny=false")).findFirst().orElseThrow().weight()==1);
   check(kanto.prizes().stream().filter(p->p.properties().equals("articuno shiny=false")).findFirst().orElseThrow().weight()==8);
   var original=monthly.prizes().stream().map(p->p.properties()).sorted().toList();check(original.stream().map(p->p.split(" ")[0]).distinct().count()==10);check(original.stream().filter(p->p.endsWith("true")).count()==10);
   var other=new CrateRepository(url,"synthetic_operator","");check(!other.rotateMonth(month));
   check(repo.rotateMonth(month.plusMonths(1)));
   for(var prize:repo.boxes().stream().filter(b->b.id()==monthly.id()).findFirst().orElseThrow().prizes())if(prize.properties().endsWith("true")){
    try(var weights=q.executeQuery("SELECT normal_weight FROM crate_species_weights WHERE species='"+prize.properties().split(" ")[0]+"'")){if(weights.next())check(Math.abs(prize.weight()*2-weights.getDouble(1))<0.000001);else check(prize.weight()==0.5);}
   }check(!repo.rotateMonth(month.plusMonths(1)));check(repo.rotateMonth(month));
   check(repo.boxes().stream().filter(b->b.id()==monthly.id()).findFirst().orElseThrow().prizes().stream().map(p->p.properties()).sorted().toList().equals(original));

   var reward=new Delivery(id,player,UUID.randomUUID(),boxes.getFirst().id(),"articuno shiny=false","{}","READY");check(!repo.reserve(reward));check(repo.keys(player).stream().allMatch(k->k.quantity()==0));check(repo.grant(player,"generation_1",3,"TEST"));check(repo.reserve(reward));check(!repo.reserve(reward));check(!repo.reserve(new Delivery(UUID.randomUUID(),player,UUID.randomUUID(),reward.crate(),reward.properties(),"{}","READY")));
   check(repo.pending().size()==1);check(repo.transition(id,"READY","DELIVERING"));check(!repo.transition(id,"READY","DELIVERING"));check(repo.transition(id,"DELIVERING","DELIVERED"));check(!repo.transition(id,"DELIVERING","DELIVERED"));
   try(var r=q.executeQuery("SELECT count(*) FROM crate_open_history")){r.next();check(r.getInt(1)==1);}
   var second=new Delivery(UUID.randomUUID(),player,UUID.randomUUID(),reward.crate(),reward.properties(),"{}","READY");check(repo.reserve(second));check(repo.transition(second.id(),"READY","DELIVERING"));check(repo.transition(second.id(),"DELIVERING","REVIEW"));check(repo.pending().isEmpty());check(!repo.reserve(new Delivery(UUID.randomUUID(),player,UUID.randomUUID(),reward.crate(),reward.properties(),"{}","READY")));
   q.execute("UPDATE crate_settings SET free_test=FALSE");check(repo.boxes().size()==10);check(!repo.reserve(new Delivery(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),reward.crate(),reward.properties(),"{}","READY")));
  }
  try(var c=DriverManager.getConnection(url,"synthetic_operator","");var q=c.createStatement();var r=q.executeQuery("SELECT quantity FROM player_keys")){r.next();check(r.getInt(1)==1);}
  System.out.println("PASS: virtual keys required, 2 debits for 2 durable outcomes, no debit on duplicate or REVIEW; 10 pools/192 entries; monthly ten unique legendary species + shiny, stable across reload and period return, renewal, duplicate and concurrent-pending rejection, CAS state transitions, single history entry, REVIEW blocks reroll, test-mode disable rejects new rewards.");
 }
}
