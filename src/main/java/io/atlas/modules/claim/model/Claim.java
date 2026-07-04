package io.atlas.modules.claim.model;

import java.util.UUID;

public record Claim(long id, UUID ownerUuid, String ownerName, String world,
                    int minX, int minZ, int maxX, int maxZ) {
    public int area() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
