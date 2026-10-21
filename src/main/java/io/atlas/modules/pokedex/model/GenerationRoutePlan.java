package io.atlas.modules.pokedex.model;
import java.util.*;
public final class GenerationRoutePlan {
 public static final List<String> ROUTES=List.of("gen1","gen2","gen3","gen4","gen5","gen6","gen7","gen8","gen9");
 private static final List<String> REGIONS=List.of("Kanto","Johto","Hoenn","Sinnoh","Unova","Kalos","Alola","Galar / Hisui","Paldea");
 private static final List<List<String>> STARTERS=List.of(List.of("bulbasaur","charmander","squirtle"),List.of("chikorita","cyndaquil","totodile"),List.of("treecko","torchic","mudkip"),List.of("turtwig","chimchar","piplup"),List.of("snivy","tepig","oshawott"),List.of("chespin","fennekin","froakie"),List.of("rowlet","litten","popplio"),List.of("grookey","scorbunny","sobble"),List.of("sprigatito","fuecoco","quaxly"));
 public static String name(String route){int index=ROUTES.indexOf(route);if(index<0)throw new IllegalArgumentException("Invalid generation");return "Geração "+(index+1)+" — "+REGIONS.get(index);}
 public static List<String> starters(String route){int index=ROUTES.indexOf(route);if(index<0)throw new IllegalArgumentException("Invalid generation");return STARTERS.get(index);}
 public static int generation(int number){return number<=0?0:number<=151?1:number<=251?2:number<=386?3:number<=493?4:number<=649?5:number<=721?6:number<=809?7:number<=905?8:number<=1025?9:0;}
 public static boolean matches(int number,String route){return ROUTES.contains(route)&&generation(number)==ROUTES.indexOf(route)+1;}
 public static List<DexRewardPlan.Part> parts(int mark,String route){if(!ROUTES.contains(route))throw new IllegalArgumentException();return switch(mark){case 10->List.of(new DexRewardPlan.Part("ITEM","cobblemon:great_ball",8));case 25->List.of(new DexRewardPlan.Part("ITEM","cobblemon:ultra_ball",8));case 50->List.of(new DexRewardPlan.Part("ITEM","cobblemon:rare_candy",8));case 75->List.of(new DexRewardPlan.Part("MINT","",1));case 100->List.of(new DexRewardPlan.Part("SHINY",route,1));default->throw new IllegalArgumentException("Invalid generation milestone");};}
}
