package io.atlas.modules.pokedex.service;
import io.atlas.modules.pokedex.model.TypeRoutePlan;
import io.atlas.modules.pokedex.repository.TypeRouteRepository;
import io.atlas.modules.pokedex.repository.TypeRouteRepository.Progress;
import net.minecraft.server.level.ServerPlayer;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.*;
public final class TypeRouteService {
 private final TypeRouteRepository repository=new TypeRouteRepository();
 private final Map<UUID,List<Progress>> cache=new HashMap<>();
 public List<Progress> progress(UUID id){return cache.computeIfAbsent(id,repository::load);}
 public void remove(UUID id){cache.remove(id);}
 public String title(UUID id){return progress(id).stream().filter(Progress::selected).findFirst().map(p->TypeRoutePlan.title(p.type())).orElse(null);}
 public static boolean matches(Species s,String type){if(io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES.contains(type))return io.atlas.modules.pokedex.model.GenerationRoutePlan.matches(s.getNationalPokedexNumber(),type);return s.getPrimaryType().getName().equalsIgnoreCase(type)||s.getSecondaryType()!=null&&s.getSecondaryType().getName().equalsIgnoreCase(type);}
 public void refresh(ServerPlayer player,List<Species> species,Set<net.minecraft.resources.ResourceLocation> owned){var before=progress(player.getUUID());boolean changed=false;
  for(String type:TypeRoutePlan.ROUTES){var list=species.stream().filter(s->matches(s,type)).toList();if(list.isEmpty())continue;int count=(int)list.stream().filter(s->owned.contains(s.getResourceIdentifier())).count();int mark=TypeRoutePlan.milestone(count,list.size());var old=before.stream().filter(p->p.type().equals(type)).findFirst().orElse(null);
   io.atlas.modules.pokedex.PokedexModule.rewards().unlock(player,type,Math.max(mark,old==null?0:old.milestone()));
   if(old!=null&&old.caught()==count&&old.total()==list.size())continue;
   repository.save(player.getUUID(),type,mark,count,list.size());changed=true;
   if(mark>(old==null?0:old.milestone()))player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§bRota "+TypeRoutePlan.name(type)+" §7• Marco §e"+mark+"% §7liberado! /dex"));
  }
  if(changed)cache.put(player.getUUID(),repository.load(player.getUUID()));
 }
 public void select(ServerPlayer player,String type){if(!TypeRoutePlan.TYPES.contains(type))return;if(!io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(player.getUUID()))return;boolean selected=repository.select(player.getUUID(),type);cache.remove(player.getUUID());player.sendSystemMessage(net.minecraft.network.chat.Component.literal(selected?"§aTítulo selecionado: "+TypeRoutePlan.title(type):"§cComplete essa rota para usar o título."));}
}
