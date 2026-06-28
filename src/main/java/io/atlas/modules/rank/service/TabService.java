package io.atlas.modules.rank.service;

import io.atlas.modules.rank.formatter.RankFormatter;
import io.atlas.modules.rank.model.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Optional;

public class TabService {

    private static final String TEAM_PREFIX = "atlas_rank_";

    private final RankService rankService;
    private final RankFormatter formatter;

    public TabService(RankService rankService) {
        this.rankService = rankService;
        this.formatter = new RankFormatter();
    }

    public void updatePlayer(ServerPlayer player) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        String scoreboardName = player.getScoreboardName();
        Optional<Rank> highestRank = rankService.getHighestRank(player.getUUID());

        if (highestRank.isEmpty()) {
            PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreboardName);
            if (currentTeam != null && currentTeam.getName().startsWith(TEAM_PREFIX)) {
                scoreboard.removePlayerFromTeam(scoreboardName, currentTeam);
            }
            return;
        }

        Rank rank = highestRank.get();
        PlayerTeam team = getOrCreateTeam(scoreboard, rank);
        scoreboard.addPlayerToTeam(scoreboardName, team);
    }

    private PlayerTeam getOrCreateTeam(ServerScoreboard scoreboard, Rank rank) {
        String teamName = TEAM_PREFIX + rank.getId();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        Component prefix = formatter.formatPrefix(rank);
        boolean hasPrefix = rank.getPrefix() != null && !rank.getPrefix().isBlank();
        team.setPlayerPrefix(hasPrefix ? prefix.copy().append(" ") : prefix);
        team.setColor(formatter.resolveTeamColor(rank));
        return team;
    }
}
