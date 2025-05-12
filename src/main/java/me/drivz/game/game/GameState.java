package me.drivz.game.game;

/**
 * The mode in which the game is in
 */
public enum GameState {

	/**
	 * The game is stopped with no players
	 */
	STOPPED,

	/**
	 * The game is starting and counting down to the {@link #PLAYED} mode
	 */
	LOBBY,

	/**
	 * The game is being played
	 */
	PLAYED,

	/**
	 * The game is being edited
	 */
	EDITED;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
