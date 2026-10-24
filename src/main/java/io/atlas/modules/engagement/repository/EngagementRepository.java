package io.atlas.modules.engagement.repository;

import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.engagement.model.EngagementPlan;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class EngagementRepository {
    public record Progress(String job,long xp,int paidLevel) {}
    public record Daily(LocalDate last,int day) {}
    public record Reward(UUID id,UUID player,String source,String key,String kind,String value,int amount,String state,String payload) {}
    private Connection connect() throws SQLException {
        var p=new Properties();p.setProperty("user",DatabaseConfig.USER);p.setProperty("password",DatabaseConfig.PASSWORD);
        p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");
        return DriverManager.getConnection(DatabaseConfig.jdbcUrl(),p);
    }
    private void lock(Connection c,UUID id) throws SQLException {
        try(var p=c.prepareStatement("SELECT pg_advisory_xact_lock(?)")){p.setLong(1,id.getMostSignificantBits());p.executeQuery().close();}
    }
    public void placed(String world,long pos) {
        try(var c=connect();var p=c.prepareStatement("INSERT INTO profession_placed_blocks VALUES(?,?) ON CONFLICT DO NOTHING")){
            p.setString(1,world);p.setLong(2,pos);p.executeUpdate();
        }catch(SQLException e){throw new IllegalStateException(e);}
    }
    public boolean activity(UUID id,String job,String key,int xp,LocalDate today,String world,Long pos) {
        if(!EngagementPlan.JOBS.contains(job)||xp<1||xp>100)throw new IllegalArgumentException();
        try(var c=connect()) {c.setAutoCommit(false);try {
            lock(c,id);
            if(pos!=null)try(var p=c.prepareStatement("SELECT 1 FROM profession_placed_blocks WHERE world=? AND pos=?")){
                p.setString(1,world);p.setLong(2,pos);try(var r=p.executeQuery()){if(r.next()){c.rollback();return false;}}
            }
            try(var p=c.prepareStatement("SELECT COALESCE(sum(xp),0) FROM profession_activity WHERE uuid=? AND job=? AND activity_date=?")){
                p.setObject(1,id);p.setString(2,job);p.setObject(3,today);try(var r=p.executeQuery()){r.next();if(r.getLong(1)+xp>5000){c.rollback();return false;}}
            }
            try(var p=c.prepareStatement("INSERT INTO profession_activity(event_key,uuid,job,activity_date,xp) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING")){
                p.setString(1,key);p.setObject(2,id);p.setString(3,job);p.setObject(4,today);p.setInt(5,xp);
                if(p.executeUpdate()!=1){c.rollback();return false;}
            }
            try(var p=c.prepareStatement("INSERT INTO profession_progress(uuid,job,xp) VALUES(?,?,?) ON CONFLICT(uuid,job) DO UPDATE SET xp=LEAST(1000000,profession_progress.xp+EXCLUDED.xp)")){
                p.setObject(1,id);p.setString(2,job);p.setInt(3,xp);p.executeUpdate();
            }
            c.commit();return true;
        }catch(Exception e){c.rollback();throw e;}}catch(SQLException e){throw new IllegalStateException(e);}
    }
    public List<Progress> progress(UUID id) {
        var out=new ArrayList<Progress>();try(var c=connect();var p=c.prepareStatement("SELECT job,xp,paid_level FROM profession_progress WHERE uuid=?")){
            p.setObject(1,id);try(var r=p.executeQuery()){while(r.next())out.add(new Progress(r.getString(1),r.getLong(2),r.getInt(3)));}return out;
        }catch(SQLException e){throw new IllegalStateException(e);}
    }
    public Daily daily(UUID id) {
        try(var c=connect();var p=c.prepareStatement("SELECT last_claim,day FROM daily_progress WHERE uuid=?")){
            p.setObject(1,id);try(var r=p.executeQuery()){return r.next()?new Daily(r.getObject(1,LocalDate.class),r.getInt(2)):new Daily(null,0);}
        }catch(SQLException e){throw new IllegalStateException(e);}
    }
    private void insert(Connection c,UUID id,String source,String key,String kind,String value,int amount) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO engagement_rewards(id,uuid,source,claim_key,kind,value,amount) VALUES(?,?,?,?,?,?,?)")){
            p.setObject(1,UUID.randomUUID());p.setObject(2,id);p.setString(3,source);p.setString(4,key);p.setString(5,kind);p.setString(6,value);p.setInt(7,amount);p.executeUpdate();
        }
    }
    public int claimDaily(UUID id,LocalDate today) {
        try(var c=connect()){c.setAutoCommit(false);try{lock(c,id);Daily d;
            try(var p=c.prepareStatement("SELECT last_claim,day FROM daily_progress WHERE uuid=?")){p.setObject(1,id);try(var r=p.executeQuery()){d=r.next()?new Daily(r.getObject(1,LocalDate.class),r.getInt(2)):new Daily(null,0);}}
            int day=EngagementPlan.nextDay(d.last(),d.day(),today);if(day==0){c.rollback();return 0;}
            var prize=EngagementPlan.prize(day);insert(c,id,"daily",today.toString(),prize.kind(),prize.item(),prize.amount());
            try(var p=c.prepareStatement("INSERT INTO daily_progress(uuid,last_claim,day) VALUES(?,?,?) ON CONFLICT(uuid) DO UPDATE SET last_claim=EXCLUDED.last_claim,day=EXCLUDED.day")){
                p.setObject(1,id);p.setObject(2,today);p.setInt(3,day);p.executeUpdate();}
            c.commit();return day;
        }catch(Exception e){c.rollback();throw e;}}catch(SQLException e){throw new IllegalStateException(e);}
    }
    public long claimJobs(UUID id) {
        try(var c=connect()){c.setAutoCommit(false);try{lock(c,id);long total=0;
            try(var p=c.prepareStatement("SELECT job,xp,paid_level FROM profession_progress WHERE uuid=? FOR UPDATE")){
                p.setObject(1,id);try(var r=p.executeQuery()){while(r.next()){
                    int level=EngagementPlan.level(r.getLong(2)),paid=r.getInt(3);if(level<=paid)continue;
                    long amount=EngagementPlan.earnings(paid,level);total+=amount;
                    insert(c,id,"job",r.getString(1)+":"+level,"MONEY",String.valueOf(amount),1);
                    try(var u=c.prepareStatement("UPDATE profession_progress SET paid_level=? WHERE uuid=? AND job=?")){u.setInt(1,level);u.setObject(2,id);u.setString(3,r.getString(1));u.executeUpdate();}
                }}
            }c.commit();return total;
        }catch(Exception e){c.rollback();throw e;}}catch(SQLException e){throw new IllegalStateException(e);}
    }
    private Reward row(ResultSet r)throws SQLException{return new Reward(r.getObject("id",UUID.class),r.getObject("uuid",UUID.class),r.getString("source"),r.getString("claim_key"),r.getString("kind"),r.getString("value"),r.getInt("amount"),r.getString("state"),r.getString("payload"));}
    public List<Reward> pending(){var out=new ArrayList<Reward>();try(var c=connect();var p=c.prepareStatement("SELECT * FROM engagement_rewards WHERE state IN ('PENDING','READY','DELIVERING') AND EXISTS(SELECT 1 FROM players p WHERE p.uuid=engagement_rewards.uuid AND p.is_online) ORDER BY created_at LIMIT 100");var r=p.executeQuery()){while(r.next())out.add(row(r));return out;}catch(SQLException e){throw new IllegalStateException(e);}}
    public boolean prepare(UUID id,String payload){try(var c=connect();var p=c.prepareStatement("UPDATE engagement_rewards SET payload=?::jsonb,state='READY' WHERE id=? AND state='PENDING'")){p.setString(1,payload);p.setObject(2,id);return p.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
    public boolean state(UUID id,String from,String to){if(!Set.of("READY","DELIVERING","DELIVERED","REVIEW").contains(to))throw new IllegalArgumentException();try(var c=connect();var p=c.prepareStatement("UPDATE engagement_rewards SET state=?,delivered_at=CASE WHEN ?='DELIVERED' THEN now() ELSE delivered_at END WHERE id=? AND state=?")){p.setString(1,to);p.setString(2,to);p.setObject(3,id);p.setString(4,from);return p.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
}
