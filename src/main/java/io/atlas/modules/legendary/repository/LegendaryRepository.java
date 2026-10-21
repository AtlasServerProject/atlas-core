package io.atlas.modules.legendary.repository;

import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Dedicated serialized JDBC worker; never reads world state off the server thread. */
public final class LegendaryRepository implements AutoCloseable {
    public record State(long last,long next,boolean warned,boolean enabled,long missed) {}
    public record Event(UUID id,UUID pokemon,UUID entity,String species,String dimension,String biome,
                        double x,double y,double z,long created,long expires,String status,boolean test) {}
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"atlas-legendary-db");t.setDaemon(true);return t;});
    private Connection connect()throws SQLException{return DriverManager.getConnection(DatabaseConfig.jdbcUrl()+"?connectTimeout=5&socketTimeout=10",DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
    @FunctionalInterface private interface Work<T>{T run(Connection c)throws Exception;}
    private <T>CompletableFuture<T> submit(Work<T> work){return CompletableFuture.supplyAsync(()->{try(Connection c=connect()){return work.run(c);}catch(Exception e){throw new CompletionException(e);}},worker);}
    public CompletableFuture<State> load(long first,boolean enabled){return submit(c->{
        try(var p=c.prepareStatement("INSERT INTO legendary_scheduler(id,next_attempt,enabled) VALUES(1,?,?) ON CONFLICT DO NOTHING")){p.setLong(1,first);p.setBoolean(2,enabled);p.executeUpdate();}
        return state(c);
    });}
    private State state(Connection c)throws SQLException{try(var p=c.prepareStatement("SELECT * FROM legendary_scheduler WHERE id=1");var r=p.executeQuery()){r.next();return new State(r.getLong("last_attempt"),r.getLong("next_attempt"),r.getBoolean("warned"),r.getBoolean("enabled"),r.getLong("missed_cycles"));}}
    public CompletableFuture<State> recover(long expected,long next,long missed){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_scheduler SET next_attempt=?,warned=FALSE,missed_cycles=missed_cycles+? WHERE id=1 AND next_attempt=?")){p.setLong(1,next);p.setLong(2,missed);p.setLong(3,expected);p.executeUpdate();}return state(c);});}
    public CompletableFuture<Boolean> warn(long expected){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_scheduler SET warned=TRUE WHERE id=1 AND next_attempt=? AND warned=FALSE")){p.setLong(1,expected);return p.executeUpdate()==1;}});}
    public CompletableFuture<State> enabled(boolean enabled){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_scheduler SET enabled=? WHERE id=1")){p.setBoolean(1,enabled);p.executeUpdate();}return state(c);});}
    public CompletableFuture<Boolean> claim(UUID id,long expected,long now,long next,String kind){return submit(c->{
        c.setAutoCommit(false);
        try {
            if(kind.equals("AUTO"))try(var p=c.prepareStatement("UPDATE legendary_scheduler SET last_attempt=?,next_attempt=?,warned=FALSE WHERE id=1 AND next_attempt=? AND enabled=TRUE")){p.setLong(1,now);p.setLong(2,next);p.setLong(3,expected);if(p.executeUpdate()!=1){c.rollback();return false;}}
            try(var p=c.prepareStatement("INSERT INTO legendary_attempts(id,attempted_at,kind,outcome) VALUES(?,?,?,'STARTED')")){p.setObject(1,id);p.setLong(2,now);p.setString(3,kind);p.executeUpdate();}
            c.commit();return true;
        }catch(Exception e){c.rollback();throw e;}
    });}
    public CompletableFuture<Void> outcome(UUID id,String outcome,String detail){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_attempts SET outcome=?,detail=? WHERE id=?")){p.setString(1,outcome);p.setString(2,detail);p.setObject(3,id);p.executeUpdate();}return null;});}
    public CompletableFuture<Boolean> reserve(Event e,int max){return reserve(e,max,false);}
    public CompletableFuture<Boolean> reserve(Event e,int max,boolean manual){return submit(c->{c.setAutoCommit(false);try{
        try(var p=c.prepareStatement("SELECT id FROM legendary_scheduler WHERE id=1 FOR UPDATE");var rs=p.executeQuery()){rs.next();}
        try(var p=c.prepareStatement("SELECT COUNT(*) FROM legendary_events WHERE status IN ('PENDING','ACTIVE')");var rs=p.executeQuery()){rs.next();if(!manual&&rs.getInt(1)>=max){c.rollback();return false;}}
        try(var p=c.prepareStatement("INSERT INTO legendary_events(id,pokemon_uuid,entity_uuid,species,dimension,biome,x,y,z,created_at,expires_at,status,test) VALUES(?,?,?,?,?,?,?,?,?,?,?,'PENDING',?)")){
            p.setObject(1,e.id);p.setObject(2,e.pokemon);p.setObject(3,e.entity);p.setString(4,e.species);p.setString(5,e.dimension);p.setString(6,e.biome);p.setDouble(7,e.x);p.setDouble(8,e.y);p.setDouble(9,e.z);p.setLong(10,e.created);p.setLong(11,e.expires);p.setBoolean(12,e.test);p.executeUpdate();}
        c.commit();return true;
    }catch(Exception x){c.rollback();throw x;}});}
    public CompletableFuture<Boolean> transition(UUID id,String status){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_events SET status=?,finished_at=CASE WHEN ?='ACTIVE' THEN NULL ELSE ? END WHERE id=? AND status IN ('PENDING','ACTIVE')")){p.setString(1,status);p.setString(2,status);p.setLong(3,System.currentTimeMillis());p.setObject(4,id);return p.executeUpdate()==1;}});}
    public CompletableFuture<Boolean> capture(UUID pokemon,UUID trainer,String json){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_events SET status='CAPTURED',trainer_uuid=?,capture_data=?::jsonb,finished_at=? WHERE pokemon_uuid=? AND status IN ('ACTIVE','PENDING')")){p.setObject(1,trainer);p.setString(2,json);p.setLong(3,System.currentTimeMillis());p.setObject(4,pokemon);return p.executeUpdate()==1;}});}
    public CompletableFuture<String> status(UUID pokemon){return submit(c->{try(var p=c.prepareStatement("SELECT status FROM legendary_events WHERE pokemon_uuid=?")){p.setObject(1,pokemon);try(var rs=p.executeQuery()){return rs.next()?rs.getString(1):null;}}});}
    public CompletableFuture<Void> moved(UUID id,double x,double y,double z){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_events SET x=?,y=?,z=? WHERE id=? AND status='ACTIVE'")){p.setDouble(1,x);p.setDouble(2,y);p.setDouble(3,z);p.setObject(4,id);p.executeUpdate();}return null;});}
    public CompletableFuture<Void> announced(UUID id){return submit(c->{try(var p=c.prepareStatement("UPDATE legendary_events SET announcement_sent=TRUE WHERE id=?")){p.setObject(1,id);p.executeUpdate();}return null;});}
    public CompletableFuture<List<Event>> active(){return submit(c->{List<Event> list=new ArrayList<>();try(var p=c.prepareStatement("SELECT * FROM legendary_events WHERE status IN ('PENDING','ACTIVE')");var r=p.executeQuery()){while(r.next())list.add(new Event(r.getObject("id",UUID.class),r.getObject("pokemon_uuid",UUID.class),r.getObject("entity_uuid",UUID.class),r.getString("species"),r.getString("dimension"),r.getString("biome"),r.getDouble("x"),r.getDouble("y"),r.getDouble("z"),r.getLong("created_at"),r.getLong("expires_at"),r.getString("status"),r.getBoolean("test")));}return list;});}
    @Override public void close(){worker.shutdown();try{worker.awaitTermination(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
