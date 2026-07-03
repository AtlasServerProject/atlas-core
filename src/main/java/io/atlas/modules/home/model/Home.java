package io.atlas.modules.home.model;

public record Home(
        long id,
        String name,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean primary
) {
}
