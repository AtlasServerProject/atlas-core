package io.atlas.modules.auth.repository;

import io.atlas.modules.auth.model.AuthAccount;
import io.atlas.modules.auth.model.AuthSession;
import io.atlas.modules.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthRepository {

    public Optional<AuthAccount> findAccountByPlayerUuid(UUID playerUuid) {
        String sql = """
                SELECT a.id, a.player_id, p.uuid, a.password_hash, a.last_ip,
                       a.is_premium, a.is_verified, a.registered_at, a.last_login
                FROM auth_accounts a
                INNER JOIN players p ON p.id = a.player_id
                WHERE p.uuid = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, playerUuid);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapAccount(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar conta de autenticação.", exception);
        }
    }

    public boolean createAccount(UUID playerUuid, String passwordHash, String ipAddress) {
        String sql = """
                INSERT INTO auth_accounts (player_id, password_hash, last_ip)
                SELECT id, ?, ?
                FROM players
                WHERE uuid = ?
                ON CONFLICT (player_id) DO NOTHING
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, ipAddress);
            statement.setObject(3, playerUuid);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao criar conta de autenticação.", exception);
        }
    }

    public AuthSession createSession(UUID playerUuid, String ipAddress) {
        String sql = """
                INSERT INTO player_sessions (player_id, ip_address, logged_in)
                SELECT id, ?, false
                FROM players
                WHERE uuid = ?
                RETURNING id, login_at
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ipAddress);
            statement.setObject(2, playerUuid);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Jogador não encontrado ao criar sessão.");
                }

                return new AuthSession(
                        result.getLong("id"),
                        playerUuid,
                        ipAddress,
                        false,
                        toLocalDateTime(result.getTimestamp("login_at")),
                        null
                );
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao criar sessão de autenticação.", exception);
        }
    }

    public void authenticateSession(long sessionId, LocalDateTime expiresAt) {
        String sql = """
                UPDATE player_sessions
                SET logged_in = true, expires_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(expiresAt));
            statement.setLong(2, sessionId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao autenticar sessão.", exception);
        }
    }

    public void closeSession(long sessionId) {
        String sql = """
                UPDATE player_sessions
                SET logged_in = false, expires_at = NOW()
                WHERE id = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao encerrar sessão.", exception);
        }
    }

    public void updateLastLogin(UUID playerUuid, String ipAddress) {
        String sql = """
                UPDATE auth_accounts a
                SET last_login = NOW(), last_ip = ?
                FROM players p
                WHERE a.player_id = p.id
                AND p.uuid = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ipAddress);
            statement.setObject(2, playerUuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao atualizar login da conta.", exception);
        }
    }

    private AuthAccount mapAccount(ResultSet result) throws SQLException {
        return new AuthAccount(
                result.getLong("id"),
                result.getLong("player_id"),
                (UUID) result.getObject("uuid"),
                result.getString("password_hash"),
                result.getString("last_ip"),
                result.getBoolean("is_premium"),
                result.getBoolean("is_verified"),
                toLocalDateTime(result.getTimestamp("registered_at")),
                toLocalDateTime(result.getTimestamp("last_login"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
