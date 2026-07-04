package io.atlas.modules.claim.model;

public enum TrustLevel {
    ACCESS,
    CONTAINER,
    BUILD;

    public boolean allows(TrustLevel required) {
        return ordinal() >= required.ordinal();
    }
}
