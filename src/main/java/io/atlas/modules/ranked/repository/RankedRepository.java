package io.atlas.modules.ranked.repository;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.ranked.service.EloRules;
import io.atlas.modules.tournament.service.TournamentOptions;
import java.sql.*;
import java.util.*;

public final class RankedRepository {
 public record Standing(UUID player,String name,int rating,int wins,int losses,long position){}
 public record Change(String tier,UUID winner,int winnerRating,UUID loser,int loserRating,int delta){}
 private Connection connect()throws SQLException{
  var p=new Properties();p.setProperty("user",DatabaseConfig.USER);p.setProperty("password",DatabaseConfig.PASSWORD);p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");
  return DriverManager.getConnection(DatabaseConfig.jdbcUrl(),p);
 }
 private void lock(Connection c)throws SQLException{try(var p=c.prepareStatement("SELECT pg_advisory_xact_lock(13842054)")){p.execute();}}
 public void prepare(UUID id,String tier,UUID first,String firstName,UUID second,String secondName){
  tier=TournamentOptions.tier(tier);if(first.equals(second))throw new IllegalArgumentException("Escolha jogadores diferentes.");
  try(var c=connect()){c.setAutoCommit(false);lock(c);
   try(var p=c.prepareStatement("SELECT 1 FROM ranked_matches WHERE state IN ('PREPARING','LIVE') AND (first_uuid IN (?,?) OR second_uuid IN (?,?)) LIMIT 1")){
    p.setObject(1,first);p.setObject(2,second);p.setObject(3,first);p.setObject(4,second);try(var r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Um dos jogadores já possui uma partida ranqueada em andamento.");}
   }
   try(var p=c.prepareStatement("INSERT INTO ranked_matches(id,tier,first_uuid,first_name,second_uuid,second_name,state,validation_version) VALUES(?,?,?,?,?,?,'PREPARING',?)")){
    p.setObject(1,id);p.setString(2,tier);p.setObject(3,first);p.setString(4,firstName);p.setObject(5,second);p.setString(6,secondName);p.setString(7,io.atlas.modules.competitive.service.CompetitivePolicy.VERSION);p.executeUpdate();
   }c.commit();
  }catch(SQLException e){throw new IllegalStateException("Não foi possível reservar partida ranqueada",e);}
 }
 public void activate(UUID id,UUID battle){try(var c=connect();var p=c.prepareStatement("UPDATE ranked_matches SET battle_id=?,state='LIVE' WHERE id=? AND state='PREPARING'")){p.setObject(1,battle);p.setObject(2,id);p.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 public void cancel(UUID id,String reason){try(var c=connect();var p=c.prepareStatement("UPDATE ranked_matches SET state='VOID',reason=?,settled_at=now() WHERE id=? AND state IN ('PREPARING','LIVE')")){p.setString(1,reason);p.setObject(2,id);p.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 public void recover(){try(var c=connect();var p=c.prepareStatement("UPDATE ranked_matches SET state='VOID',reason='Reinício sem resultado confirmado',settled_at=now() WHERE state IN ('PREPARING','LIVE')")){p.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 private int rating(Connection c,String tier,UUID id,String name)throws SQLException{
  try(var p=c.prepareStatement("INSERT INTO ranked_ratings(tier,player_uuid,player_name) VALUES(?,?,?) ON CONFLICT(tier,player_uuid) DO UPDATE SET player_name=excluded.player_name")){p.setString(1,tier);p.setObject(2,id);p.setString(3,name);p.executeUpdate();}
  try(var p=c.prepareStatement("SELECT rating FROM ranked_ratings WHERE tier=? AND player_uuid=? FOR UPDATE")){p.setString(1,tier);p.setObject(2,id);try(var r=p.executeQuery()){r.next();return r.getInt(1);}}
 }
 public Optional<Change> settle(UUID id,UUID battle,UUID winner,UUID loser){
  try(var c=connect()){c.setAutoCommit(false);lock(c);
   try(var p=c.prepareStatement("SELECT * FROM ranked_matches WHERE id=? FOR UPDATE")){p.setObject(1,id);try(var r=p.executeQuery()){
    if(!r.next())throw new IllegalArgumentException("Partida não registrada");
    if(r.getString("state").equals("SETTLED")||r.getString("state").equals("VOID"))return Optional.empty();
    UUID a=r.getObject("first_uuid",UUID.class),b=r.getObject("second_uuid",UUID.class),storedBattle=r.getObject("battle_id",UUID.class);
    if(winner.equals(loser)||!(winner.equals(a)&&loser.equals(b)||winner.equals(b)&&loser.equals(a))||storedBattle!=null&&!storedBattle.equals(battle))throw new IllegalArgumentException("Resultado divergente da partida");
    String tier=r.getString("tier"),wn=r.getString(winner.equals(a)?"first_name":"second_name"),ln=r.getString(loser.equals(a)?"first_name":"second_name");
    int wr=rating(c,tier,winner,wn),lr=rating(c,tier,loser,ln),delta=EloRules.delta(wr,lr);
    try(var q=c.prepareStatement("UPDATE ranked_ratings SET rating=rating+?,wins=wins+? ,losses=losses+? WHERE tier=? AND player_uuid=?")){
     q.setInt(1,delta);q.setInt(2,1);q.setInt(3,0);q.setString(4,tier);q.setObject(5,winner);q.executeUpdate();
     q.setInt(1,-delta);q.setInt(2,0);q.setInt(3,1);q.setObject(5,loser);q.executeUpdate();
    }
    try(var q=c.prepareStatement("UPDATE ranked_matches SET state='SETTLED',battle_id=?,winner_uuid=?,delta=?,settled_at=now() WHERE id=?")){q.setObject(1,battle);q.setObject(2,winner);q.setInt(3,delta);q.setObject(4,id);q.executeUpdate();}
    c.commit();return Optional.of(new Change(tier,winner,wr+delta,loser,lr-delta,delta));
   }}
  }catch(SQLException e){throw new IllegalStateException("Resultado ranqueado pendente",e);}
 }
 private static final String BOARD="SELECT player_uuid,player_name,rating,wins,losses,row_number() OVER(ORDER BY rating DESC,wins DESC,losses,player_uuid) position FROM ranked_ratings WHERE tier=? AND wins+losses>0";
 private Standing row(ResultSet r)throws SQLException{return new Standing(r.getObject(1,UUID.class),r.getString(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getLong(6));}
 public List<Standing> top(String tier){try(var c=connect();var p=c.prepareStatement(BOARD+" ORDER BY position LIMIT 10")){p.setString(1,TournamentOptions.tier(tier));try(var r=p.executeQuery()){List<Standing> out=new ArrayList<>();while(r.next())out.add(row(r));return out;}}catch(SQLException e){throw new IllegalStateException(e);}}
 public Optional<Standing> profile(String tier,UUID id){try(var c=connect();var p=c.prepareStatement("SELECT * FROM ("+BOARD+") board WHERE player_uuid=?")){p.setString(1,TournamentOptions.tier(tier));p.setObject(2,id);try(var r=p.executeQuery()){return r.next()?Optional.of(row(r)):Optional.empty();}}catch(SQLException e){throw new IllegalStateException(e);}}
}
