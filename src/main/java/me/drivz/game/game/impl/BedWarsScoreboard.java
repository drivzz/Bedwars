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

        final int playerSpawnpoints = this.getGame().getPlayerSpawnPoints().size();
        final int maxPlayers = this.getGame().getMaxPlayers();

        message = Replacer.replaceArray(message,
                "player_spawnpoints", (playerSpawnpoints >= maxPlayers ? "&a" : "") + playerSpawnpoints + "/" + maxPlayers,
                "generator_level", ((BedWarsHeartbeat) this.getGame().getHeartbeat()).getGeneratorsLevel());

        return super.replaceVariables(player, message);
    }

    @Override
    protected List<Object> onEditLines() {
        return Common.newList("Player spawnpoints: {player_spawnpoints}");
    }

    @Override
    public void onGameStart() {
        super.onGameStart();

        this.addRows("Generators level: {generator_level}");
    }

    @Override
    public BedWars getGame() {
        return (BedWars) super.getGame();
    }
}
