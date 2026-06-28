package io.atlas.modules.chat;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.chat.listener.ChatListener;
import io.atlas.modules.chat.service.ChatService;
import io.atlas.modules.rank.RankModule;

public class ChatModule implements AtlasModule {

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public void enable() {
        ChatListener.register(new ChatService(RankModule.getRankService()));
        AtlasMod.LOGGER.info("Chat Formatter iniciado.");
    }

    @Override
    public void disable() {
    }
}
