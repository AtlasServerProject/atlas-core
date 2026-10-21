package io.atlas.modules.chat.service;

import io.atlas.modules.chat.formatter.ChatFormatter;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;

public class ChatService {

    private static final String COLORED_CHAT_PERMISSION = "atlas.chat.color";
    private static final Set<String> COLORED_CHAT_RANKS = Set.of(
            "VIP",
            "VIPPLUS",
            "VIPPLUSPLUS",
            "SUP",
            "MOD",
            "ADMIN",
            "ADM",
            "OWNER",
            "DONO"
    );

    private final RankService rankService;
    private final ChatFormatter formatter;

    public ChatService(RankService rankService) {
        this.rankService = rankService;
        this.formatter = new ChatFormatter();
    }

    private final java.util.Map<java.util.UUID,String> channels = new java.util.HashMap<>();
    private boolean staffAccess(ServerPlayer player){return io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(player.getUUID())&&rankService.isStaff(player.getUUID());}
    private boolean vipAccess(ServerPlayer player){return io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(player.getUUID())&&(io.atlas.modules.vipcommands.service.VipCommandsService.canUse(player,1)||rankService.isStaff(player.getUUID()));}
    public void remove(java.util.UUID id){channels.remove(id);}
    private boolean permitted(ServerPlayer player){
        var auth=io.atlas.modules.auth.AuthModule.getProtectionService();
        if(!auth.canChat(player.getUUID())){player.sendSystemMessage(auth.loginRequiredMessage());return false;}
        var moderation=io.atlas.modules.moderation.ModerationModule.getService();
        var mute=moderation.activeMute(player.getUUID());
        if(mute.isPresent()){player.sendSystemMessage(moderation.mutedMessage(mute.get()));return false;}
        return true;
    }
    public void broadcast(ServerPlayer sender,PlayerChatMessage message){send(sender,channels.getOrDefault(sender.getUUID(),"global"),message.decoratedContent().getString());}
    public int select(ServerPlayer sender,String channel){
        if(!permitted(sender))return 0;
        if(channel.equals("vip")&&!vipAccess(sender)){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cEste canal exige VIP ativo ou cargo staff autorizado."));return 0;}
        if(channel.equals("staff")&&!staffAccess(sender)){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCanal exclusivo da staff."));return 0;}
        channels.put(sender.getUUID(),channel);sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aCanal selecionado: "+(channel.equals("local")?"Local (100 blocos)":channel.equals("vip")?"VIP":channel.equals("staff")?"Staff":"Global")));return 1;
    }
    private net.minecraft.network.chat.Component formatted(ServerPlayer sender,String channel,String text){
        var preferences=io.atlas.modules.vipcommands.service.VipCommandsService.preferences(sender);
        if(preferences.color()!=null)text="&"+io.atlas.modules.vipcommands.service.VipCommandsService.color(preferences.color()).getChar()+text;
        var typeTitle=io.atlas.modules.pokedex.PokedexModule.types().title(sender.getUUID());
        var professor=io.atlas.modules.pokedex.PokedexModule.service().professor(sender.getUUID());
        return net.minecraft.network.chat.Component.literal((typeTitle==null?"":"["+typeTitle+"] ")+(professor?"[Professor Atlas] ":io.atlas.modules.pokedex.PokedexModule.service().researcher(sender.getUUID())?"[Pesquisador Atlas] ":"")).withStyle(net.minecraft.ChatFormatting.GOLD).append(net.minecraft.network.chat.Component.literal("["+channel+"] ").withStyle(channel.equals("Staff")?net.minecraft.ChatFormatting.RED:channel.equals("VIP")?net.minecraft.ChatFormatting.LIGHT_PURPLE:channel.equals("Feira")?net.minecraft.ChatFormatting.DARK_BLUE:channel.equals("G")?net.minecraft.ChatFormatting.GRAY:net.minecraft.ChatFormatting.YELLOW)
            .append(formatter.format(preferences.nickname()==null?sender.getName().getString():"~"+preferences.nickname(),rankService.getHighestRank(sender.getUUID()),net.minecraft.network.chat.Component.literal(text),canUseColoredChat(sender))));
    }
    public int send(ServerPlayer sender,String channel,String text){
        if(!permitted(sender))return 0;
        if(channel.equals("vip")&&!vipAccess(sender)){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cEste canal exige VIP ativo ou cargo staff autorizado."));return 0;}
        if(channel.equals("staff")&&!staffAccess(sender)){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCanal exclusivo da staff."));return 0;}
        if(text.isBlank()||text.length()>256){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cEnvie de 1 a 256 caracteres."));return 0;}
        var message=formatted(sender,channel.equals("local")?"Local":channel.equals("feira")?"Feira":channel.equals("vip")?"VIP":channel.equals("staff")?"Staff":"G",text);
        for(var recipient:sender.getServer().getPlayerList().getPlayers()){
            if(!io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(recipient.getUUID()))continue;
            if(channel.equals("local")&&(recipient.serverLevel()!=sender.serverLevel()||recipient.distanceToSqr(sender)>10000))continue;
            if(channel.equals("vip")&&!vipAccess(recipient))continue;
            if(channel.equals("staff")&&!staffAccess(recipient))continue;
            recipient.sendSystemMessage(message);
        }
        return 1;
    }
    public int tell(ServerPlayer sender,ServerPlayer target,String text){
        if(!permitted(sender))return 0;
        if(!io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(target.getUUID())){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cEsse jogador ainda não está autenticado."));return 0;}
        if(text.isBlank()||text.length()>256){sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cEnvie de 1 a 256 caracteres."));return 0;}
        var body=formatted(sender,"Tell → "+target.getName().getString(),text);
        sender.sendSystemMessage(body);if(target!=sender)target.sendSystemMessage(body);return 1;
    }

    private boolean canUseColoredChat(ServerPlayer sender) {
        return rankService.hasPermission(sender.getUUID(), COLORED_CHAT_PERMISSION)
                || rankService.getPlayerRanks(sender.getUUID()).stream()
                .map(Rank::getIdentifier)
                .map(identifier -> identifier.toUpperCase(Locale.ROOT))
                .anyMatch(COLORED_CHAT_RANKS::contains);
    }
}
