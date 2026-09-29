package io.atlas.modules.moderation.model;

import java.time.Instant;

/** One immutable event in the player's staff timeline. */
public record StaffRecord(long id, String type, String actor, String text, Instant at, String status) {}
