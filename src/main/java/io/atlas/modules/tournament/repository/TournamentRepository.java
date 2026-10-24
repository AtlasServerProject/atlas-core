package io.atlas.modules.tournament.repository;
import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.*;

public final class TournamentRepository {
 public record Event(UUID id,String slug,String title,String state,int count,String error,int capacity,java.time.Instant deadline,String format) {
  public String url(){return "https://challonge.com/"+slug;}
 }
 public record Notice(long id,String message) {}
 private Connection connect()throws SQLException {
  Properties p=new Properties();p.setProperty("user",DatabaseConfig.USER);p.setProperty("password",DatabaseConfig.PASSWORD);p.setProperty("connectTimeout","3");p.setProperty("socketTimeout","10");
  return DriverManager.getConnection(DatabaseConfig.jdbcUrl(),p);
 }
 private Event row(ResultSet r)throws SQLException{return new Event(r.getObject("id",UUID.class),r.getString("slug"),r.getString("title"),r.getString("state"),r.getInt("n"),r.getString("last_error"),r.getInt("capacity"),r.getTimestamp("registration_deadline").toInstant(),r.getString("format"));}
 private static final String LIVE="SELECT t.*, (SELECT count(*) FROM atlas_tournament_entries e WHERE e.tournament_id=t.id AND e.active) n FROM atlas_tournaments t WHERE state NOT IN ('CANCELLED','COMPLETE') ORDER BY created_at DESC LIMIT 1";
 public Optional<Event> active(){try(var c=connect();var p=c.prepareStatement(LIVE);var r=p.executeQuery()){return r.next()?Optional.of(row(r)):Optional.empty();}catch(SQLException e){throw new IllegalStateException("Falha ao consultar torneio",e);}}
 public UUID create(String actor){return create(actor,"OU",16,1800);}
 public UUID create(String actor,String tier,int capacity,long seconds){
  tier=io.atlas.modules.tournament.service.TournamentOptions.tier(tier);io.atlas.modules.tournament.service.TournamentOptions.capacity(capacity);
  if(seconds<60||seconds>86400)throw new IllegalArgumentException("Prazo deve estar entre 1m e 24h.");
  UUID id=UUID.randomUUID();try(var c=connect();var p=c.prepareStatement("INSERT INTO atlas_tournaments(id,slug,title,format,capacity,state,created_by,registration_deadline) VALUES(?,?,?,?,?,'CREATING',?,clock_timestamp()+(? * interval '1 second'))")){
   p.setObject(1,id);p.setString(2,"atlas"+id.toString().replace("-",""));p.setString(3,"Torneio "+tier+" GEN 9");p.setString(4,"atlasgen9"+tier.toLowerCase(java.util.Locale.ROOT));p.setInt(5,capacity);p.setString(6,actor);p.setLong(7,seconds);p.executeUpdate();return id;
  }catch(SQLException e){if("23505".equals(e.getSQLState()))throw new IllegalArgumentException("Já existe um torneio em preparação ou andamento.");throw new IllegalStateException("Falha ao criar torneio",e);}
 }
 private void expire(Connection c)throws SQLException{
  try(var p=c.prepareStatement("UPDATE atlas_tournaments t SET state='CANCELLED',last_error=NULL,updated_at=clock_timestamp() WHERE state IN ('CREATING','OPEN','REVIEW') AND registration_deadline<=clock_timestamp() AND (SELECT count(*) FROM atlas_tournament_entries e WHERE e.tournament_id=t.id AND e.active)<capacity RETURNING id,title,capacity" );var r=p.executeQuery()){
   while(r.next()){
    UUID id=r.getObject("id",UUID.class);
    try(var q=c.prepareStatement("UPDATE atlas_tournament_announcements SET delivered=true WHERE tournament_id=? AND NOT delivered")){q.setObject(1,id);q.executeUpdate();}
    notice(c,id,r.getString("title")+" cancelado: o prazo de inscrição terminou sem preencher as "+r.getInt("capacity")+" vagas.");
   }
  }
 }
 public void expire(){try(var c=connect()){c.setAutoCommit(false);try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(13842016)")){lock.execute();}expire(c);c.commit();}catch(SQLException e){throw new IllegalStateException("Falha ao verificar prazo",e);}}
 private void notice(Connection c,UUID id,String text)throws SQLException{try(var p=c.prepareStatement("INSERT INTO atlas_tournament_announcements(tournament_id,message) VALUES(?,?)")){p.setObject(1,id);p.setString(2,text);p.executeUpdate();}}
 public String enter(UUID player,String name){return change(player,name,true);}
 public String leave(UUID player){return change(player,"",false);}
 private String change(UUID player,String name,boolean join){
  try(var c=connect()) {c.setAutoCommit(false); try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(13842016)")){lock.execute();}expire(c);
   try(var p=c.prepareStatement(LIVE+" FOR UPDATE OF t");var r=p.executeQuery()){
    if(!r.next()){c.commit();return "Não há inscrições abertas.";}
    Event e=row(r);if(!e.state.equals("OPEN"))return "As inscrições não estão abertas. Consulte /torneio.";
    boolean existing=false;try(var q=c.prepareStatement("SELECT active FROM atlas_tournament_entries WHERE tournament_id=? AND player_uuid=?")){q.setObject(1,e.id);q.setObject(2,player);try(var x=q.executeQuery()){existing=x.next()&&x.getBoolean(1);}}
    if(join&&existing)return "Você já está inscrito. Participantes: "+e.count+"/"+e.capacity+".";
    if(!join&&!existing)return "Você não está inscrito.";
    if(join&&e.count>=e.capacity)return "As "+e.capacity+" vagas já foram preenchidas.";
    int count=e.count+(join?1:-1);
    if(join){try(var q=c.prepareStatement("INSERT INTO atlas_tournament_entries(tournament_id,player_uuid,player_name) VALUES(?,?,?) ON CONFLICT(tournament_id,player_uuid) DO UPDATE SET active=true,player_name=excluded.player_name,joined_at=now()")){q.setObject(1,e.id);q.setObject(2,player);q.setString(3,name);q.executeUpdate();}}
    else try(var q=c.prepareStatement("UPDATE atlas_tournament_entries SET active=false WHERE tournament_id=? AND player_uuid=?")){q.setObject(1,e.id);q.setObject(2,player);q.executeUpdate();}
    try(var q=c.prepareStatement("UPDATE atlas_tournaments SET revision=revision+1,state=?,retry_at=CASE WHEN last_error IS NULL THEN now() ELSE retry_at END,updated_at=now() WHERE id=?")){q.setString(1,count==e.capacity?"FULL":"OPEN");q.setObject(2,e.id);q.executeUpdate();}
    notice(c,e.id,join?name+" entrou no "+e.title+"! Participantes: "+count+"/"+e.capacity+".":"Uma vaga foi liberada no "+e.title+". Participantes: "+count+"/"+e.capacity+".");
    if(count==e.capacity)notice(c,e.id,count+"/"+e.capacity+" participantes! Inscrições encerradas. Preparando as chaves no Challonge; aguarde a confirmação da equipe antes de batalhar.");
    c.commit();return join?"Inscrição recebida! Participantes: "+count+"/"+e.capacity+". A sincronização com o Challonge é automática.":"Você saiu do torneio.";
   }
  }catch(SQLException e){throw new IllegalStateException("Falha ao atualizar inscrição",e);}
 }
 public String cancel(){try(var c=connect()){c.setAutoCommit(false); try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(13842016)")){lock.execute();}try(var p=c.prepareStatement(LIVE+" FOR UPDATE OF t");var r=p.executeQuery()){
  if(!r.next())return "Nenhum torneio ativo.";Event e=row(r);
  try(var q=c.prepareStatement("UPDATE atlas_tournaments SET state='CANCELLED',updated_at=now() WHERE id=?")){q.setObject(1,e.id);q.executeUpdate();}
  notice(c,e.id,e.title+" foi cancelado pela equipe. O histórico no Challonge será preservado.");c.commit();return "Torneio cancelado no Atlas. O registro externo foi preservado.";
 }}catch(SQLException e){throw new IllegalStateException("Falha ao cancelar torneio",e);}}
 public String close(){try(var c=connect()){c.setAutoCommit(false);try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(13842016)")){lock.execute();}
 try(var p=c.prepareStatement(LIVE+" FOR UPDATE OF t");var r=p.executeQuery()){
 if(!r.next())return "Nenhum torneio ativo.";Event e=row(r);if(!e.state.equals("STARTED"))return "Só é possível encerrar um torneio com chaves iniciadas. Use cancelar para interromper inscrições.";
 try(var q=c.prepareStatement("UPDATE atlas_tournaments SET state='COMPLETE',updated_at=now() WHERE id=?")){q.setObject(1,e.id);q.executeUpdate();}
 notice(c,e.id,"A equipe encerrou o "+e.title+" no Atlas. Resultados e histórico: "+e.url());c.commit();return "Encerrado no Atlas. Confira e finalize os resultados no Challonge pela conta organizadora.";
 }}catch(SQLException e){throw new IllegalStateException("Falha ao encerrar torneio",e);}}
 public List<Notice> pending(){try(var c=connect();var p=c.prepareStatement("SELECT id,message FROM atlas_tournament_announcements WHERE NOT delivered ORDER BY id LIMIT 30");var r=p.executeQuery()){List<Notice> n=new ArrayList<>();while(r.next())n.add(new Notice(r.getLong(1),r.getString(2)));return n;}catch(SQLException e){throw new IllegalStateException(e);}}
 public void delivered(long id){try(var c=connect();var p=c.prepareStatement("UPDATE atlas_tournament_announcements SET delivered=true WHERE id=?")){p.setLong(1,id);p.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
}
