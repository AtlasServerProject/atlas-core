package io.atlas.modules.chat.listener;

import io.atlas.modules.chat.service.ChatService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class ChatListener {

    public static void register(ChatService chatService) {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            chatService.broadcast(sender, message);
            return false;
        });
    }
}
