package io.atlas.modules.moderation.model;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public record ModerationDuration(Duration duration, boolean permanent) {

    public static Optional<ModerationDuration> parse(String input) {
        String value = input.toLowerCase(Locale.ROOT).trim();
        if (value.equals("perma") || value.equals("perm") || value.equals("permanente")) {
            return Optional.of(new ModerationDuration(null, true));
        }
        if (value.length() < 2) {
            return Optional.empty();
        }

        char unit = value.charAt(value.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(value.substring(0, value.length() - 1));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        if (amount <= 0) {
            return Optional.empty();
        }

        return switch (unit) {
            case 's' -> Optional.of(new ModerationDuration(Duration.ofSeconds(amount), false));
            case 'm' -> Optional.of(new ModerationDuration(Duration.ofMinutes(amount), false));
            case 'h' -> Optional.of(new ModerationDuration(Duration.ofHours(amount), false));
            case 'd' -> Optional.of(new ModerationDuration(Duration.ofDays(amount), false));
            default -> Optional.empty();
        };
    }
}
