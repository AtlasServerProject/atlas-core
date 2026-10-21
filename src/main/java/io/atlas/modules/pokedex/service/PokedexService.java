package io.atlas.modules.pokedex.service;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.pokemon.Species;
import io.atlas.modules.pokedex.repository.PokedexRepository;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class PokedexService {
 private final PokedexRepository repository=new PokedexRepository();
 private final Map<UUID,PokedexRepository.Progress> cache=new HashMap<>();
 private int ticks;
 public static String region(int n){return n<=0?"outros":n<=151?"kanto":n<=251?"johto":n<=386?"hoenn":n<=493?"sinnoh":n<=649?"unova":n<=721?"kalos":n<=809?"alola":n<=905?"galar":n<=1025?"paldea":"outros";}
 public static int milestone(int caught,int total){return total<=0?0:(int)Math.min(100,(long)caught*100/total)/10*10;}
 public PokedexRepository.Progress progress(ServerPlayer player){refresh(player);return cache.computeIfAbsent(player.getUUID(),repository::load);}
 public boolean researcher(UUID id){return cache.computeIfAbsent(id,repository::load).milestone()>=50;}
 public boolean professor(UUID id){var p=cache.computeIfAbsent(id,repository::load);return p.completed()!=null;}
 private List<Species> species(){return PokemonSpecies.getImplemented().stream().filter(s->s.getNationalPokedexNumber()>0).toList();}
 // Cobblemon 1.8 renamed CAUGHT to OWNED; resolve once, only when the native dex is read.
 private static final class CaughtProgress {
  private static final PokedexEntryProgress VALUE=resolve();
  private static PokedexEntryProgress resolve(){
   try{return PokedexEntryProgress.valueOf("OWNED");}
   catch(IllegalArgumentException legacyVersion){return PokedexEntryProgress.valueOf("CAUGHT");}
  }
 }
 private Set<net.minecraft.resources.ResourceLocation> caught(ServerPlayer p){var result=new HashSet<net.minecraft.resources.ResourceLocation>();Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(p).getSpeciesRecords().forEach((id,record)->{if(record.hasAtLeast(CaughtProgress.VALUE))result.add(id);});return result;}
 public void refresh(ServerPlayer player){
  if(!io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(player.getUUID()))return;
  var all=species();if(all.isEmpty())return;var caught=caught(player);int count=(int)all.stream().filter(s->caught.contains(s.getResourceIdentifier())).count();
  io.atlas.modules.pokedex.PokedexModule.types().refresh(player,all,caught);
  var old=cache.computeIfAbsent(player.getUUID(),repository::load);int mark=milestone(count,all.size());
  io.atlas.modules.pokedex.PokedexModule.rewards().unlock(player,Math.max(old.milestone(),mark));
  if(old.caught()==count&&old.total()==all.size()&&old.milestone()>=mark)return;
  var next=repository.save(player.getUUID(),player.getGameProfile().getName(),mark,count,all.size());cache.put(player.getUUID(),next);
  if(mark>old.milestone()){
   for(int step=old.milestone()+10;step<=mark;step+=10)player.sendSystemMessage(Component.literal("§b§l✦ ACADEMIA ATLAS §r§7• Pokédex: §e"+step+"% §7• /academia premios"));
   if(old.completed()==null&&next.completed()!=null){player.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§6§l✦ PROFESSOR ATLAS §r§f"+player.getGameProfile().getName()+" §bconcluiu a Pokédex!"),false);player.getServer().getCommands().sendCommands(player);}
  }
 }
 public void tick(net.minecraft.server.MinecraftServer server){if(++ticks<100)return;ticks=0;for(var player:server.getPlayerList().getPlayers())try{refresh(player);}catch(Exception e){io.atlas.AtlasMod.LOGGER.warn("Falha ao atualizar Pokédex de {}",player.getUUID(),e);}}
 public void remove(UUID id){cache.remove(id);}
 public int show(ServerPlayer player,String filter){
  refresh(player);var all=species();var owned=caught(player);java.util.function.Predicate<Species> predicate=s->true;
  if(filter.startsWith("regiao:")){String region=filter.substring(7).toLowerCase(Locale.ROOT);if(!Set.of("kanto","johto","hoenn","sinnoh","unova","kalos","alola","galar","paldea","outros").contains(region)){player.sendSystemMessage(Component.literal("§cRegião inválida."));return 0;}predicate=s->region(s.getNationalPokedexNumber()).equals(region);}
  if(filter.startsWith("tipo:")){String type=normalizeType(filter.substring(5));if(!Set.of("normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy").contains(type)){player.sendSystemMessage(Component.literal("§cTipo inválido."));return 0;}predicate=s->s.getPrimaryType().getName().equalsIgnoreCase(type)||s.getSecondaryType()!=null&&s.getSecondaryType().getName().equalsIgnoreCase(type);}
  var selected=all.stream().filter(predicate).toList();long count=selected.stream().filter(s->owned.contains(s.getResourceIdentifier())).count();double percent=selected.isEmpty()?0:count*100.0/selected.size();
  player.sendSystemMessage(Component.literal("§b§l✦ ACADEMIA ATLAS §r§7• "+filter));
  player.sendSystemMessage(Component.literal(String.format(Locale.forLanguageTag("pt-BR"),"§fCapturados: §e%d/%d §7• §b%.2f%%",count,selected.size(),percent)));
  var progress=cache.get(player.getUUID());player.sendSystemMessage(Component.literal(professor(player.getUUID())?"§6Professor Atlas §7• /pc • /pokeheal • /checkspawns legendary":"§7Próximo marco global: §e"+Math.min(100,progress.milestone()+10)+"%"));return 1;
 }
 private String normalizeType(String type){return switch(type.toLowerCase(Locale.ROOT)){case "dragoes","dragao","dragões","dragão"->"dragon";case "fadas","fada"->"fairy";case "fantasmas","fantasma"->"ghost";case "fogo"->"fire";case "agua","água"->"water";case "eletrico","elétrico"->"electric";case "planta"->"grass";case "gelo"->"ice";case "lutador"->"fighting";case "veneno"->"poison";case "terra"->"ground";case "voador"->"flying";case "psiquico","psíquico"->"psychic";case "inseto"->"bug";case "pedra"->"rock";case "sombrio"->"dark";case "aco","aço"->"steel";default->type.toLowerCase(Locale.ROOT);};}
 public int hall(ServerPlayer player,int page){player.sendSystemMessage(Component.literal("§6§l✦ HALL DOS PROFESSORES §r§7• Página "+(page+1)));var entries=repository.hall(page);if(entries.isEmpty())player.sendSystemMessage(Component.literal("§7Nenhum professor nesta página."));for(var line:entries)player.sendSystemMessage(Component.literal("§e"+line));return 1;}
}
