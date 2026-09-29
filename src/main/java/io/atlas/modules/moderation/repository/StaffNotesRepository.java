package io.atlas.modules.moderation.repository;

import io.atlas.modules.moderation.model.StaffRecord;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class StaffNotesRepository {
    public static final int PAGE_SIZE = 5;
    private final Supplier<Connection> connection;
    public StaffNotesRepository(Supplier<Connection> connection) { this.connection = connection; }

    public long add(long target, UUID author, String name, String body) {
        try (var s = connection.get().prepareStatement("""
                INSERT INTO staff_notes(target_player_id, author_uuid, author_name, body)
                VALUES (?, ?, ?, ?) RETURNING id
                """)) {
            s.setLong(1, target); s.setObject(2, author); s.setString(3, name); s.setString(4, body);
            try (var r = s.executeQuery()) { r.next(); return r.getLong(1); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao salvar nota interna.", e); }
    }

    /** Atomic soft deletion: no caller can replace an earlier archive reason. */
    public boolean archive(long target, long id, UUID actor, String name, String reason) {
        try (var s = connection.get().prepareStatement("""
                UPDATE staff_notes SET archived_at=NOW(), archived_by_uuid=?, archived_by_name=?, archive_reason=?
                WHERE target_player_id=? AND id=? AND archived_at IS NULL
                """)) {
            s.setObject(1, actor); s.setString(2, name); s.setString(3, reason);
            s.setLong(4, target); s.setLong(5, id);
            return s.executeUpdate() == 1;
        } catch (SQLException e) { throw new IllegalStateException("Falha ao arquivar nota interna.", e); }
    }

    public List<StaffRecord> list(long target, int page, boolean history) {
        if (page < 1 || page > 1000000) throw new IllegalArgumentException("Página inválida.");
        String sql = history ? """
                SELECT * FROM (
                    SELECT id, 'NOTA' AS kind, author_name AS actor, body AS detail, created_at AS event_at,
                        CASE WHEN archived_at IS NULL THEN 'registrada' ELSE 'arquivada' END AS status
                    FROM staff_notes WHERE target_player_id=?
                    UNION ALL
                    SELECT id, 'NOTA_ARQUIVADA', archived_by_name, archive_reason, archived_at, 'arquivada'
                    FROM staff_notes WHERE target_player_id=? AND archived_at IS NOT NULL
                    UNION ALL
                    SELECT id, type, actor_name, reason, created_at,
                        CASE WHEN revoked_at IS NOT NULL THEN 'revogada'
                             WHEN type IN ('WARN', 'KICK') THEN 'registrada'
                             WHEN expires_at <= NOW() THEN 'expirada' ELSE 'ativa' END
                    FROM moderation_punishments WHERE target_player_id=?
                    UNION ALL
                    SELECT id, 'REVOGACAO_' || type, revoked_by_name, revoked_reason, revoked_at, 'revogada'
                    FROM moderation_punishments WHERE target_player_id=? AND revoked_at IS NOT NULL
                ) events ORDER BY event_at DESC, kind ASC, id DESC LIMIT ? OFFSET ?
                """ : """
                SELECT id, 'NOTA' AS kind, author_name AS actor, body AS detail, created_at AS event_at,
                    'registrada' AS status FROM staff_notes
                WHERE target_player_id=? AND archived_at IS NULL
                ORDER BY event_at DESC, id DESC LIMIT ? OFFSET ?
                """;
        try (var s = connection.get().prepareStatement(sql)) {
            int targets = history ? 4 : 1;
            for (int i = 1; i <= targets; i++) s.setLong(i, target);
            s.setInt(targets + 1, PAGE_SIZE + 1);
            s.setInt(targets + 2, (page - 1) * PAGE_SIZE);
            try (var r = s.executeQuery()) {
                List<StaffRecord> result = new ArrayList<>();
                while (r.next()) result.add(new StaffRecord(r.getLong("id"), r.getString("kind"),
                        r.getString("actor"), r.getString("detail"), r.getTimestamp("event_at").toInstant(), r.getString("status")));
                return result;
            }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar histórico interno.", e); }
    }
}
