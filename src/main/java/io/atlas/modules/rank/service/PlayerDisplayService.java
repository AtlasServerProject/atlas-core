package io.atlas.modules.rank.service;

import io.atlas.modules.rank.formatter.RankFormatter;
import io.atlas.modules.rank.model.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.util.Locale;
import java.util.Optional;

public class PlayerDisplayService {

    private static final String TEAM_PREFIX = "aR";
    private static final String UNRANKED_TEAM = "zz_atlas_player";
    private static final int MAX_SORT_PRIORITY = 99_999_999;

    private final RankService rankService;
    private final RankFormatter formatter;

    public PlayerDisplayService(RankService rankService) {
        this.rankService = rankService;
        this.formatter = new RankFormatter();
    }

    public void updatePlayer(ServerPlayer player) {
        updatePlayer(player, rankService.getHighestRank(player.getUUID()));
    }

    public void updatePlayer(ServerPlayer player, Optional<Rank> highestRank) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        String scoreboardName = player.getScoreboardName();

        if (highestRank.isEmpty()) {
            scoreboard.addPlayerToTeam(scoreboardName, getOrCreateUnrankedTeam(scoreboard));
            return;
        }

        PlayerTeam team = getOrCreateTeam(scoreboard, highestRank.get());
        scoreboard.addPlayerToTeam(scoreboardName, team);
    }

    private PlayerTeam getOrCreateTeam(ServerScoreboard scoreboard, Rank rank) {
        String teamName = teamName(rank);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        Component prefix = formatter.formatPrefix(rank);
        boolean hasPrefix = rank.getPrefix() != null && !rank.getPrefix().isBlank();
        team.setPlayerPrefix(hasPrefix ? prefix.copy().append(" ") : prefix);
        team.setColor(formatter.resolveTeamColor(rank));
        team.setNameTagVisibility(Team.Visibility.ALWAYS);
        return team;
    }

    private PlayerTeam getOrCreateUnrankedTeam(ServerScoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayerTeam(UNRANKED_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(UNRANKED_TEAM);
        }
        team.setPlayerPrefix(Component.empty());
        team.setNameTagVisibility(Team.Visibility.ALWAYS);
        return team;
    }

    private String teamName(Rank rank) {
        int normalizedPriority = Math.max(0, Math.min(MAX_SORT_PRIORITY, rank.getPriority()));
        int sortOrder = MAX_SORT_PRIORITY - normalizedPriority;
        long rankId = Math.floorMod(rank.getId(), 1_000_000L);

        return String.format(
                Locale.ROOT,
                "%s%08d%06d",
                TEAM_PREFIX,
                sortOrder,
                rankId
        );
    }
}
