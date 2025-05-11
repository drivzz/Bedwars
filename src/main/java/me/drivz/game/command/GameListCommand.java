package me.drivz.game.command;

import java.util.List;

import org.mineacademy.fo.Common;
import me.drivz.game.model.Game;

final class GameListCommand extends GameSubCommand {

	public GameListCommand() {
		super("list");

		setDescription("Lists available games.");
	}

	@Override
	protected void onCommand() {
		tellInfo("Loaded games: " + Common.join(Game.getGames(), game -> game.getName() + " (" + game.getType().getName() + ")"));
	}

	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
