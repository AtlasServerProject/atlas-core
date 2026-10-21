package io.atlas.modules.vip.repository;
import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.*;
public final class VipLegendaryRewardRepository {
 public record Reward(UUID delivery,UUID player,UUID pokemon,String state,String data){}
 private final String url,user,password;
 public VipLegendaryRewardRepository(){this(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
 public VipLegendaryRewardRepository(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
 private Connection connect()throws SQLException{var p=new Properties();p.setProperty("user",user);p.setProperty("password",password);p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");return DriverManager.getConnection(url,p);}
 public List<Reward> pending(){var out=new ArrayList<Reward>();try(var c=connect();var s=c.prepareStatement("SELECT r.*,p.uuid FROM commercial_vip_legendary_rewards r JOIN players p ON p.id=r.player_id WHERE r.state IN ('PENDING','READY','DELIVERING') ORDER BY r.created_at LIMIT 100");var rs=s.executeQuery()){while(rs.next())out.add(new Reward(rs.getObject("delivery_id",UUID.class),rs.getObject("uuid",UUID.class),rs.getObject("pokemon_uuid",UUID.class),rs.getString("state"),rs.getString("pokemon_data")));return out;}catch(SQLException e){throw new IllegalStateException("Reward database unavailable",e);}}
 public boolean prepare(UUID id,String data){try(var c=connect();var s=c.prepareStatement("UPDATE commercial_vip_legendary_rewards SET pokemon_data=?::jsonb,state='READY' WHERE delivery_id=? AND state='PENDING'")){s.setString(1,data);s.setObject(2,id);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
 public boolean transition(UUID id,String from,String to){if(!Set.of("READY","DELIVERING","DELIVERED","REVIEW").contains(to))throw new IllegalArgumentException();try(var c=connect();var s=c.prepareStatement("UPDATE commercial_vip_legendary_rewards SET state=?,delivered_at=CASE WHEN ?='DELIVERED' THEN now() ELSE delivered_at END WHERE delivery_id=? AND state=?")){s.setString(1,to);s.setString(2,to);s.setObject(3,id);s.setString(4,from);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
}
