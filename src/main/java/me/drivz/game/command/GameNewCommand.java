package me.drivz.game.command;

import me.drivz.game.game.Game;
import me.drivz.game.game.GameType;

import java.util.List;

final class GameNewCommand extends GameSubCommand {

	GameNewCommand() {
		super("new");

		setDescription("Creates a new game.");
		setUsage("<gameType> <name>");
		setMinArguments(2);
	}

	@Override
	protected void onCommand() {
		final GameType type = this.findEnum(GameType.class, args[0], "No such game type '{0}'. Available: {available}");
		final String name = this.joinArgs(1);
		this.checkBoolean(!Game.isGameLoaded(name), "Game: '" + name + "' already exists!");

		Game.createGame(name, type);
		tellSuccess("Created " + type.getName() + " game '" + name + "'!");
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(GameType.values()) : NO_COMPLETE;
	}
}
