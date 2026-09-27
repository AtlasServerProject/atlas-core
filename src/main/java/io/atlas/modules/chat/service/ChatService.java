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

    public void broadcast(ServerPlayer sender, PlayerChatMessage message) {
        var formattedMessage = formatter.format(
                sender.getName().getString(),
                rankService.getHighestRank(sender.getUUID()),
                message.decoratedContent(),
                canUseColoredChat(sender)
        );

        sender.getServer()
                .getPlayerList()
                .broadcastSystemMessage(formattedMessage, false);
    }

    private boolean canUseColoredChat(ServerPlayer sender) {
        return rankService.hasPermission(sender.getUUID(), COLORED_CHAT_PERMISSION)
                || rankService.getPlayerRanks(sender.getUUID()).stream()
                .map(Rank::getIdentifier)
                .map(identifier -> identifier.toUpperCase(Locale.ROOT))
                .anyMatch(COLORED_CHAT_RANKS::contains);
    }
}
