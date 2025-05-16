package me.drivz.game.game.impl;

import me.drivz.game.game.GameScoreboard;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.Replacer;

import java.util.List;

public class BedWarsScoreboard extends GameScoreboard {

    public BedWarsScoreboard(final BedWars game) {
        super(game);
    }

    @Override
    protected String replaceVariables(final Player player, String message) {

        final int playerSpawnpoints = this.getGame().getPlayerSpawnpoints().size();
        final int bedSpawnpoints = this.getGame().getBedSpawnpoints().size();
        final boolean hasTeams = this.getGame() instanceof BedWarsTeams;
        final int maxPlayers = hasTeams ? (((BedWarsTeams) this.getGame()).getTeamAmount()) : this.getGame().getMaxPlayers();

        message = Replacer.replaceArray(message,
                "player_spawnpoints", (playerSpawnpoints >= maxPlayers ? "&a" : "") + playerSpawnpoints + "/" + maxPlayers,
                "bed_spawnpoints", (bedSpawnpoints >= maxPlayers ? "&a" : "") + bedSpawnpoints + "/" + maxPlayers,
                "bed_count", this.getGame().getRemainingBedCount(),
                "generator_level", ((BedWarsHeartbeat) this.getGame().getHeartbeat()).getGeneratorsLevel());

        if (hasTeams) {
            BedWarsTeams teamGame = (BedWarsTeams) this.getGame();
            if (teamGame.hasTeam(player)) {
                Team team = teamGame.findTeam(player);
                message = Replacer.replaceArray("team", team.getChatColor() + team.getName());
            }
        }

        return super.replaceVariables(player, message);
    }

    @Override
    protected List<Object> onEditLines() {
        final boolean hasTeams = this.getGame() instanceof BedWarsTeams;
        return Common.newList((hasTeams ? "Team" : "Player") + " spawnpoints: {player_spawnpoints}",
                "Bed spawnpoints: {bed_spawnpoints}");
    }

    @Override
    public void onGameStart() {
        super.onGameStart();

        final boolean hasTeams = this.getGame() instanceof BedWarsTeams;

        if (hasTeams)
            this.addRows("Your team: {team}");

        this.addRows("Generators level: {generator_level}",
                "Remaining beds: {bed_count}");
    }

    @Override
    public BedWars getGame() {
        return (BedWars) super.getGame();
    }
}
