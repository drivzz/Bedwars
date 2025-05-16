package me.drivz.game.command;

import me.drivz.game.game.Game;
import me.drivz.game.game.GameStopReason;

import java.util.List;

final class GameRemoveCommand extends GameSubCommand {

	GameRemoveCommand() {
		super("remove");

		setDescription("Removes a game.");
		setUsage("<name>");
		setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String name = this.joinArgs(0);
		this.checkGameExists(name);

		Game game = Game.findByName(name);

		if (!game.isStopped())
			game.stop(GameStopReason.COMMAND);

		Game.removeGame(name);
		tellSuccess("Removed game '" + name + "'!");
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
	}
}
