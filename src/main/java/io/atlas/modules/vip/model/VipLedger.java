package io.atlas.modules.vip.model;
import java.time.*;
import java.util.UUID;
/** Commercial time is independent of staff/manual ranks. Only the highest positive balance runs. */
public final class VipLedger {
 public static final long DAY_MS=86400000L;
 public record Delivery(UUID id,UUID leaseToken,String server,String mode,UUID subject,long corePlayerId,String plan,int days){}
 public record Balance(UUID subject,long corePlayerId,String server,long vip1Ms,long vip2Ms,long vip3Ms,Instant checkpoint){
  public int level(){return vip3Ms>0?3:vip2Ms>0?2:vip1Ms>0?1:0;}
 }
 public record Receipt(UUID deliveryId,UUID receiptId,UUID subject,long corePlayerId,String server,String mode,String plan,int days,Instant activatedAt){}
 public record Grant(Receipt receipt,Balance balance){}
 public static Balance advance(Balance b,Instant time){
  long elapsed=Math.max(0,Duration.between(b.checkpoint(),time).toMillis());long[] v={b.vip1Ms(),b.vip2Ms(),b.vip3Ms()};
  for(int i=2;i>=0;i--){long used=Math.min(elapsed,v[i]);v[i]-=used;elapsed-=used;}
  return new Balance(b.subject(),b.corePlayerId(),b.server(),v[0],v[1],v[2],time.isBefore(b.checkpoint())?b.checkpoint():time);
 }
 public static Balance add(Balance previous,int level,int days,Instant time){
  if(level<1||level>3||days!=30)throw new IllegalArgumentException("Unsupported VIP plan");var b=advance(previous,time);long[] v={b.vip1Ms(),b.vip2Ms(),b.vip3Ms()};v[level-1]=Math.addExact(v[level-1],days*DAY_MS);if(v[level-1]>3153600000000L)throw new IllegalArgumentException("VIP balance limit exceeded");return new Balance(b.subject(),b.corePlayerId(),b.server(),v[0],v[1],v[2],b.checkpoint());
 }
 public static int plan(String slug){return switch(slug){case "vip-1"->1;case "vip-2"->2;case "vip-3"->3;default->throw new IllegalArgumentException("Unsupported VIP plan");};}
}
