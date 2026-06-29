package io.atlas.modules.auth.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.cache.AuthSessionCache;
import io.atlas.modules.auth.model.AuthAccount;
import io.atlas.modules.auth.model.AuthSession;
import io.atlas.modules.auth.model.AuthSessionState;
import io.atlas.modules.auth.repository.AuthRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

    private static final Duration SESSION_DURATION = Duration.ofHours(12);
    private static final int BCRYPT_COST = 12;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;

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

    public RegistrationResult register(
            UUID playerUuid,
            String password,
            String confirmation,
            String ipAddress
    ) {
        Optional<AuthSessionState> currentState = cache.get(playerUuid);
        if (currentState.isEmpty()) {
            return RegistrationResult.SESSION_NOT_FOUND;
        }

        if (currentState.get().registered()) {
            return RegistrationResult.ALREADY_REGISTERED;
        }

        if (!password.equals(confirmation)) {
            return RegistrationResult.PASSWORD_MISMATCH;
        }

        if (!isValidPassword(password)) {
            return RegistrationResult.INVALID_PASSWORD;
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
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

    public LoginResult login(UUID playerUuid, String password, String ipAddress) {
        Optional<AuthSessionState> currentState = cache.get(playerUuid);
        if (currentState.isEmpty()) {
            return LoginResult.SESSION_NOT_FOUND;
        }

        if (!currentState.get().registered()) {
            return LoginResult.NOT_REGISTERED;
        }

        if (currentState.get().authenticated()) {
            return LoginResult.ALREADY_AUTHENTICATED;
        }

        Optional<AuthAccount> account = repository.findAccountByPlayerUuid(playerUuid);
        if (account.isEmpty() || !matches(password, account.get().passwordHash())) {
            return LoginResult.INVALID_PASSWORD;
        }

        return authenticate(playerUuid, ipAddress)
                ? LoginResult.SUCCESS
                : LoginResult.SESSION_NOT_FOUND;
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

    private boolean isValidPassword(String password) {
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        return password.length() >= MIN_PASSWORD_LENGTH
                && byteLength <= MAX_PASSWORD_BYTES;
    }

    private boolean matches(String password, String passwordHash) {
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            AtlasMod.LOGGER.error("Hash de autenticação inválido para uma conta.");
            return false;
        }
    }

    public enum RegistrationResult {
        SUCCESS,
        ALREADY_REGISTERED,
        PASSWORD_MISMATCH,
        INVALID_PASSWORD,
        SESSION_NOT_FOUND
    }

    public enum LoginResult {
        SUCCESS,
        NOT_REGISTERED,
        ALREADY_AUTHENTICATED,
        INVALID_PASSWORD,
        SESSION_NOT_FOUND
    }
}
