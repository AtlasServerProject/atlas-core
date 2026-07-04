package io.atlas.modules.performance.service;

import net.minecraft.server.MinecraftServer;

public final class TpsMonitorService {
    private static final int WINDOW = 200;
    private final long[] samples = new long[WINDOW];
    private int cursor;
    private int size;
    private long tickStarted;

    public void startTick(MinecraftServer server) {
        tickStarted = System.nanoTime();
    }

    public void endTick(MinecraftServer server) {
        if (tickStarted == 0L) return;
        samples[cursor] = System.nanoTime() - tickStarted;
        cursor = (cursor + 1) % WINDOW;
        if (size < WINDOW) size++;
    }

    public double mspt() {
        if (size == 0) return 0.0;
        long total = 0L;
        for (int i = 0; i < size; i++) total += samples[i];
        return total / (double) size / 1_000_000.0;
    }

    public double tps() {
        double mspt = mspt();
        return mspt <= 0.0 ? 20.0 : Math.min(20.0, 1_000.0 / mspt);
    }
}
