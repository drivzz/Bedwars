package me.drivz.game.command;

import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleSubCommand;

abstract class GameSubCommand extends SimpleSubCommand {

	protected GameSubCommand(String sublabel) {
		super(sublabel);
	}

	protected final PlayerCache getCache() {
		return isPlayer() ? PlayerCache.from(getPlayer()) : null;
	}

	protected final Game findGameFromLocationOrFirstArg() {
		Game game;

		if (this.args.length > 0)
			game = this.findGame(this.joinArgs(0));

		else {
			game = Game.findByLocation(getPlayer().getLocation());

			this.checkNotNull(game, "Unable to locate a game. Type a game name. Available: " + Common.join(Game.getGameNames()));
		}

		return game;
	}

	protected final Game findGame(String name) {
		Game game = Game.findByName(name);
		this.checkNotNull(game, "No such game: '" + name + "'. Available: " + Common.join(Game.getGameNames()));

		return game;
	}

	protected final void checkGameExists(String gameName) {
		this.checkBoolean(Game.isGameLoaded(gameName),
				"No such game: '" + gameName + "'. Available: " + Common.join(Game.getGameNames()));
	}
}
