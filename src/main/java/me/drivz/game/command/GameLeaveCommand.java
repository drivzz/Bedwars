package me.drivz.game.command;

import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;

import java.util.List;

final class GameLeaveCommand extends GameSubCommand {

	GameLeaveCommand() {
		super("leave/l");

		this.setDescription("Leaves the game you are currently playing.");
	}

	@Override
	protected void onCommand() {
		this.checkConsole();

		final PlayerCache cache = PlayerCache.from(this.getPlayer());
		this.checkBoolean(cache.hasGame(), "You are not playing any game right now.");

		cache.getCurrentGame().leavePlayer(getPlayer());
	}

	@Override
	protected List<String> tabComplete() {
		return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
	}
}
