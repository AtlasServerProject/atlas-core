package io.atlas.modules.crates.repository;

import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.*;

public final class CrateRepository {
 public record Prize(String properties,double weight) {}
 public record Box(long id,String name,String dimension,int x,int y,int z,List<Prize> prizes) {}
 public record Delivery(UUID id,UUID player,UUID pokemon,long crate,String properties,String data,String state) {}
 private final String url,user,password;
 public CrateRepository(){this(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
 public CrateRepository(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
 private Connection connect() throws SQLException {
  var p=new Properties();p.setProperty("user",user);p.setProperty("password",password);p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");
  return DriverManager.getConnection(url,p);
 }
 public boolean rotateMonth(java.time.YearMonth month) throws SQLException {
  var period=java.sql.Date.valueOf(month.atDay(1));
  try(var c=connect()) {c.setAutoCommit(false);
   try {
    try(var q=c.prepareStatement("SELECT pg_advisory_xact_lock(660025)")){q.execute();}
    try(var q=c.prepareStatement("SELECT monthly_period FROM crate_settings WHERE singleton FOR UPDATE");var r=q.executeQuery()){if(r.next()&&period.equals(r.getDate(1))){c.commit();return false;}}
    boolean fresh;
    try(var q=c.prepareStatement("INSERT INTO crate_monthly_periods(period) VALUES(?) ON CONFLICT DO NOTHING")){q.setDate(1,period);fresh=q.executeUpdate()==1;}
    if(fresh) {
     var candidates=new ArrayList<String>();try(var q=c.prepareStatement("SELECT species FROM crate_monthly_candidates ORDER BY species");var r=q.executeQuery()){while(r.next())candidates.add(r.getString(1));}
     if(candidates.size()<10)throw new SQLException("Monthly crate requires ten eligible legendary species");
     Collections.shuffle(candidates,new java.security.SecureRandom());
     try(var q=c.prepareStatement("INSERT INTO crate_monthly_selection(period,slot,species) VALUES(?,?,?)")){for(int i=0;i<10;i++){q.setDate(1,period);q.setInt(2,i+1);q.setString(3,candidates.get(i));q.addBatch();}q.executeBatch();}
    }
    try(var q=c.prepareStatement("SELECT count(*) FROM crate_monthly_selection WHERE period=?")){q.setDate(1,period);try(var r=q.executeQuery()){r.next();if(r.getInt(1)!=10)throw new SQLException("Incomplete monthly selection");}}
    try(var q=c.prepareStatement("DELETE FROM crate_rewards WHERE crate_id=(SELECT id FROM crates WHERE identifier='legendary_monthly')")){q.executeUpdate();}
    try(var q=c.prepareStatement("INSERT INTO crate_rewards(crate_id,reward_type,reward_identifier,amount,chance) SELECT c.id,'POKEMON',s.species||' shiny='||v.shiny,1,coalesce((SELECT normal_weight FROM crate_species_weights WHERE species=s.species),1)*CASE WHEN v.shiny='true' THEN 0.5 ELSE 1 END FROM crates c CROSS JOIN crate_monthly_selection s CROSS JOIN (VALUES('false'),('true')) v(shiny) WHERE c.identifier='legendary_monthly' AND s.period=? ORDER BY s.slot,v.shiny")){q.setDate(1,period);q.executeUpdate();}
    try(var q=c.prepareStatement("UPDATE crate_settings SET monthly_period=? WHERE singleton")){q.setDate(1,period);q.executeUpdate();}
    try(var q=c.prepareStatement("UPDATE crates SET display_name=?,description='Dez lendários do mês, normais e shiny.' WHERE identifier='legendary_monthly'")){q.setString(1,"Lendários do mês • "+month);q.executeUpdate();}
    c.commit();return true;
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }
 }
 public List<Box> boxes() throws SQLException {
  try(var c=connect()) {
   var out=new ArrayList<Box>();
   try(var q=c.prepareStatement("SELECT c.id,c.display_name,l.* FROM crates c JOIN crate_locations l ON l.crate_id=c.id WHERE c.enabled ORDER BY c.id");var r=q.executeQuery()) {
    while(r.next()){
     var prizes=new ArrayList<Prize>();
     try(var s=c.prepareStatement("SELECT reward_identifier,chance FROM crate_rewards WHERE crate_id=? AND reward_type='POKEMON' AND chance>0 ORDER BY id")){
      s.setLong(1,r.getLong("id"));try(var rs=s.executeQuery()){while(rs.next())prizes.add(new Prize(rs.getString(1),rs.getDouble(2)));}
     }
     out.add(new Box(r.getLong("id"),r.getString("display_name"),r.getString("dimension"),r.getInt("x"),r.getInt("y"),r.getInt("z"),List.copyOf(prizes)));
    }
   }return out;
  }
 }
 public record KeyBalance(String identifier,String name,int quantity) {}
 public List<KeyBalance> keys(UUID player)throws SQLException {
  try(var c=connect();var q=c.prepareStatement("SELECT c.identifier,c.display_name,coalesce(k.quantity,0) FROM crates c LEFT JOIN players p ON p.uuid=? LEFT JOIN player_keys k ON k.player_id=p.id AND k.crate_id=c.id JOIN crate_locations l ON l.crate_id=c.id ORDER BY c.id")){
   q.setObject(1,player);try(var r=q.executeQuery()){var out=new ArrayList<KeyBalance>();while(r.next())out.add(new KeyBalance(r.getString(1),r.getString(2),r.getInt(3)));return out;}
  }
 }
 public boolean grant(UUID player,String identifier,int quantity,String actor)throws SQLException {
  if(quantity<1||quantity>1000)throw new IllegalArgumentException();
  try(var c=connect()){c.setAutoCommit(false);try{
   long crate;try(var q=c.prepareStatement("SELECT id FROM crates WHERE identifier=? AND EXISTS(SELECT 1 FROM crate_locations WHERE crate_id=crates.id)")){q.setString(1,identifier);try(var r=q.executeQuery()){if(!r.next()){c.rollback();return false;}crate=r.getLong(1);}}
   try(var q=c.prepareStatement("INSERT INTO player_keys(player_id,crate_id,quantity) SELECT id,?,? FROM players WHERE uuid=? ON CONFLICT(player_id,crate_id) DO UPDATE SET quantity=player_keys.quantity+excluded.quantity,updated_at=now()")){q.setLong(1,crate);q.setInt(2,quantity);q.setObject(3,player);if(q.executeUpdate()!=1){c.rollback();return false;}}
   try(var q=c.prepareStatement("INSERT INTO crate_key_ledger(id,player_uuid,crate_id,delta,reason,actor) VALUES(?,?,?,?,'STAFF_GRANT',?)")){q.setObject(1,UUID.randomUUID());q.setObject(2,player);q.setLong(3,crate);q.setInt(4,quantity);q.setString(5,actor);q.executeUpdate();}
   c.commit();return true;
  }catch(SQLException e){c.rollback();throw e;}}
 }
 public boolean reserve(Delivery d) throws SQLException {
  try(var c=connect()){c.setAutoCommit(false);try{
   // The player's row serializes wallet changes and duplicate opening attempts.
   long player;try(var q=c.prepareStatement("SELECT id FROM players WHERE uuid=? FOR UPDATE")){q.setObject(1,d.player);try(var r=q.executeQuery()){if(!r.next()){c.rollback();return false;}player=r.getLong(1);}}
   try(var q=c.prepareStatement("INSERT INTO crate_deliveries(id,player_uuid,pokemon_uuid,crate_id,reward_identifier,pokemon_data,state,test,rotation_period) SELECT ?,?,?,?,?,?::jsonb,'READY',FALSE,(SELECT CASE WHEN identifier='legendary_monthly' THEN (SELECT monthly_period FROM crate_settings WHERE singleton) ELSE NULL END FROM crates WHERE id=?) WHERE EXISTS(SELECT 1 FROM crates c WHERE c.id=? AND c.enabled AND (c.identifier<>'legendary_monthly' OR (SELECT monthly_period FROM crate_settings WHERE singleton)=date_trunc('month',current_timestamp AT TIME ZONE 'America/Fortaleza')::date)) AND EXISTS(SELECT 1 FROM crate_rewards WHERE crate_id=? AND reward_identifier=? AND reward_type='POKEMON' AND chance>0) ON CONFLICT DO NOTHING")){
    q.setObject(1,d.id);q.setObject(2,d.player);q.setObject(3,d.pokemon);q.setLong(4,d.crate);q.setString(5,d.properties);q.setString(6,d.data);q.setLong(7,d.crate);q.setLong(8,d.crate);q.setLong(9,d.crate);q.setString(10,d.properties);if(q.executeUpdate()!=1){c.rollback();return false;}
   }
   try(var q=c.prepareStatement("UPDATE player_keys SET quantity=quantity-1,updated_at=now() WHERE player_id=? AND crate_id=? AND quantity>0")){q.setLong(1,player);q.setLong(2,d.crate);if(q.executeUpdate()!=1){c.rollback();return false;}}
   try(var q=c.prepareStatement("INSERT INTO crate_key_ledger(id,player_uuid,crate_id,delta,reason,actor,delivery_id) VALUES(?,?,?,-1,'OPEN','PLAYER',?)")){q.setObject(1,UUID.randomUUID());q.setObject(2,d.player);q.setLong(3,d.crate);q.setObject(4,d.id);q.executeUpdate();}
   c.commit();return true;
  }catch(SQLException e){c.rollback();throw e;}}
 }
 public List<Delivery> pending() throws SQLException {
  try(var c=connect();var q=c.prepareStatement("SELECT * FROM crate_deliveries WHERE state IN ('READY','DELIVERING') ORDER BY created_at LIMIT 100");var r=q.executeQuery()){
   var out=new ArrayList<Delivery>();while(r.next())out.add(new Delivery(r.getObject("id",UUID.class),r.getObject("player_uuid",UUID.class),r.getObject("pokemon_uuid",UUID.class),r.getLong("crate_id"),r.getString("reward_identifier"),r.getString("pokemon_data"),r.getString("state")));return out;
  }
 }
 public boolean transition(UUID id,String from,String to) throws SQLException {
  if(!Set.of("READY","DELIVERING","DELIVERED","REVIEW").contains(to))throw new IllegalArgumentException();
  try(var c=connect()) {c.setAutoCommit(false);
   try(var q=c.prepareStatement("UPDATE crate_deliveries SET state=?,delivered_at=CASE WHEN ?='DELIVERED' THEN now() ELSE delivered_at END WHERE id=? AND state=? RETURNING player_uuid,crate_id,reward_identifier")){
    q.setString(1,to);q.setString(2,to);q.setObject(3,id);q.setString(4,from);
    try(var r=q.executeQuery()){if(!r.next()){c.rollback();return false;}
     if(to.equals("DELIVERED"))try(var h=c.prepareStatement("INSERT INTO crate_open_history(player_id,crate_id,reward_type,reward_identifier,amount) SELECT id,?,'POKEMON',?,1 FROM players WHERE uuid=?")){h.setLong(1,r.getLong(2));h.setString(2,r.getString(3));h.setObject(3,r.getObject(1,UUID.class));h.executeUpdate();}
    }c.commit();return true;
   }catch(SQLException e){c.rollback();throw e;}
  }
 }
}
