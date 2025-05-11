package me.drivz.game.model;

/**
 * What mode is the player joined the game?
 */
public enum GameJoinMode {

	/**
	 * The player is playing the game
	 */
	PLAYING,

	/**
	 * The player is editing the game
	 */
	EDITING,

	/**
	 * The player is dead (or joined later) and is now spectating the game
	 */
	SPECTATING;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
