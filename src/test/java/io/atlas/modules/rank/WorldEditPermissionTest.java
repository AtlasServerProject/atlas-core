package io.atlas.modules.rank;
import io.atlas.modules.rank.service.WorldEditPermissionService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class WorldEditPermissionTest {
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        AtomicBoolean auth = new AtomicBoolean(false), admin = new AtomicBoolean(true);
        AtomicLong time = new AtomicLong();
        AtomicInteger lookups = new AtomicInteger();
        var service = new WorldEditPermissionService(u -> auth.get(), u -> { lookups.incrementAndGet(); return admin.get(); }, time::get);
        expect(service.check(player, "minecraft.command.op"), WorldEditPermissionService.Decision.DEFAULT);
        expect(service.check(player, "worldeditx.wand"), WorldEditPermissionService.Decision.DEFAULT);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.DENY);
        require(lookups.get() == 0, "Unauthenticated identities must not load grants");
        auth.set(true);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.ALLOW);
        expect(service.check(player, "worldedit.clipboard.paste"), WorldEditPermissionService.Decision.ALLOW);
        require(lookups.get() == 1, "Checks should reuse role cache");
        auth.set(false);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.DENY);
        auth.set(true); admin.set(false);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.DENY);
        admin.set(true); time.addAndGet(2_000_000_001L);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.ALLOW);
        admin.set(false); time.addAndGet(2_000_000_001L);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.DENY);
        var failing = new WorldEditPermissionService(u -> true, u -> { throw new IllegalStateException("DB unavailable"); }, time::get);
        expect(failing.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.DENY);
        service.forget(player); admin.set(true);
        expect(service.check(player, "worldedit.wand"), WorldEditPermissionService.Decision.ALLOW);
        System.out.println("WorldEdit permissions: authentication, scope, revocation, cache, disconnect and fail-closed checks passed.");
    }
    private static void expect(Object actual, Object expected) { require(actual == expected, actual + " != " + expected); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
