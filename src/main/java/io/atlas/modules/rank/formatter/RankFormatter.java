package io.atlas.modules.rank.formatter;

import io.atlas.modules.rank.model.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public class RankFormatter {

    private static final TextColor DEFAULT_COLOR =
            TextColor.fromLegacyFormat(ChatFormatting.WHITE);

    public Component formatPrefix(Rank rank) {
        if (rank.getPrefix() == null || rank.getPrefix().isBlank()) {
            return Component.empty();
        }

        return Component.literal(rank.getPrefix().trim())
                .withColor(resolveColor(rank.getColor()).getValue());
    }

    public Component formatPlayerName(Rank rank, String username) {
        MutableComponent result = Component.empty();

        if (rank.getPrefix() != null && !rank.getPrefix().isBlank()) {
            result.append(formatPrefix(rank)).append(" ");
        }

        return result.append(
                Component.literal(username)
                        .withColor(resolveColor(rank.getColor()).getValue())
        );
    }

    public Component formatPlayerName(String username) {
        return Component.literal(username).withStyle(ChatFormatting.WHITE);
    }

    public ChatFormatting resolveTeamColor(Rank rank) {
        if (rank.getColor() == null || rank.getColor().isBlank()) {
            return ChatFormatting.WHITE;
        }

        ChatFormatting color = ChatFormatting.getByName(rank.getColor().trim());
        return color != null && color.isColor() ? color : ChatFormatting.WHITE;
    }

    private TextColor resolveColor(String color) {
        if (color == null || color.isBlank()) {
            return DEFAULT_COLOR;
        }

        return TextColor.parseColor(color.trim())
                .result()
                .orElse(DEFAULT_COLOR);
    }
}
