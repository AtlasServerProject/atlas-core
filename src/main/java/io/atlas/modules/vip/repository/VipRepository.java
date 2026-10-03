package io.atlas.modules.vip.repository;
import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.vip.model.VipLedger;
import io.atlas.modules.vip.model.VipLedger.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
public final class VipRepository {
 private final String url,user,password;
 public VipRepository(){this(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
 public VipRepository(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
 private Connection connect()throws SQLException{var p=new Properties();p.setProperty("user",user);p.setProperty("password",password);p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");return DriverManager.getConnection(url,p);}
 private Balance balance(ResultSet r)throws SQLException{return new Balance(r.getObject("subject",UUID.class),r.getLong("player_id"),r.getString("server"),r.getLong("vip1_ms"),r.getLong("vip2_ms"),r.getLong("vip3_ms"),r.getTimestamp("checkpoint").toInstant());}
 private Balance load(Connection c,long id,Instant now)throws SQLException{try(var s=c.prepareStatement("SELECT * FROM commercial_vip_balances WHERE player_id=? AND server='emerald'")){s.setLong(1,id);try(var r=s.executeQuery()){if(!r.next())throw new IllegalStateException("VIP balance missing");return VipLedger.advance(balance(r),now);}}}
 public Grant grant(Delivery d,Instant instant){
  if(d.id()==null||d.subject()==null||!"emerald".equals(d.server())||!Set.of("production","test").contains(d.mode())||d.days()!=30||d.corePlayerId()<1)throw new IllegalArgumentException("Unsupported delivery");int level=VipLedger.plan(d.plan());Instant now=instant.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
  String fingerprint;try{fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((d.id()+":"+d.subject()+":"+d.corePlayerId()+":"+d.server()+":"+d.mode()+":"+d.plan()+":"+d.days()).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Fingerprint unavailable");}
  try(var c=connect()){
   c.setAutoCommit(false);try{
    try(var s=c.prepareStatement("SELECT p.id FROM players p JOIN site_identities i ON i.player_id=p.id WHERE p.id=? AND i.subject=? FOR UPDATE OF p")){s.setLong(1,d.corePlayerId());s.setObject(2,d.subject());try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Identity mismatch");}}
    try(var s=c.prepareStatement("SELECT * FROM commercial_vip_receipts WHERE delivery_id=?")){s.setObject(1,d.id());try(var r=s.executeQuery()){if(r.next()){
     if(!fingerprint.equals(r.getString("fingerprint")))throw new IllegalArgumentException("Receipt mismatch");var receipt=new Receipt(d.id(),r.getObject("receipt_id",UUID.class),d.subject(),d.corePlayerId(),d.server(),d.mode(),d.plan(),d.days(),r.getTimestamp("activated_at").toInstant());var result=new Grant(receipt,load(c,d.corePlayerId(),now));c.commit();return result;
    }}}
    try(var s=c.prepareStatement("INSERT INTO commercial_vip_balances(player_id,subject,server,checkpoint) VALUES(?,?,'emerald',?) ON CONFLICT(player_id,server) DO NOTHING")){s.setLong(1,d.corePlayerId());s.setObject(2,d.subject());s.setTimestamp(3,Timestamp.from(now));s.executeUpdate();}
    var b=VipLedger.add(load(c,d.corePlayerId(),now),level,d.days(),now);UUID receiptId=UUID.randomUUID();
    try(var s=c.prepareStatement("UPDATE commercial_vip_balances SET vip1_ms=?,vip2_ms=?,vip3_ms=?,checkpoint=? WHERE player_id=? AND server='emerald' AND subject=?")){s.setLong(1,b.vip1Ms());s.setLong(2,b.vip2Ms());s.setLong(3,b.vip3Ms());s.setTimestamp(4,Timestamp.from(b.checkpoint()));s.setLong(5,d.corePlayerId());s.setObject(6,d.subject());if(s.executeUpdate()!=1)throw new IllegalStateException("VIP identity mismatch");}
    try(var s=c.prepareStatement("INSERT INTO commercial_vip_receipts(delivery_id,receipt_id,player_id,subject,server,mode,plan,days,activated_at,fingerprint) VALUES(?,?,?,?,?,?,?,?,?,?)")){s.setObject(1,d.id());s.setObject(2,receiptId);s.setLong(3,d.corePlayerId());s.setObject(4,d.subject());s.setString(5,d.server());s.setString(6,d.mode());s.setString(7,d.plan());s.setInt(8,d.days());s.setTimestamp(9,Timestamp.from(now));s.setString(10,fingerprint);s.executeUpdate();}
    c.commit();return new Grant(new Receipt(d.id(),receiptId,d.subject(),d.corePlayerId(),d.server(),d.mode(),d.plan(),d.days(),now),b);
   }catch(Exception e){c.rollback();throw e;}
  }catch(SQLException e){throw new IllegalStateException("VIP database unavailable");}
 }
 public Optional<Balance> find(UUID uuid){try(var c=connect();var s=c.prepareStatement("SELECT b.* FROM commercial_vip_balances b JOIN players p ON p.id=b.player_id WHERE p.uuid=? AND b.server='emerald'")){s.setObject(1,uuid);try(var r=s.executeQuery()){return r.next()?Optional.of(balance(r)):Optional.empty();}}catch(SQLException e){throw new IllegalStateException("VIP database unavailable");}}
 public List<Balance> batch(long after,Instant now){List<Balance> out=new ArrayList<>();try(var c=connect();var s=c.prepareStatement("SELECT * FROM commercial_vip_balances WHERE player_id>? AND server='emerald' ORDER BY player_id LIMIT 100")){s.setLong(1,after);try(var r=s.executeQuery()){while(r.next())out.add(VipLedger.advance(balance(r),now));}return out;}catch(SQLException e){throw new IllegalStateException("VIP database unavailable");}}
}
