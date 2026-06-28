package io.atlas.modules.chat.service;

import io.atlas.modules.chat.formatter.ChatFormatter;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

public class ChatService {

    private final RankService rankService;
    private final ChatFormatter formatter;

    public ChatService(RankService rankService) {
        this.rankService = rankService;
        this.formatter = new ChatFormatter();
    }

    public void broadcast(ServerPlayer sender, PlayerChatMessage message) {
        var formattedMessage = formatter.format(
                sender.getName().getString(),
                rankService.getHighestRank(sender.getUUID()),
                message.decoratedContent()
        );

        sender.getServer()
                .getPlayerList()
                .broadcastSystemMessage(formattedMessage, false);
    }
}
