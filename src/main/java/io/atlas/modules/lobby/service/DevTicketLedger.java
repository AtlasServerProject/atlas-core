package io.atlas.modules.lobby.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/** Autorizações de uso único, sem contas, senhas ou dados de jogo. */
public final class DevTicketLedger {
    public record Identity(UUID uuid, String username, String role) {}
    private record Ticket(Identity identity, Instant expires) {}
    private final Map<String, Ticket> tickets = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public DevTicketLedger(Clock clock) { this.clock = clock; }

    public synchronized String issue(Identity identity) {
        tickets.entrySet().removeIf(entry -> !entry.getValue().expires().isAfter(clock.instant()));
        tickets.entrySet().removeIf(entry -> entry.getValue().identity().uuid().equals(identity.uuid()));
        if (tickets.size() >= 256) throw new IllegalStateException("Muitas transferências pendentes.");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(token, new Ticket(identity, clock.instant().plusSeconds(30)));
        return token;
    }

    public synchronized Optional<Identity> consume(String token, String username, Predicate<UUID> authorized) {
        Ticket ticket = tickets.remove(token);
        if (ticket == null || !ticket.expires().isAfter(clock.instant())
                || !ticket.identity().username().equals(username)
                || !authorized.test(ticket.identity().uuid())) return Optional.empty();
        return Optional.of(ticket.identity());
    }
}
