package io.atlas.modules.legendary.service;

import io.atlas.modules.legendary.config.LegendaryConfig;
import io.atlas.modules.legendary.model.LegendaryRules;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.*;
import com.google.gson.JsonObject;

public final class LegendaryAnnouncementService {
    public static final Stats[] STATS={Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED};
    private static final String[] LABELS={"HP","Attack","Defense","Sp. Attack","Sp. Defense","Speed"};
    public static JsonObject captureData(Pokemon pokemon,ServerPlayer player){
        JsonObject data=new JsonObject();data.addProperty("trainerUuid",player.getUUID().toString());data.addProperty("trainer",player.getGameProfile().getName());
        int total=0;for(int i=0;i<6;i++){int iv=pokemon.getIvs().get(STATS[i]);data.addProperty(LABELS[i],iv);total+=iv;}
        data.addProperty("total",total);data.addProperty("percentage",LegendaryRules.percentage(total));data.addProperty("quality",LegendaryRules.quality(total));
        data.addProperty("nature",pokemon.getNature().getName().getPath());data.addProperty("ability",pokemon.getAbility().getName());data.addProperty("shiny",pokemon.getShiny());return data;
    }
    private Component colored(String text,String hex){return Component.literal(text).withStyle(s->s.withColor(TextColor.parseColor(hex).result().orElse(TextColor.fromRgb(0x55ffff))));}
    private void send(MinecraftServer server,String text,String color){server.getPlayerList().broadcastSystemMessage(colored(text,color),false);}
    public void warning(MinecraftServer server,int minutes){
        send(server,"═══════ ✦ ═══════", "#ffaa00");send(server,"ATLAS COBBLEMON", "#55ffff");
        send(server,"Uma energia misteriosa começa a se espalhar pelo mundo…", "#aa55ff");
        send(server,"Uma tentativa de aparição lendária acontecerá em "+minutes+" minutos!", "#ffaa00");send(server,"Prepare-se para explorar!", "#55ff55");
    }
    public void spawn(MinecraftServer server,LegendaryConfig c,LegendaryConfig.Spawn e,Pokemon pokemon,String biome,boolean test){
        if(!c.main.announceSpawn)return;var m=c.messages.get(e.announcement);
        var players=server.getPlayerList();
        players.broadcastSystemMessage(colored("━━━━━━ ✦ ATLAS • LENDÁRIOS ✦ ━━━━━━","#F6C85F").copy().withStyle(style->style.withBold(true)),false);
        players.broadcastSystemMessage(colored((test?"[TESTE] ":"")+m.symbol+" "+m.title,"#67E8F9").copy().withStyle(style->style.withBold(true)),false);
        players.broadcastSystemMessage(colored(m.story,"#CBD5E1").copy().withStyle(style->style.withItalic(true)),false);
        players.broadcastSystemMessage(colored(e.displayName.toUpperCase(java.util.Locale.ROOT),m.primary).copy().withStyle(style->style.withBold(true)).append(colored(" apareceu no mundo!","#E2E8F0")),false);
        if(pokemon.getShiny()&&c.main.enableShinyAnnouncements)players.broadcastSystemMessage(colored("✧ Uma aparição SHINY extraordinária! ✧","#F6C85F").copy().withStyle(style->style.withBold(true)),false);
        players.broadcastSystemMessage(colored("  ◆ Bioma: ","#94A3B8").copy().append(Component.translatable("biome."+biome.replace(':','.')).withStyle(style->style.withColor(0x67E8F9).withBold(true))),false);
        players.broadcastSystemMessage(colored("━━━━━━━━━━━━━━━━━━━━━━━━━━━━","#64748B"),false);
        extras(server,c,e,m);
    }
    public void outcome(MinecraftServer server,LegendaryConfig c,LegendaryConfig.Spawn e,String status,boolean test){
        if(!c.main.announceSpawn)return;
        String title,text,color;
        switch(status){
            case "DEFEATED"->{title="UMA LENDA FOI DERROTADA";text=" foi derrotado. Sua presença lendária deixa uma marca no Atlas.";color="#FB7185";}
            case "EXPIRED"->{title="O TEMPO DA LENDA CHEGOU AO FIM";text=" partiu após o tempo de aparição se esgotar.";color="#F6C85F";}
            case "DESPAWNED","LOST"->{title="UMA PRESENÇA LENDÁRIA DESAPARECEU";text=" já não está presente no mundo. Sua energia se dissipou.";color="#A78BFA";}
            default->{return;}
        }
        var players=server.getPlayerList();
        players.broadcastSystemMessage(colored("━━━━━━ ✦ ATLAS • LENDÁRIOS ✦ ━━━━━━","#F6C85F").copy().withStyle(style->style.withBold(true)),false);
        players.broadcastSystemMessage(colored((test?"[TESTE] ":"")+title,color).copy().withStyle(style->style.withBold(true)),false);
        players.broadcastSystemMessage(colored(e.displayName,"#67E8F9").copy().withStyle(style->style.withBold(true)).append(colored(text,"#CBD5E1")),false);
        players.broadcastSystemMessage(colored("━━━━━━━━━━━━━━━━━━━━━━━━━━━━","#64748B"),false);
    }
    public void capture(MinecraftServer server,LegendaryConfig c,LegendaryConfig.Spawn e,JsonObject data,boolean test){
        if(!c.main.announceCapture)return;var m=c.messages.get(e.announcement);
        send(server,"═══════ "+m.symbol+" ═══════",m.primary);send(server,(test?"[TESTE] ":"")+m.captureTitle,m.primary);send(server,m.captureText,m.secondary);
        send(server,"Treinador: "+data.get("trainer").getAsString(),m.secondary);
        for(String stat:LABELS)send(server,stat+": "+data.get(stat).getAsInt()+" IVs",m.primary);
        send(server,"IVs Totais: "+data.get("total").getAsInt()+"/186 — "+data.get("percentage").getAsString()+"%",m.secondary);
        int total=data.get("total").getAsInt();
        String qualityColor=total==186||total*100.0/186>=95?"#ffaa00":total*100.0/186>=85?"#aa00aa":total*100.0/186>=70?"#5555ff":total*100.0/186>=50?"#55ff55":"#aaaaaa";
        send(server,"Qualidade: "+data.get("quality").getAsString(),qualityColor);
        send(server,"Nature: "+data.get("nature").getAsString()+" | Ability: "+data.get("ability").getAsString(),m.primary);
        if(data.get("shiny").getAsBoolean()&&c.main.enableShinyAnnouncements)send(server,"✧ CAPTURA LENDÁRIA SHINY! ✧",m.secondary);
        if(data.get("total").getAsInt()==186)send(server,"✦ UM LENDÁRIO COM IVs MÁXIMOS! ✦", "#ffaa00");
        extras(server,c,e,m);
    }
    private void extras(MinecraftServer server,LegendaryConfig c,LegendaryConfig.Spawn e,LegendaryConfig.Message m){
        for(ServerPlayer player:server.getPlayerList().getPlayers()){
            if(com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player)!=null)continue;
            var sound=c.sounds.getOrDefault(e.species,c.sounds.get("default"));
            if(c.main.enableSoundEffects&&sound!=null&&sound.enabled){var event=BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(sound.id));
                player.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(event),SoundSource.valueOf(sound.category),player.getX(),player.getY(),player.getZ(),sound.volume,sound.pitch,server.overworld().random.nextLong()));}
            if(c.main.enableTitles){player.connection.send(new ClientboundSetTitlesAnimationPacket(10,60,20));player.connection.send(new ClientboundSetTitleTextPacket(colored(e.displayName,m.primary).copy().withStyle(style->style.withBold(true))));player.connection.send(new ClientboundSetSubtitleTextPacket(colored(m.title,m.secondary)));}
            if(c.main.enableActionBar)player.displayClientMessage(colored(e.displayName+" • Uma lenda no Atlas",m.primary),true);
        }
    }
}
