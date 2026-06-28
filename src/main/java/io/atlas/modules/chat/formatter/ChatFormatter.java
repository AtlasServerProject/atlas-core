package io.atlas.modules.chat.formatter;

import io.atlas.modules.rank.formatter.RankFormatter;
import io.atlas.modules.rank.model.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

public class ChatFormatter {

    private final RankFormatter rankFormatter = new RankFormatter();

    public Component format(String username, Optional<Rank> highestRank, Component message) {
        MutableComponent result = Component.empty();

        highestRank
                .filter(rank -> rank.getPrefix() != null && !rank.getPrefix().isBlank())
                .ifPresent(rank -> result
                        .append(rankFormatter.formatPrefix(rank))
                        .append(" "));

        result.append(Component.literal(username).withStyle(ChatFormatting.WHITE));
        result.append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
        result.append(message.copy().withStyle(ChatFormatting.GRAY));
        return result;
    }
}
