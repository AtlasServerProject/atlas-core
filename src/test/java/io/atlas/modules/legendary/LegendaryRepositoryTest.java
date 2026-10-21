package io.atlas.modules.legendary;

import io.atlas.modules.legendary.repository.LegendaryRepository;
import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.*;

/** Explicit opt-in integration runner: isolated dev DB only, restores scheduler and deletes own fixtures. */
public final class LegendaryRepositoryTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        if(!DatabaseConfig.DATABASE.equals("atlas_dev")||DatabaseConfig.PORT!=25432)throw new IllegalStateException("Test requires isolated atlas_dev DB");
        List<UUID> ids=new ArrayList<>();
        try(var first=new LegendaryRepository();var second=new LegendaryRepository();var c=DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD)){
            long now=System.currentTimeMillis();var before=first.load(now+18000000,true).get();
            check(first.active().get().isEmpty(),"dev server must have no active event");
            try{
                first.recover(before.next(),now+10000,0).get();
                check(first.warn(now+10000).get()&&!second.warn(now+10000).get(),"one warning per cycle across workers");
                UUID one=UUID.randomUUID(),two=UUID.randomUUID();ids.add(one);ids.add(two);
                var a=first.claim(one,now+10000,now,now+18000000,"AUTO");var b=second.claim(two,now+10000,now,now+18000000,"AUTO");
                boolean wonA=a.get(),wonB=b.get();check(wonA!=wonB,"CAS prevents duplicate global cycle");UUID winner=wonA?one:two;
                UUID other=UUID.randomUUID();ids.add(other);check(first.claim(other,0,now,0,"TEST").get(),"test attempt ledger");
                var event=new LegendaryRepository.Event(winner,UUID.randomUUID(),UUID.randomUUID(),"mew","atlas:survival_emerald","minecraft:jungle",1,70,1,now,now+900000,"PENDING",true);
                var otherEvent=new LegendaryRepository.Event(other,UUID.randomUUID(),UUID.randomUUID(),"mew","atlas:survival_emerald","minecraft:jungle",2,70,2,now,now+900000,"PENDING",true);
                var reserveA=first.reserve(event,1);var reserveB=second.reserve(otherEvent,1);
                boolean reservedA=reserveA.get(),reservedB=reserveB.get();check(reservedA!=reservedB,"global active limit transaction");
                if(!reservedA){event=otherEvent;winner=other;}
                check(second.active().get().size()==1&&second.active().get().getFirst().pokemon().equals(event.pokemon()),"pending event reload retains original UUID");
                var manualEvent=reservedA?otherEvent:new LegendaryRepository.Event(one,UUID.randomUUID(),UUID.randomUUID(),"mew","atlas:survival_emerald","minecraft:jungle",2,70,2,now,now+900000,"PENDING",true);
                if(!reservedA&&!wonA)check(first.claim(one,0,now,0,"TEST").get(),"manual ledger");
                check(!first.reserve(manualEvent,1).get(),"automatic blocked while active");
                check(first.reserve(manualEvent,1,true).get(),"manual spawn bypasses active cap");
                check(first.active().get().size()==2,"both events retained");
                check(first.transition(manualEvent.id(),"DESPAWNED").get(),"manual cleanup");
                check(first.transition(winner,"ACTIVE").get(),"active confirmation");
                check(!first.capture(UUID.randomUUID(),UUID.randomUUID(),"{}").get(),"external capture ignored");
                check(first.capture(event.pokemon(),UUID.randomUUID(),"{\"total\":176}").get(),"legitimate tracked capture accepted");
                check(!second.capture(event.pokemon(),UUID.randomUUID(),"{}").get(),"duplicate capture blocked");
                check(first.active().get().isEmpty(),"capture frees active limit");
                for(String outcome:List.of("DEFEATED","DESPAWNED","LOST","EXPIRED")){
                    UUID eventId=UUID.randomUUID();ids.add(eventId);check(first.claim(eventId,0,now,0,"TEST").get(),"outcome attempt");
                    var ending=new LegendaryRepository.Event(eventId,UUID.randomUUID(),UUID.randomUUID(),"mew","atlas:survival_emerald","minecraft:jungle",1,70,1,now,now+1000,"PENDING",true);
                    check(first.reserve(ending,1).get(),"outcome reservation");check(first.transition(eventId,"ACTIVE").get(),"outcome active");
                    check(first.transition(eventId,outcome).get(),"distinct ending "+outcome);
                    check(!second.transition(eventId,outcome).get(),"duplicate ending blocked");
                    check(!first.capture(ending.pokemon(),UUID.randomUUID(),"{}").get(),"capture after ending rejected");
                    check(outcome.equals(first.status(ending.pokemon()).get()),"ending persisted across repositories");
                }
                check(first.load(0,false).get().next()==now+18000000,"restart does not reset timer");
                check(!first.enabled(false).get().enabled()&&!second.load(0,true).get().enabled(),"disable persists independently of file default");
                System.out.println("LegendaryRepositoryTest PASS: persisted timer, one warning, concurrent cycle/limit, UUID recovery, external/duplicate capture, disable");
            }finally{
                for(UUID id:ids){try(var p=c.prepareStatement("DELETE FROM legendary_events WHERE id=?")){p.setObject(1,id);p.executeUpdate();}try(var p=c.prepareStatement("DELETE FROM legendary_attempts WHERE id=?")){p.setObject(1,id);p.executeUpdate();}}
                try(var p=c.prepareStatement("UPDATE legendary_scheduler SET last_attempt=?,next_attempt=?,warned=?,enabled=?,missed_cycles=? WHERE id=1")){
                    p.setLong(1,before.last());p.setLong(2,before.next());p.setBoolean(3,before.warned());p.setBoolean(4,before.enabled());p.setLong(5,before.missed());p.executeUpdate();}
            }
        }
    }
}
