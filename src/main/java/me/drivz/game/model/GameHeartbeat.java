package me.drivz.game.model;

import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.Countdown;

import java.util.Arrays;
import java.util.List;

/**
 * The countdown responsible for ticking played games
 */
public class GameHeartbeat extends Countdown {

	/**
	 * The game that to tick
	 */
	private final Game game;

	/**
	 * Create a new countdown
	 *
	 * @param game
	 */
	public GameHeartbeat(final Game game) {
		super(game.getGameDuration());

		this.game = game;
	}

	/**
	 * Called automatically on startup
	 */
	@Override
	protected void onStart() {
	}

	/**
	 * Called automatically each tick 1 second by default
	 */
	@Override
	protected void onTick() {
		final List<Integer> broadcastTimes = Arrays.asList(20, 30, 60);

		// % == the remaining amount after a division
		// 120 / 120 = 1 . 00
		// 240 / 120 = 2 . 00
		// 239 % 120 = 1 . 99 === 99

		// Broadcast every 2 minutes, when there is less than 10 seconds away and on 20th, 30th and 60th second to end away
		if (this.getTimeLeft() % 120 == 0 || this.getTimeLeft() <= 10 || broadcastTimes.contains(this.getTimeLeft()))
			this.game.broadcastWarn("Game ends in less than " + TimeUtil.formatTimeGeneric(this.getTimeLeft()) + ".");
	}

	/**
	 * Stop the game if the ticking fails
	 *
	 * @param t
	 */
	@Override
	protected void onTickError(final Throwable t) {
		this.game.stop();
	}

	/**
	 * Called when the countdown runs up yo!
	 */
	@Override
	protected void onEnd() {
		this.game.stop();
	}
}
