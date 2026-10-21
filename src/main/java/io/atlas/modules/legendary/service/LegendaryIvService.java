package io.atlas.modules.legendary.service;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.google.gson.*;
import java.io.*;
import java.util.*;
/** Global legendary/mythical policy, independent of acquisition channel. */
public final class LegendaryIvService {
 private static final Set<String> SPECIES=knownSpecies();
 private static final Stats[] STATS={Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED};
 private static final ThreadLocal<Boolean> APPLYING=ThreadLocal.withInitial(()->false);
 private LegendaryIvService(){}
 private static Set<String> knownSpecies(){var names=new HashSet<String>();try(var in=LegendaryIvService.class.getResourceAsStream("/atlas-legendary/legendary-spawns.json")){
  for(var e:JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray())names.add(e.getAsJsonObject().get("species").getAsString());
 }catch(Exception e){throw new IllegalStateException("Legendary species policy unavailable",e);}return Set.copyOf(names);}
 public static void ensure(Pokemon pokemon){
  if(pokemon==null||pokemon.isClient$common())return;
  var species=pokemon.getSpecies();if(species==null)return;
  var labels=species.getLabels();String id=species.getResourceIdentifier().getPath();
  if(!SPECIES.contains(id)&&!labels.contains("legendary")&&!labels.contains("mythical"))return;
  if(APPLYING.get())return;
  int[] values=new int[6];for(int i=0;i<6;i++)values[i]=pokemon.getIvs().get(STATS[i]);
  var uuid=pokemon.getUuid();int[] repaired=io.atlas.modules.legendary.model.LegendaryIvRules.ensure(values,new Random(uuid.getMostSignificantBits()^uuid.getLeastSignificantBits()));
  APPLYING.set(true);try{for(int i=0;i<6;i++)if(repaired[i]!=values[i])pokemon.setIV(STATS[i],repaired[i]);}finally{APPLYING.remove();}
 }
}
