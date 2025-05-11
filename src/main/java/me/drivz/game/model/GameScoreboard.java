package me.drivz.game.model;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleScoreboard;

/**
 * A simple game scoreboard
 */
public class GameScoreboard extends SimpleScoreboard {

    /**
     * The game
     */
    private final Game game;

    /**
     * Create a new scoreboard
     *
     * @param game
     */
    public GameScoreboard(final Game game) {
        this.game = game;

        this.setTitle("&8------- &c" + game.getName() + " &8-------");
        this.setTheme(ChatColor.RED, ChatColor.GRAY);
        this.setUpdateDelayTicks(20 /* 1 second */);
    }

    @Override
    protected String replaceVariables(final Player player, String message) {

        message = Replacer.replaceArray(message,
                "remaining_start", Common.plural(this.game.getStartCountdown().getTimeLeft(), "second"),
                "remaining_end", Common.plural(this.game.getHeartbeat().getTimeLeft(), "second"),
                "players", this.game.getPlayers(this.game.getState() == GameState.EDITED ? GameJoinMode.EDITING : GameJoinMode.PLAYING /* ignore spectators */).size(),
                "state", ItemUtil.bountifyCapitalized(this.game.getState()), // PLAYED -> Played
                "lobby_set", this.game.getGameLobbyLocation() != null,
                "region_set", this.game.getRegion().isWhole());

        return message.replace("true", "&ayes").replace("false", "&4no");
    }

    /**
     * Called automatically when the player joins
     *
     * @param player
     */
    public void onPlayerJoin(final Player player) {
        this.show(player);
    }

    /**
     * Called on player leave
     *
     * @param player
     */
    public void onPlayerLeave(final Player player) {
        if (this.isViewing(player))
            this.hide(player);
    }

    /**
     * Called automatically on lobby start
     */
    public void onLobbyStart() {
        this.addRows("",
                "Players: {players}",
                "Starting in: {remaining_start}",
                "State: {state}");
    }

    /**
     * Called automatically when the first player stars to edit the game
     */
    public void onEditStart() {
        this.addRows("",
                "Editing players: {players}",
                "",
                "Lobby: {lobby_set}",
                "Region: {region_set}",
                "",
                "&7Use: /game tools to edit.");
    }

    /**
     * Called automatically when the game starts
     */
    public void onGameStart() {
        this.removeRow("Starting in");
        this.addRows("Time left: {remaining_end}");
    }

    /**
     * Called on game stop
     */
    public void onGameStop() {
        this.clearRows();

        this.stop();
    }
}
