package io.atlas.modules.pokedex.model;
import java.util.*;
public final class TypeRoutePlan {
 public static final List<String> TYPES=List.of("normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy");
 public static final List<String> ROUTES=java.util.stream.Stream.concat(TYPES.stream(),GenerationRoutePlan.ROUTES.stream()).toList();
 public static final List<Integer> MARKS=List.of(10,25,50,75,100);
 public static String name(String type){if(GenerationRoutePlan.ROUTES.contains(type))return GenerationRoutePlan.name(type);return switch(type){case "normal"->"Normal";case "fire"->"Fogo";case "water"->"Água";case "electric"->"Elétrico";case "grass"->"Planta";case "ice"->"Gelo";case "fighting"->"Lutador";case "poison"->"Veneno";case "ground"->"Terra";case "flying"->"Voador";case "psychic"->"Psíquico";case "bug"->"Inseto";case "rock"->"Pedra";case "ghost"->"Fantasma";case "dragon"->"Dragão";case "dark"->"Sombrio";case "steel"->"Aço";case "fairy"->"Fada";default->throw new IllegalArgumentException("Invalid type");};}
 public static String title(String type){return type.equals("dragon")?"Mestre dos Dragões":"Mestre do tipo "+name(type);}
 public static int milestone(int caught,int total){if(total<=0)return 0;long percentage=(long)caught*100/total;int mark=0;for(int m:MARKS)if(percentage>=m)mark=m;return mark;}
 public static List<DexRewardPlan.Part> parts(int mark,String type){if(GenerationRoutePlan.ROUTES.contains(type))return GenerationRoutePlan.parts(mark,type);if(!TYPES.contains(type))throw new IllegalArgumentException();return switch(mark){case 10->List.of(new DexRewardPlan.Part("ITEM","cobblemon:great_ball",8));case 25->List.of(new DexRewardPlan.Part("ITEM","cobblemon:ultra_ball",8));case 50->List.of(new DexRewardPlan.Part("ITEM","cobblemon:rare_candy",8));case 75->List.of(new DexRewardPlan.Part("MINT","",1));case 100->List.of(new DexRewardPlan.Part("SHINY",type,1));default->throw new IllegalArgumentException("Invalid type milestone");};}
}
