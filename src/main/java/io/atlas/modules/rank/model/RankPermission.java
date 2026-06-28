package io.atlas.modules.rank.model;

public record RankPermission(
        String permission,
        boolean enabled
) {}