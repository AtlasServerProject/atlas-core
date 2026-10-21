package io.atlas.modules.pokedex.model;
import java.util.*;
public final class DexRewardPlan {
 public record Part(String kind,String value,int amount){}
 private static Part item(String name,int amount){return new Part("ITEM","cobblemon:"+name,amount);}
 public static List<Part> parts(int mark){return switch(mark){
 case 10->List.of(item("poke_ball",16));case 20->List.of(item("great_ball",16));case 30->List.of(item("ultra_ball",8));case 40->List.of(item("rare_candy",8));
 case 50->List.of(item("ultra_ball",32),item("quick_ball",8));
 case 60->List.of(item("quick_ball",32),item("dusk_ball",32),item("rare_candy",16));
 case 70->List.of(item("rare_candy",32),new Part("MINT","",1),new Part("MINT","",1),item("ability_capsule",1));
 case 80->List.of(new Part("SHINY","",1),new Part("SHINY","",1),new Part("SHINY","",1));
 case 90->List.of(new Part("LEGENDARY","",1),item("rare_candy",64),new Part("MINT","",1),new Part("MINT","",1),new Part("MINT","",1),item("ability_patch",2));
 case 100->List.of(new Part("LEGENDARY_CHOICE","",1));default->throw new IllegalArgumentException("Invalid milestone");};}
 public static final Set<String> MINTS=Set.of("lonely","adamant","naughty","brave","bold","impish","lax","relaxed","modest","mild","rash","quiet","calm","gentle","careful","sassy","timid","hasty","jolly","naive","serious");
}
