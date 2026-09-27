package io.atlas.modules.chat.formatter;

import io.atlas.modules.rank.formatter.RankFormatter;
import io.atlas.modules.rank.model.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public class ChatFormatter {

    private final RankFormatter rankFormatter = new RankFormatter();

    public Component format(
            String username,
            Optional<Rank> highestRank,
            Component message,
            boolean allowColorCodes
    ) {
        MutableComponent result = Component.empty();

        highestRank
                .filter(rank -> rank.getPrefix() != null && !rank.getPrefix().isBlank())
                .ifPresent(rank -> result
                        .append(rankFormatter.formatPrefix(rank))
                        .append(" "));

        result.append(Component.literal(username).withStyle(ChatFormatting.WHITE));
        result.append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
        result.append(allowColorCodes
                ? translateColorCodes(message.getString())
                : message.copy().withStyle(ChatFormatting.GRAY));
        return result;
    }

    private Component translateColorCodes(String rawMessage) {
        MutableComponent result = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style currentStyle = Style.EMPTY.withColor(ChatFormatting.GRAY);

        for (int index = 0; index < rawMessage.length(); index++) {
            char current = rawMessage.charAt(index);
            if (current != '&' || index + 1 >= rawMessage.length()) {
                buffer.append(current);
                continue;
            }

            ChatFormatting formatting = formattingByCode(rawMessage.charAt(index + 1));
            if (formatting == null) {
                buffer.append(current);
                continue;
            }

            appendSegment(result, buffer, currentStyle);
            currentStyle = applyFormatting(currentStyle, formatting);
            index++;
        }

        appendSegment(result, buffer, currentStyle);
        return result;
    }

    private void appendSegment(MutableComponent result, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) {
            return;
        }

        result.append(Component.literal(buffer.toString()).withStyle(style));
        buffer.setLength(0);
    }

    private Style applyFormatting(Style currentStyle, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY.withColor(ChatFormatting.GRAY);
        }

        if (formatting.isColor()) {
            return Style.EMPTY.withColor(formatting);
        }

        return currentStyle.applyFormat(formatting);
    }

    private ChatFormatting formattingByCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'l' -> ChatFormatting.BOLD;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'o' -> ChatFormatting.ITALIC;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }
}
