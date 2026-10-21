package io.atlas.modules.pokedex;
import io.atlas.modules.pokedex.service.PokedexService;
import io.atlas.modules.pokedex.repository.PokedexRepository;
import io.atlas.modules.database.*;
import java.util.UUID;
public final class PokedexProgressTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(PokedexService.milestone(9,100)==0,"no early milestone");
  check(PokedexService.milestone(99,100)==90,"100 requires full dex");
  check(PokedexService.milestone(100,100)==100,"completion");
  check(PokedexService.milestone(0,0)==0,"empty registry cannot complete");
  check(PokedexService.region(151).equals("kanto")&&PokedexService.region(152).equals("johto")&&PokedexService.region(386).equals("hoenn")&&PokedexService.region(1025).equals("paldea"),"region boundaries");
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("isolated DB required");
  UUID id=UUID.randomUUID();DatabaseManager.connect();
  try{
   var repository=new PokedexRepository();
   check(repository.load(id).completed()==null,"new player not professor");
   check(repository.save(id,"DexFixture",90,99,100).completed()==null,"partial not professor");
   var completed=repository.save(id,"DexFixture",100,100,100);check(completed.completed()!=null,"completion date saved");
   DatabaseManager.disconnect();DatabaseManager.connect();
   var reopened=new PokedexRepository().load(id);check(reopened.completed().equals(completed.completed()),"date survives restart");
   var changed=repository.save(id,"DexFixture",90,100,110);check(changed.completed().equals(completed.completed())&&changed.milestone()==100,"title and first date survive registry changes");
   System.out.println("PokedexProgressTest PASS: thresholds, regions, first completion, restart durability and idempotent title");
  }finally{try(var p=DatabaseManager.getConnection().prepareStatement("DELETE FROM pokedex_progress WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}DatabaseManager.disconnect();}
 }
}
