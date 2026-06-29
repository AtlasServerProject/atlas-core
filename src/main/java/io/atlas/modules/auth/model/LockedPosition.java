package io.atlas.modules.auth.model;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record LockedPosition(
        ResourceKey<Level> world,
        double x,
        double y,
        double z
) {
}
