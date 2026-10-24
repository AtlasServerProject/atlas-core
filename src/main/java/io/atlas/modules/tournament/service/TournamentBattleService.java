package io.atlas.modules.tournament.service;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.rules.RulesModule;
import io.atlas.modules.audit.service.ActivityLogService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Staff-mediated native Singles PvP. Does not report tournament results. */
public final class TournamentBattleService {
    public int start(CommandSourceStack source, ServerPlayer first, ServerPlayer second, String tier) {
        if (source.isPlayer() && (!AuthModule.getAuthService().isAuthenticated(source.getPlayer().getUUID())
                || RulesModule.service().pending(source.getPlayer().getUUID())
                || !RankModule.getRankService().isStaff(source.getPlayer().getUUID())))
            return fail(source, "Apenas a staff autenticada pode iniciar confrontos.");
        String unavailable = io.atlas.modules.battle.service.PlayerBattleChecks.error(source.getServer(), first, second);
        if (unavailable != null) return fail(source, unavailable);
        io.atlas.modules.competitive.CompetitiveModule.service().begin(tier,first,second,started->{
                String text = "§b[Atlas • Torneios] §f" + source.getTextName() + " iniciou o confronto "+tier+": "
                        + first.getName().getString() + " × " + second.getName().getString() + ". Cláusulas ativas.";
                first.sendSystemMessage(Component.literal(text));second.sendSystemMessage(Component.literal(text));
                source.sendSuccess(() -> Component.literal(text), false);
                ActivityLogService.record("TOURNAMENT_BATTLE_START", source.getTextName(),source.isPlayer()?source.getPlayer().getUUID().toString():"console", "tournament",tier+": "+first.getName().getString()+" x "+second.getName().getString());
        },reason->fail(source,reason));
        return 1;
    }

    private int fail(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
        return 0;
    }
}
