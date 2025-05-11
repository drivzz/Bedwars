package me.drivz.game.command;

import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import me.drivz.game.model.GameJoinMode;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;

import java.util.List;

final class GameEditCommand extends GameSubCommand {

	GameEditCommand() {
		super("edit/e");

		this.setDescription("Joins a game for editing.");
		this.setUsage("[name]");
		this.setMinArguments(0);
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		Game game;
		final Player player = this.getPlayer();

		if (args.length == 0) {
			game = PlayerCache.from(player).getCurrentGame();

			if (game == null)
				game = Game.findByLocation(getPlayer().getLocation());

			this.checkNotNull(game, "Unable to locate a game. Type a game name. Available: " + Common.join(Game.getGameNames()));

		} else {
			final String gameName = this.joinArgs(0);
			this.checkGameExists(gameName);

			game = Game.findByName(gameName);
		}

		if (game.isJoined(player))
			game.leavePlayer(player);

		else
			game.joinPlayer(player, GameJoinMode.EDITING);
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
	}
}
