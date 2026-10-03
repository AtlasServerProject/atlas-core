package io.atlas.modules.vip.service;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import io.atlas.modules.rank.RankModule;
public final class VipRuntimeService {
 private int ticks;
 public void tick(MinecraftServer server){
  if(++ticks<20)return;ticks=0;
  for(var player:server.getPlayerList().getPlayers()){
   var mode=player.gameMode.getGameModeForPlayer();if(mode==GameType.CREATIVE||mode==GameType.SPECTATOR)continue;
   if(player.getAbilities().mayfly&&!RankModule.getRankService().canFly(player.getUUID())){player.getAbilities().mayfly=false;player.getAbilities().flying=false;player.onUpdateAbilities();}
  }
 }
}
