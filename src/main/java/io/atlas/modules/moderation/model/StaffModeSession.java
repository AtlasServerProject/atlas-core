package io.atlas.modules.moderation.model;

import java.util.UUID;

public record StaffModeSession(UUID uuid, String gameMode, String world,
        double x, double y, double z, float yaw, float pitch,
        boolean mayFly, boolean flying, boolean invulnerable, boolean instantBuild, boolean mayBuild,
        float flySpeed, float walkSpeed) {}
