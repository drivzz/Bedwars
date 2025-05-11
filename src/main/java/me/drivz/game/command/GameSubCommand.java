package me.drivz.game.command;

import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleSubCommand;

abstract class GameSubCommand extends SimpleSubCommand {

	protected GameSubCommand(String sublabel) {
		super(sublabel);
	}

	protected final PlayerCache getCache() {
		return isPlayer() ? PlayerCache.from(getPlayer()) : null;
	}

	protected final void checkGameExists(String gameName) {
		this.checkBoolean(Game.isGameLoaded(gameName),
				"No such game: '" + gameName + "'. Available: " + Common.join(Game.getGameNames()));
	}
}
