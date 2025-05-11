package me.drivz.game.command;

import me.drivz.game.model.Game;
import me.drivz.game.model.GameJoinMode;

import java.util.List;

final class GameJoinCommand extends GameSubCommand {

	GameJoinCommand() {
		super("join/j");

		this.setDescription("Joins a game.");
		this.setUsage("<name>");
		this.setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		final String name = this.joinArgs(0);

		this.checkConsole();
		this.checkGameExists(name);

		Game.findByName(name).joinPlayer(this.getPlayer(), GameJoinMode.PLAYING);
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
	}
}
