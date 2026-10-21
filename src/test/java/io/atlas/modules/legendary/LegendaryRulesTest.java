package io.atlas.modules.legendary;
import io.atlas.modules.legendary.model.LegendaryRules;
import java.util.*;
public final class LegendaryRulesTest {
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        long interval=18000000;
        var future=LegendaryRules.recover(interval,1000,interval);check(future.next()==interval&&future.missed()==0,"restart preserves future timer");
        var equal=LegendaryRules.recover(interval,interval,interval);check(equal.next()==2*interval&&equal.missed()==1,"exact overdue boundary");
        var missed=LegendaryRules.recover(interval,3*interval+100,interval);check(missed.next()==4*interval&&missed.missed()==3,"skip missed cycles without replay");
        check(LegendaryRules.timeMatches("DAY",0)&&!LegendaryRules.timeMatches("DAY",12000),"day boundaries");
        check(LegendaryRules.timeMatches("DAWN",23500)&&LegendaryRules.timeMatches("DAWN",500),"wraparound dawn");
        check(LegendaryRules.timeMatches("NIGHT",18000)&&!LegendaryRules.timeMatches("NIGHT",6000),"night eligibility");
        check(LegendaryRules.weatherMatches("THUNDER",true,true)&&!LegendaryRules.weatherMatches("CLEAR",true,false),"weather eligibility");
        check(LegendaryRules.select(List.of(0.,0.),new Random(1))==-1,"zero weight excludes events");
        Random random=new Random(314159);int selected=0;for(int i=0;i<100000;i++)if(LegendaryRules.select(List.of(1.,3.),random)==1)selected++;
        check(selected>74000&&selected<76000,"weighted selection distribution");
        check(LegendaryRules.percentage(176).equals("94,62")&&LegendaryRules.percentage(185).equals("99,46"),"real IV percentage Portuguese");
        check(LegendaryRules.quality(186).equals("IVs máximos")&&LegendaryRules.quality(185).equals("Quase perfeito")&&LegendaryRules.quality(0).equals("Comum"),"IV classifications");
        check(LegendaryRules.eventProgress(100,200,50)==1,"progress clamped before start");
        check(LegendaryRules.eventProgress(100,200,150)==.5f,"half countdown");
        check(LegendaryRules.eventProgress(100,200,250)==0,"progress clamped after expiry");
        check(LegendaryRules.liveOutcome(1,true,false,200,200)==null,"battle blocks expiry");
        check(LegendaryRules.liveOutcome(0,false,true,200,200)==null,"capture blocks defeat/expiry");
        check("EXPIRED".equals(LegendaryRules.liveOutcome(1,false,false,200,200)),"distinct expiration");
        check("DEFEATED".equals(LegendaryRules.liveOutcome(0,false,false,200,200)),"defeat precedes expiry");
        System.out.println("LegendaryRulesTest PASS: calendar restart/missed cycles, time/weather, weighting, IVs");
    }
}
