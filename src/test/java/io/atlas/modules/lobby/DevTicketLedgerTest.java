package io.atlas.modules.lobby;

import io.atlas.modules.lobby.service.DevTicketLedger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

public final class DevTicketLedgerTest {
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }

    public static void main(String[] args) {
        TestClock clock = new TestClock();
        DevTicketLedger ledger = new DevTicketLedger(clock);
        UUID id = UUID.randomUUID();
        var identity = new DevTicketLedger.Identity(id, "Tester", "admin");
        String token = ledger.issue(identity);
        check(token.length() == 43, "token carries 256 random bits");
        check(ledger.consume(token, "Tester", uuid -> true).orElseThrow().equals(identity), "canonical identity retained");
        check(ledger.consume(token, "Tester", uuid -> true).isEmpty(), "replay rejected");
        check(ledger.consume(ledger.issue(identity), "Impostor", uuid -> true).isEmpty(), "name mismatch rejected");
        check(ledger.consume(ledger.issue(identity), "Tester", uuid -> false).isEmpty(), "revoked rank rejected");
        String old = ledger.issue(identity);
        String current = ledger.issue(identity);
        check(ledger.consume(old, "Tester", uuid -> true).isEmpty(), "new transfer invalidates old ticket");
        clock.now = clock.now.plusSeconds(30);
        check(ledger.consume(current, "Tester", uuid -> true).isEmpty(), "exact expiry rejected");
        check(ledger.consume("forged", "Tester", uuid -> true).isEmpty(), "unknown ticket rejected");
        String concurrent = ledger.issue(identity);
        var first = java.util.concurrent.CompletableFuture.supplyAsync(() -> ledger.consume(concurrent, "Tester", uuid -> true).isPresent());
        var second = java.util.concurrent.CompletableFuture.supplyAsync(() -> ledger.consume(concurrent, "Tester", uuid -> true).isPresent());
        check(first.join() != second.join(), "only one concurrent claim succeeds");
        System.out.println("PASS: dev identity, replay, name mismatch, rank revocation, replacement, expiry and concurrent claim.");
    }
}
