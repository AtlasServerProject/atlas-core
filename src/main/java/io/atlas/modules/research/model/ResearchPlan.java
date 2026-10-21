package io.atlas.modules.research.model;
import java.util.*;
public final class ResearchPlan {
 public record Stage(String track,int stage,int target,long money){}
 public static final List<String> TRACKS=List.of("captures","species","types");
 public static String name(String track){return switch(track){case "captures"->"Expedição de campo";case "species"->"Diversidade Pokémon";case "types"->"Estudo dos tipos";default->throw new IllegalArgumentException();};}
 public static String objective(String track){return switch(track){case "captures"->"Pokémon capturados";case "species"->"espécies diferentes capturadas";case "types"->"tipos diferentes capturados";default->throw new IllegalArgumentException();};}
 public static List<Stage> stages(String track){int[] targets;long[] rewards;switch(track){case "captures"->{targets=new int[]{5,15,30,50,100};rewards=new long[]{250,600,1200,2000,5000};}case "species"->{targets=new int[]{5,10,25,50,100};rewards=new long[]{500,1000,2500,5000,10000};}case "types"->{targets=new int[]{2,4,8,12,18};rewards=new long[]{300,800,1800,3000,6000};}default->throw new IllegalArgumentException();}var out=new ArrayList<Stage>();for(int i=0;i<5;i++)out.add(new Stage(track,i+1,targets[i],rewards[i]));return List.copyOf(out);}
 public static Stage stage(String track,int number){if(number<1||number>5)throw new IllegalArgumentException();return stages(track).get(number-1);}
}
