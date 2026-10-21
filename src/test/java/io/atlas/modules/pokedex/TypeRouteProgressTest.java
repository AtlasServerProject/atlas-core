package io.atlas.modules.pokedex;
import io.atlas.modules.pokedex.model.TypeRoutePlan;
import io.atlas.modules.pokedex.repository.TypeRouteRepository;
import io.atlas.modules.database.*;
import java.util.UUID;
public final class TypeRouteProgressTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES.size()==9,"nine generations");int[] ends={151,251,386,493,649,721,809,905,1025};for(int i=0;i<ends.length;i++){check(io.atlas.modules.pokedex.model.GenerationRoutePlan.generation(ends[i])==i+1,"last species of generation");if(i<8)check(io.atlas.modules.pokedex.model.GenerationRoutePlan.generation(ends[i]+1)==i+2,"first species of next generation");check(io.atlas.modules.pokedex.model.GenerationRoutePlan.starters("gen"+(i+1)).size()==3,"three base starters");}
  check(TypeRoutePlan.TYPES.size()==18,"18 routes");check(TypeRoutePlan.milestone(0,0)==0,"empty never completes");check(TypeRoutePlan.milestone(24,100)==10&&TypeRoutePlan.milestone(25,100)==25&&TypeRoutePlan.milestone(74,100)==50&&TypeRoutePlan.milestone(99,100)==75&&TypeRoutePlan.milestone(100,100)==100,"exact boundaries");
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Isolated DB required");
  UUID id=UUID.randomUUID();DatabaseManager.connect();try{var repository=new TypeRouteRepository();repository.save(id,"gen1",100,151,151);repository.save(id,"gen2",25,25,100);check(repository.load(id).stream().filter(x->x.type().equals("gen1")).findFirst().orElseThrow().completed()!=null,"generation completion persisted");
   repository.save(id,"dragon",100,50,50);repository.save(id,"fairy",75,75,100);check(repository.select(id,"dragon"),"completed title selectable");check(!repository.select(id,"fairy"),"partial title rejected");var before=repository.load(id).stream().filter(p->p.type().equals("dragon")).findFirst().orElseThrow();check(before.selected(),"failed title selection preserves old title");repository.save(id,"dragon",75,50,60);DatabaseManager.disconnect();DatabaseManager.connect();var after=new TypeRouteRepository().load(id).stream().filter(p->p.type().equals("dragon")).findFirst().orElseThrow();check(after.selected()&&after.milestone()==100&&after.completed().equals(before.completed()),"title/date/milestone durable and not reset when catalog grows");repository.save(id,"fairy",100,100,100);check(repository.select(id,"fairy")&&repository.load(id).stream().filter(TypeRouteRepository.Progress::selected).count()==1,"one selected title");System.out.println("TypeRouteProgressTest PASS: 18 routes, thresholds, title eligibility, persistence and independent completion");}finally{try(var p=DatabaseManager.getConnection().prepareStatement("DELETE FROM pokedex_type_progress WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}DatabaseManager.disconnect();}
 }
}
