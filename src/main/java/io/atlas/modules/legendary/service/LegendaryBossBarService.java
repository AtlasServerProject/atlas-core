package io.atlas.modules.legendary.service;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.legendary.model.LegendaryRules;
import io.atlas.modules.legendary.repository.LegendaryRepository.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class LegendaryBossBarService {
 private final Map<UUID,ServerBossEvent> bars=new HashMap<>();
 public void update(MinecraftServer server,Event event,String name,long now,boolean waiting,Set<String> worlds){
  var bar=bars.computeIfAbsent(event.id(),id->new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS));
  long seconds=Math.max(0,(event.expires()-now+999)/1000);
  String suffix=waiting&&now>=event.expires()?"Aguardando fim da batalha/captura":String.format(java.util.Locale.ROOT,"%02d:%02d restantes",seconds/60,seconds%60);
  bar.setName(Component.literal((event.test()?"[TESTE] ":"")+"✦ "+name+" • "+suffix).withStyle(style->style.withBold(true).withColor(0x67E8F9)));
  bar.setProgress(LegendaryRules.eventProgress(event.created(),event.expires(),now));
  for(var p:List.copyOf(bar.getPlayers()))if(p.hasDisconnected()||p.isSpectator()||!worlds.contains(p.level().dimension().location().toString())||!AuthModule.getAuthService().isAuthenticated(p.getUUID()))bar.removePlayer(p);
  for(var p:server.getPlayerList().getPlayers())if(!p.hasDisconnected()&&!p.isSpectator()&&worlds.contains(p.level().dimension().location().toString())&&AuthModule.getAuthService().isAuthenticated(p.getUUID()))bar.addPlayer(p);
 }
 public void remove(UUID id){var bar=bars.remove(id);if(bar!=null)bar.removeAllPlayers();}
 public void clear(){for(var bar:bars.values())bar.removeAllPlayers();bars.clear();}
}
