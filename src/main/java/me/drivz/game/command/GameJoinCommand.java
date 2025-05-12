package me.drivz.game.command;

import me.drivz.game.game.Game;
import me.drivz.game.game.GameJoinMode;

import java.util.List;

final class GameJoinCommand extends GameSubCommand {

	GameJoinCommand() {
		super("join/j");

		this.setDescription("Joins a game.");
		this.setUsage("[name]");
	}

	@Override
	protected void onCommand() {
		this.checkConsole();
		Game game = this.findGameFromLocationOrFirstArg();

		game.joinPlayer(this.getPlayer(), GameJoinMode.PLAYING);
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
	}
}
