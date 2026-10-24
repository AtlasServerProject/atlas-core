package io.atlas.modules.chat;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.chat.listener.ChatListener;
import io.atlas.modules.chat.service.ChatService;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.moderation.ModerationModule;

public class ChatModule implements AtlasModule {

    private static final ChatService SERVICE=new ChatService(RankModule.getRankService());
    public static ChatService service(){return SERVICE;}

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public void enable() {
        ChatListener.register(
                SERVICE,
                AuthModule.getProtectionService(),
                ModerationModule.getService()
        );
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->SERVICE.remove(handler.player.getUUID()));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->
            io.atlas.modules.audit.service.ActivityLogService.record("JOIN",handler.player.getName().getString(),handler.player.getUUID().toString(),"connection","Entrou no Minecraft"));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->
            io.atlas.modules.audit.service.ActivityLogService.record("LEAVE",handler.player.getName().getString(),handler.player.getUUID().toString(),"connection","Saiu do Minecraft"));
        AtlasMod.LOGGER.info("Chat Formatter iniciado.");
    }

    @Override
    public void disable() {
    }
}
