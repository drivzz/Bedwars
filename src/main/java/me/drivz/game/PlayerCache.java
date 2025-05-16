package me.drivz.game;

import lombok.Getter;
import lombok.Setter;
import me.drivz.game.game.Game;
import me.drivz.game.game.GameJoinMode;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A sample player cache storing permanent player information
 * to data.db or MySQL database for players.
 */
@Getter
public final class PlayerCache extends YamlConfig {

	/**
	 * The player cache map caching data for players online.
	 */
	private static volatile Map<UUID, PlayerCache> cacheMap = new HashMap<>();

	/**
	 * This instance's player's unique id
	 */
	private final UUID uniqueId;

	/**
	 * This instance's player's name
	 */
	private final String playerName;

	@Setter
	private boolean joining, leaving;

	/*
	 * The mode players is currently in
	 * Null if not joined any arena
	 */
	@Setter
	private GameJoinMode currentGameMode;
	@Setter
	private String currentGameName;

	/*
	 * Not saveable player data whilst in a game
	 */
	private final StrictMap<String, Object> tags = new StrictMap<>();

	/*
	 * Creates a new player cache (see the bottom)
	 */
	private PlayerCache(String name, UUID uniqueId) {
		this.playerName = name;
		this.uniqueId = uniqueId;

		this.setPathPrefix("Players." + uniqueId.toString());
		this.loadConfiguration(NO_DEFAULT, FoConstants.File.DATA);
	}

	/**
	 * Automatically called when loading data from disk.
	 */
	@Override
	protected void onLoad() {

	}

	/**
	 * Called automatically when the file is about to be saved, set your field values here
	 */
	@Override
	public void onSave() {

	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PlayerCache && ((PlayerCache) obj).getUniqueId().equals(this.uniqueId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.uniqueId);
	}

	/* ------------------------------------------------------------------------------- */
	/* Tags methods */
	/* ------------------------------------------------------------------------------- */
	/**
	 *
	 * @return
	 */
	public Game getCurrentGame() {
		if (this.hasGame()) {
			final Game game = Game.findByName(this.currentGameName);
			Valid.checkNotNull(game, "Found player " + this.playerName + " having unloaded game " + this.currentGameName);

			return game;
		}

		return null;
	}

	/**
	 *
	 * @return
	 */
	public boolean hasGame() {

		// Integrity check
		if ((this.currentGameName != null && this.currentGameMode == null) || (this.currentGameName == null && this.currentGameMode != null))
			throw new FoException("Current game and current game mode must both be set or both be null, " + this.getPlayerName() + " had game " + this.currentGameName + " and mode " + this.currentGameMode);

		return this.currentGameName != null;
	}


	/**
	 * Return true if the given player has data at the given key
	 *
	 * @param key
	 * @return
	 */
	public boolean hasPlayerTag(final String key) {
		return getPlayerTag(key) != null;
	}

	/**
	 * Return the a value at the given key for the player, null if not set
	 *
	 * @param <T>
	 * @param key
	 * @return
	 */
	public <T> T getPlayerTag(final String key) {
		final Object value = this.tags.get(key);

		return value != null ? (T) value : null;
	}

	/**
	 * Sets the player a key-value data pair that is persistent until the arena finishes
	 * even if the player gets kicked out
	 *
	 * @param key
	 * @param value
	 */
	public void setPlayerTag(final String key, final Object value) {
		this.tags.override(key, value);
	}

	public void removePlayerTag(final String key) {
		this.tags.remove(key);
	}

	/**
	 *
	 */
	public void clearTags() {
		this.tags.clear();
	}

	/* ------------------------------------------------------------------------------- */
	/* Misc methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return player from cache if online or null otherwise
	 *
	 * @return
	 */
	@Nullable
	public Player toPlayer() {
		final Player player = Remain.getPlayerByUUID(this.uniqueId);

		return player != null && player.isOnline() ? player : null;
	}

	/**
	 * Remove this cached data from memory if it exists
	 */
	public void removeFromMemory() {
		synchronized (cacheMap) {
			cacheMap.remove(this.uniqueId);
		}
	}

	@Override
	public String toString() {
		return "PlayerCache{" + this.playerName + ", " + this.uniqueId + "}";
	}

	/* ------------------------------------------------------------------------------- */
	/* Static access */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return or create new player cache for the given player
	 *
	 * @param player
	 * @return
	 */
	public static PlayerCache from(Player player) {
		synchronized (cacheMap) {
			final UUID uniqueId = player.getUniqueId();
			final String playerName = player.getName();

			PlayerCache cache = cacheMap.get(uniqueId);

			if (cache == null) {
				cache = new PlayerCache(playerName, uniqueId);

				cacheMap.put(uniqueId, cache);
			}

			return cache;
		}
	}

	/**
	 * Clear the entire cache map
	 */
	public static void clearCaches() {
		synchronized (cacheMap) {
			cacheMap.clear();
		}
	}
}
