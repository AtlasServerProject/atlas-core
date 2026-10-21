package io.atlas.modules.player;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.player.model.ReconnectPosition;
import io.atlas.modules.player.repository.ReconnectPositionRepository;
import java.util.UUID;
public final class ReconnectPositionRepositoryTest {
 public static void main(String[] args)throws Exception {
  if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Isolated dev DB required");
  UUID id=UUID.randomUUID();DatabaseManager.connect();
  try {
   var repository=new ReconnectPositionRepository();
   if(repository.find(id).isPresent())throw new AssertionError("fixture should be new");
   var pos=new ReconnectPosition("minecraft:the_nether",123.25,75,-456.5,179.5f,-45f);repository.save(id,pos);
   DatabaseManager.disconnect();DatabaseManager.connect();
   if(!new ReconnectPositionRepository().find(id).orElseThrow().equals(pos))throw new AssertionError("restart must retain dimension, location, direction");
   var next=new ReconnectPosition("minecraft:the_end",-1.5,90,5.125,-15f,20f);repository.save(id,next);
   if(!repository.find(id).orElseThrow().equals(next))throw new AssertionError("latest logout overrides old position");
   System.out.println("ReconnectPositionRepositoryTest PASS: restart durability, dimension/coordinates/direction, latest snapshot");
  } finally {
   try(var p=DatabaseManager.getConnection().prepareStatement("DELETE FROM player_reconnect_positions WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}
   DatabaseManager.disconnect();
  }
 }
}
