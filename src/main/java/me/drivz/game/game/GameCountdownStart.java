package me.drivz.game.game;

import lombok.Getter;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.Countdown;

/**
 * The countdown responsible for starting games
 */
public class GameCountdownStart extends Countdown {

	/**
	 * The game that to start
	 */
	@Getter
	private final Game game;

	/**
	 * Create a new start countdown
	 */
	protected GameCountdownStart(final Game game) {
		super(game.getLobbyDuration());

		this.game = game;
	}

	/**
	 * Called automatically each tick closer to the start
	 * 1 second by default
	 *
	 * @see Countdown#onTick()
	 */
	@Override
	protected void onTick() {

		// Broadcast every fifth second or every second when there are 5 or less seconds left
		if (this.getTimeLeft() <= 5 || this.getTimeLeft() % 10 == 0)
			this.game.broadcastWarn("Game starts in less than " + Common.plural(this.getTimeLeft(), "second"));
	}

	/**
	 * Stop the game if the ticking fails
	 *
	 * @param t
	 */
	@Override
	protected void onTickError(final Throwable t) {
		this.game.stop(GameStopReason.ERROR);
	}

	/**
	 * Called when the countdown runs up yo!
	 *
	 * @see Countdown#onEnd()
	 */
	@Override
	protected void onEnd() {
		this.game.start();
	}
}
