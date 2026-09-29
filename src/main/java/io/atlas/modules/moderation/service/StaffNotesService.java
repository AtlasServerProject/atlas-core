package io.atlas.modules.moderation.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.moderation.repository.ModerationRepository;
import io.atlas.modules.moderation.repository.StaffNotesRepository;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class StaffNotesService {
    private final ModerationRepository players = new ModerationRepository();
    private final StaffNotesRepository notes = new StaffNotesRepository(DatabaseManager::getConnection);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    public boolean canUse(CommandSourceStack source) {
        var actor = source.getPlayer();
        return actor == null ? source.getEntity() == null && source.hasPermission(4)
                : AuthModule.getAuthService().isAuthenticated(actor.getUUID())
                    && RankModule.getRankService().isStaff(actor.getUUID());
    }

    private ModerationRepository.PlayerRef target(CommandSourceStack source, String username) {
        if (!canUse(source)) throw new IllegalArgumentException("Acesso restrito à staff autenticada e ao console administrativo.");
        var target = players.findPlayer(username).orElseThrow(() -> new IllegalArgumentException("Jogador não encontrado no banco."));
        var actor = source.getPlayer();
        var ranks = RankModule.getRankService();
        boolean allowed = StaffNotesPolicy.mayAccess(actor == null, actor != null
                && AuthModule.getAuthService().isAuthenticated(actor.getUUID()), actor != null && ranks.isStaff(actor.getUUID()),
                actor == null ? 0 : ranks.getHighestRank(actor.getUUID()).map(r -> r.getPriority()).orElse(0),
                ranks.getHighestRank(target.uuid()).map(r -> r.getPriority()).orElse(0));
        if (!allowed) throw new IllegalArgumentException("Você só pode acessar registros de jogadores com cargo inferior ao seu.");
        return target;
    }

    public int add(CommandSourceStack source, String username, String body) {
        return safely(source, () -> {
            var target = target(source, username);
            long id = notes.add(target.id(), actorId(source), actorName(source), StaffNotesPolicy.text(body));
            send(source, "§aNota #" + id + " registrada para " + target.username() + ".");
        });
    }

    public int archive(CommandSourceStack source, String username, long id, String reason) {
        return safely(source, () -> {
            var target = target(source, username);
            if (!notes.archive(target.id(), id, actorId(source), actorName(source), StaffNotesPolicy.text(reason)))
                throw new IllegalArgumentException("Nota não encontrada para esse jogador ou já arquivada.");
            send(source, "§aNota #" + id + " arquivada. Registro preservado em /history " + target.username() + ".");
        });
    }

    public int list(CommandSourceStack source, String username, int page, boolean history) {
        return safely(source, () -> {
            var target = target(source, username);
            var records = notes.list(target.id(), page, history);
            if (records.isEmpty()) { send(source, "§eNenhum registro nesta página para " + target.username() + "."); return; }
            send(source, "§6" + (history ? "Histórico" : "Notas ativas") + " de " + target.username() + " — página " + page + " (UTC)");
            records.stream().limit(StaffNotesRepository.PAGE_SIZE).forEach(r -> send(source,
                    "§7[" + DATE.format(r.at()) + "] §f" + r.type() + " #" + r.id() + " §8| §7" + r.status()
                    + " §8| §7por §f" + display(r.actor()) + "§7: §f" + display(r.text())));
            if (records.size() > StaffNotesRepository.PAGE_SIZE) send(source, "§ePróxima: /"
                    + (history ? "history " : "staffnotes list ") + target.username() + " " + (page + 1));
        });
    }

    // Historical punishment reasons may predate text validation; keep one safe chat line.
    private String display(String text) {
        if (text == null) return "—";
        String clean = text.replaceAll("[\\p{Cc}\\p{Cf}§]", " ");
        return clean.length() > 500 ? clean.substring(0, 500) + "…" : clean;
    }
    private UUID actorId(CommandSourceStack source) { return source.getPlayer() == null ? null : source.getPlayer().getUUID(); }
    private String actorName(CommandSourceStack source) { return source.getPlayer() == null ? "CONSOLE" : source.getPlayer().getName().getString(); }
    private void send(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); }
    private int safely(CommandSourceStack source, Runnable action) {
        try { action.run(); return 1; }
        catch (IllegalArgumentException e) { source.sendFailure(Component.literal("§c" + e.getMessage())); }
        catch (RuntimeException e) {
            AtlasMod.LOGGER.error("Falha em Staff Notes/Histórico", e);
            source.sendFailure(Component.literal("§cNão foi possível acessar os registros. Consulte os logs do servidor."));
        }
        return 0;
    }
}
