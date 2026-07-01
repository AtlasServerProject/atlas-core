package io.atlas.modules.chat.listener;

import io.atlas.modules.chat.service.ChatService;
import io.atlas.modules.auth.service.AuthProtectionService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class ChatListener {

    public static void register(
            ChatService chatService,
            AuthProtectionService authProtection
    ) {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!authProtection.canChat(sender.getUUID())) {
                sender.sendSystemMessage(authProtection.loginRequiredMessage());
                return false;
            }

            chatService.broadcast(sender, message);
            return false;
        });
    }
}
