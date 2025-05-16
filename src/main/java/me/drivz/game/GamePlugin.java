package me.drivz.game;

import me.drivz.game.game.Game;
import me.drivz.game.game.GameStopReason;
import me.drivz.game.task.EscapeTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

public final class GamePlugin extends SimplePlugin {

	@Override
	protected void onPluginStart() {
	}

	@Override
	protected void onReloadablesStart() {
		Common.runTimer(1 * 20, new EscapeTask());

		for (Game game : Game.getGames())
			if (!game.isStopped())
				game.stop(GameStopReason.RELOAD);

		Game.loadGames();
	}
}
