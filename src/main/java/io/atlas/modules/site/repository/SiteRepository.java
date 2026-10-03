package io.atlas.modules.site.repository;
import io.atlas.modules.database.DatabaseManager;
import java.sql.*;
import java.util.UUID;
public final class SiteRepository {
 public record Identity(UUID subject,long playerId,UUID minecraftUuid,String nickname) {}
 public Identity identity(UUID minecraftUuid){
  var connection=DatabaseManager.getConnection();
  try(var statement=connection.prepareStatement("INSERT INTO site_identities(subject,player_id) SELECT ?,id FROM players WHERE uuid=? ON CONFLICT(player_id) DO NOTHING")){
   statement.setObject(1,UUID.randomUUID());statement.setObject(2,minecraftUuid);statement.executeUpdate();
  }catch(SQLException e){throw new IllegalStateException("Site identity unavailable");}
  try(var statement=connection.prepareStatement("SELECT s.subject,p.id,p.uuid,p.username FROM site_identities s JOIN players p ON p.id=s.player_id WHERE p.uuid=?")){
   statement.setObject(1,minecraftUuid);try(var result=statement.executeQuery()){if(!result.next())throw new IllegalStateException("Player identity unavailable");return new Identity(result.getObject(1,UUID.class),result.getLong(2),result.getObject(3,UUID.class),result.getString(4));}
  }catch(SQLException e){throw new IllegalStateException("Site identity unavailable");}
 }
}
