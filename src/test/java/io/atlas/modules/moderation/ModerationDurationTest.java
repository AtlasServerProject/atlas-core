package io.atlas.modules.moderation;

import io.atlas.modules.moderation.model.ModerationDuration;
import io.atlas.modules.moderation.service.StaffModePolicy;
import java.time.Duration;

public final class ModerationDurationTest {
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    public static void main(String[] args) {
        check(ModerationDuration.parseTemporary("12h").orElseThrow().duration().equals(Duration.ofHours(12)), "12h");
        check(ModerationDuration.parseTemporary("1d").orElseThrow().duration().equals(Duration.ofDays(1)), "1d");
        check(ModerationDuration.parseTemporary("3650d").isPresent(), "maximum accepted");
        check(ModerationDuration.parseTemporary("1s").isPresent(), "minimum accepted");
        for (String bad : new String[]{"perma", "perm", "permanente", "0s", "-1h", "12", "1x", "", "1h30m", "3651d", "9223372036854775807d", "999999999999999999999s"})
            check(ModerationDuration.parseTemporary(bad).isEmpty(), "reject " + bad);
        check(ModerationDuration.parse("perma").orElseThrow().permanent(), "permanent mute unchanged");
        System.out.println("ModerationDurationTest PASS: finite durations, bounds, overflow and permanent rejection");
    }
}
