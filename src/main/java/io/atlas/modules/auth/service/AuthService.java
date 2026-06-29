package io.atlas.modules.auth.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.cache.AuthSessionCache;
import io.atlas.modules.auth.model.AuthAccount;
import io.atlas.modules.auth.model.AuthSession;
import io.atlas.modules.auth.model.AuthSessionState;
import io.atlas.modules.auth.repository.AuthRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

    private static final Duration SESSION_DURATION = Duration.ofHours(12);

    private final AuthRepository repository;
    private final AuthSessionCache cache;

    public AuthService() {
        this(new AuthRepository(), new AuthSessionCache());
    }

    AuthService(AuthRepository repository, AuthSessionCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public void loadPlayer(UUID playerUuid, String ipAddress) {
        Optional<AuthAccount> account = repository.findAccountByPlayerUuid(playerUuid);
        AuthSession session = repository.createSession(playerUuid, ipAddress);

        cache.put(new AuthSessionState(
                session,
                account.isPresent(),
                account.map(AuthAccount::premium).orElse(false)
        ));

        AtlasMod.LOGGER.info("Sessão de autenticação criada para {}.", playerUuid);
    }

    public void unloadPlayer(UUID playerUuid) {
        cache.get(playerUuid).ifPresent(state ->
                repository.closeSession(state.session().id())
        );
        cache.remove(playerUuid);
    }

    public RegistrationResult registerAccount(
            UUID playerUuid,
            String passwordHash,
            String ipAddress
    ) {
        Optional<AuthSessionState> currentState = cache.get(playerUuid);
        if (currentState.isEmpty()) {
            return RegistrationResult.SESSION_NOT_FOUND;
        }

        if (currentState.get().registered()) {
            return RegistrationResult.ALREADY_REGISTERED;
        }

        if (!repository.createAccount(playerUuid, passwordHash, ipAddress)) {
            return RegistrationResult.ALREADY_REGISTERED;
        }

        cache.put(new AuthSessionState(
                currentState.get().session(),
                true,
                false
        ));
        authenticate(playerUuid, ipAddress);
        return RegistrationResult.SUCCESS;
    }

    public boolean authenticate(UUID playerUuid, String ipAddress) {
        Optional<AuthSessionState> currentState = cache.get(playerUuid);
        if (currentState.isEmpty() || !currentState.get().registered()) {
            return false;
        }

        LocalDateTime expiresAt = LocalDateTime.now().plus(SESSION_DURATION);
        AuthSession currentSession = currentState.get().session();
        repository.authenticateSession(currentSession.id(), expiresAt);
        repository.updateLastLogin(playerUuid, ipAddress);

        AuthSession authenticatedSession = new AuthSession(
                currentSession.id(),
                currentSession.playerUuid(),
                currentSession.ipAddress(),
                true,
                currentSession.loginAt(),
                expiresAt
        );
        cache.put(new AuthSessionState(
                authenticatedSession,
                true,
                currentState.get().premium()
        ));
        return true;
    }

    public Optional<AuthSessionState> getSession(UUID playerUuid) {
        return cache.get(playerUuid);
    }

    public boolean isLoaded(UUID playerUuid) {
        return cache.contains(playerUuid);
    }

    public boolean isRegistered(UUID playerUuid) {
        return cache.get(playerUuid)
                .map(AuthSessionState::registered)
                .orElse(false);
    }

    public boolean isAuthenticated(UUID playerUuid) {
        return cache.get(playerUuid)
                .map(AuthSessionState::authenticated)
                .orElse(false);
    }

    public void shutdown() {
        cache.getAll().forEach(state -> repository.closeSession(state.session().id()));
        cache.clear();
    }

    public enum RegistrationResult {
        SUCCESS,
        ALREADY_REGISTERED,
        SESSION_NOT_FOUND
    }
}
