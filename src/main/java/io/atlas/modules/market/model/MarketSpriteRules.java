package io.atlas.modules.market.model;
import java.util.*;
public final class MarketSpriteRules {
 public record Sprite(int id,String path,String sha256){}
 public static String normalize(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace("alolan","alola").replace("galarian","galar").replace("hisuian","hisui").replace("paldean","paldea").replace("gmax","gigantamax").replace("percent","").replaceAll("[^a-z0-9]","");}
 public static Optional<Sprite> select(Collection<Sprite> sprites,int dex,String species,Collection<String> aspects,String gender,String form,boolean shiny){
  String base=normalize(species);var wanted=new HashSet<String>();for(var aspect:aspects)add(wanted,aspect);add(wanted,gender);add(wanted,form);
  Sprite best=null;int bestScore=-1;
  for(var sprite:sprites){String filename=sprite.path().substring(sprite.path().lastIndexOf('/')+1).replace(".png","");boolean imageShiny=filename.endsWith("_shiny");if(imageShiny!=shiny)continue;if(imageShiny)filename=filename.substring(0,filename.length()-6);var parts=new ArrayList<>(Arrays.asList(filename.split("_")));if(parts.getFirst().matches("[0-9]+")){int number=Integer.parseInt(parts.removeFirst());if(dex>0&&number!=dex)continue;}String prefix="";while(!parts.isEmpty()&&prefix.length()<base.length())prefix+=normalize(parts.removeFirst());if(!prefix.equals(base))continue;boolean valid=true;int score=0;for(String part:parts){String token=normalize(part);if(token.isEmpty()||token.equals("normal")||token.equals("standard"))continue;if(!wanted.contains(token)){valid=false;break;}score++;}if(valid&&score>bestScore){best=sprite;bestScore=score;}}
  return Optional.ofNullable(best);
 }
 private static void add(Set<String> target,String value){target.add(normalize(value));for(String piece:value.split("[^a-zA-Z0-9]+"))target.add(normalize(piece));}
}
