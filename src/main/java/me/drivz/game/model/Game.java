package me.drivz.game.model;

import lombok.NonNull;
import me.drivz.game.PlayerCache;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.mineacademy.fo.*;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.model.Countdown;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigItems;
import org.mineacademy.fo.settings.SimpleSettings;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.visual.VisualizedRegion;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public abstract class Game extends YamlConfig {

	/**
	 * The folder name where all items are stored
	 */
	private static final String FOLDER = "games";
	public static final String TAG_TELEPORTING = "Game_Teleporting";

	/**
	 * The config helper instance which loads and saves items
	 */
	private static final ConfigItems<? extends Game> loadedFiles = ConfigItems.fromFolder(FOLDER, fileName -> {
		final YamlConfig config = YamlConfig.fromFileFast(FileUtil.getFile(FOLDER + "/" + fileName + ".yml"));
		final GameType type = config.get("Type", GameType.class);

		Valid.checkNotNull(type, "Unrecognized GameType." + config.getObject("Type") + " in " + fileName + "! Available: " + Common.join(GameType.values()));
		return type.getInstanceClass();
	});

	private GameType type;
	private int minPlayers;
	private int maxPlayers;
	private SimpleTime gameDuration;
	private VisualizedRegion region;
	private Location gameLobbyLocation;
	private Location returnBackLocation;
	private SimpleTime lobbyDuration;
	public boolean starting, stopping;

	/* ------------------------------------------------------------------------------- */
	/* Local properties which are not saved to the game settings file */
	/* ------------------------------------------------------------------------------- */

	private final StrictList<PlayerCache> players = new StrictList<>();
	private Countdown startCountdown;
	private Countdown heartbeat;
	private GameScoreboard scoreboard;
	private GameState state = GameState.STOPPED;


	/*
	 * Loads a disk file, used when loading from disk
	 */
	Game(String name) {
		this(name, null);
	}

	/*
	 * Create a new file, used when creating new games via command
	 */
	Game(String name, @Nullable GameType type) {
		this.type = type;

		this.setHeader(
				Common.configLine(),
				"This file stores information about a single game.",
				Common.configLine() + "\n");

		this.loadConfiguration(NO_DEFAULT, FOLDER + "/" + name + ".yml");

		this.startCountdown = new GameCountdownStart(this);
		this.heartbeat = new GameHeartbeat(this);
		this.scoreboard = new GameScoreboard(this);
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoad()
	 */

	@Override
	protected void onLoad() {

		this.minPlayers = getInteger("Min_Players", 1);
		this.maxPlayers = getInteger("Max_Players", 1);
		this.region = get("Region", VisualizedRegion.class, new VisualizedRegion());
		this.gameLobbyLocation = getLocation("Lobby_Location");
		this.returnBackLocation = getLocation("Return_Back_Location");
		this.lobbyDuration = getTime("Lobby_Duration", SimpleTime.from("10 seconds"));
		this.gameDuration = getTime("Game_Duration", SimpleTime.from("5 seconds"));

		if (this.type != null)
			this.save();

		else
			this.type = get("Type", GameType.class);
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#serialize()
	 */
	@Override
	protected void onSave() {
		this.set("Type", this.type);
		this.set("Min_Players", this.minPlayers);
		this.set("Max_Players", this.maxPlayers);
		this.set("Region", this.region);
		this.set("Lobby_Location", this.gameLobbyLocation);
		this.set("Return_Back_Location", this.returnBackLocation);
		this.set("Lobby_Duration", this.lobbyDuration);
		this.set("Game_Duration", this.gameDuration);
	}

	/* ------------------------------------------------------------------------------- */
	/* Configuration getters */
	/* ------------------------------------------------------------------------------- */

	public final GameType getType() {
		return this.type;
	}

	public final Countdown getStartCountdown() {
		return this.startCountdown;
	}

	public final Countdown getHeartbeat() {
		return this.heartbeat;
	}

	public final GameState getState() {
		return this.state;
	}

	public final int getMinPlayers() {
		return this.minPlayers;
	}

	public abstract Location getRespawnLocation(Player player);

	public boolean isDestructionEnabled() {
		return false;
	}

	public final int getMaxPlayers() {
		return this.maxPlayers;
	}

	public final SimpleTime getLobbyDuration() {
		return this.lobbyDuration;
	}

	public final SimpleTime getGameDuration() {
		return this.gameDuration;
	}

	public final VisualizedRegion getRegion() {
		return this.region;
	}

	public final void setRegion(final Location primary, final Location secondary) {
		this.region.updateLocation(primary, secondary);

		this.save();
	}

	public final Location getGameLobbyLocation() {
		return this.gameLobbyLocation;
	}

	public final Location getReturnBackLocation() {
		return this.returnBackLocation;
	}

	public void setReturnBackLocation(Location returnBackLocation) {
		this.returnBackLocation = returnBackLocation;

		this.save();
	}

	public final void setGameLobbyLocation(final Location location) {
		this.gameLobbyLocation = location;

		this.save();
	}

	public boolean isSetup() {
		return this.region.isWhole() && this.gameLobbyLocation != null;
	}

	public final boolean isStarting() {
		return this.starting;
	}

	public final boolean isStopping() {
		return this.stopping;
	}

	public final boolean isStopped() {
		return this.state == GameState.STOPPED;
	}

	public final boolean isEdited() {
		return this.state == GameState.EDITED;
	}

	public final boolean isPlayed() {
		return this.state == GameState.PLAYED;
	}

	@Override
    public final String getName() {
		return super.getName();
	}

	/**
	 * Run a function for all players in the game regardless of their mode
	 *
	 * @param consumer
	 */
	protected final void forEachPlayerInAllModes(final Consumer<Player> consumer) {
		this.forEachPlayer(consumer, null);
	}

	/**
	 * Run a function for each players having the given mode
	 *
	 * @param consumer
	 * @param mode
	 */
	protected final void forEachPlayer(final Consumer<Player> consumer, final GameJoinMode mode) {
		for (final PlayerCache player : this.getPlayers(mode))
			consumer.accept(player.toPlayer());
	}

	/**
	 * Run a function for all players in the game regardless of their mode
	 *
	 * @param consumer
	 */
	protected final void forEachInAllModes(final Consumer<PlayerCache> consumer) {
		this.forEach(consumer, null);
	}

	/**
	 * Run a function for each players having the given mode
	 *
	 * @param consumer
	 * @param mode
	 */
	protected final void forEach(final Consumer<PlayerCache> consumer, final GameJoinMode mode) {
		for (final PlayerCache player : this.getPlayers(mode))
			consumer.accept(player);
	}

	public final boolean isJoined(Player player) {
		for (final PlayerCache otherCache : this.players)
			if (otherCache.getUniqueId().equals(player.getUniqueId()))
				return true;

		return false;
	}

	public final boolean isJoined(PlayerCache cache) {
		return this.players.contains(cache);
	}

	public final List<Player> getBukkitPlayersInAllModes() {
		return this.getBukkitPlayers(null);
	}

	/**
	 * Get a list of players in the given mode
	 *
	 * @param mode
	 * @return
	 */
	public final List<Player> getBukkitPlayers(final GameJoinMode mode) {
		return Common.convert(this.getPlayers(mode), PlayerCache::toPlayer);
	}

	/**
	 * Get game players currently joined in all modes
	 *
	 * @return
	 */
	public final List<PlayerCache> getPlayersInAllModes() {
		return Collections.unmodifiableList(this.players.getSource());
	}

	/**
	 * Get game players in the given mode
	 *
	 * @param mode
	 * @return
	 */
	public final List<PlayerCache> getPlayers(@Nullable final GameJoinMode mode) {
		final List<PlayerCache> foundPlayers = new ArrayList<>();

		for (final PlayerCache otherCache : this.players)
			if (mode == null || (otherCache.hasGame() && otherCache.getCurrentGameMode() == mode))
				foundPlayers.add(otherCache);

		return Collections.unmodifiableList(foundPlayers);
	}

	/**
	 * Return the game player, or null if he is not in this game
	 *
	 * @param player
	 * @return
	 */
	public final PlayerCache findPlayer(final Player player) {
		this.checkIntegrity();

		for (final PlayerCache otherCache : this.players)
			if (otherCache.hasGame() && otherCache.getCurrentGame().equals(this) && otherCache.getUniqueId().equals(player.getUniqueId()))
				return otherCache;

		return null;
	}

	protected final void checkBoolean(boolean value, Player player, String errorMessage) {
		if (!value) {
			Messenger.error(player, errorMessage);

			this.cancelEvent();
		}
	}

	protected final void cancelEvent() {
		throw new EventHandledException(true);
	}

	protected final void returnHandled() {
		throw new EventHandledException(false);
	}

	/*
	 * Runs a few security checks to prevent accidental errors
	 */
	private void checkIntegrity() {

		if (this.state == GameState.STOPPED)
			Valid.checkBoolean(this.players.isEmpty(), "Found players in stopped " + this.getName() + " game: " + this.players);

		int playing = 0, editing = 0, spectating = 0;

		for (final PlayerCache cache : this.players) {
			final Player player = cache.toPlayer();
			final GameJoinMode mode = cache.getCurrentGameMode();

			Valid.checkBoolean(player != null && player.isOnline(), "Found a disconnected player " + player + " in game " + this.getName());

			if (mode == GameJoinMode.PLAYING)
				playing++;

			else if (mode == GameJoinMode.EDITING)
				editing++;

			else if (mode == GameJoinMode.SPECTATING)
				spectating++;
		}

		if (editing > 0) {
			Valid.checkBoolean(this.state == GameState.EDITED, "Game " + this.getName() + " must be in EDIT mode not " + this.state + " while there are " + editing + " editing players!");
			Valid.checkBoolean(playing == 0 && spectating == 0, "Found " + playing + " and " + spectating + " players in edited game " + this.getName());
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Game logic */
	/* ------------------------------------------------------------------------------- */

	public final void start() {
		Valid.checkBoolean(this.state == GameState.LOBBY, "Cannot start game " + this.getName() + " while in the " + this.state + " mode");

		this.state = GameState.PLAYED;
		this.starting = true;

		try {

			if (this.players.size() < this.minPlayers) {
				this.stop();

				return;
			}

			this.heartbeat.launch();
			this.scoreboard.onGameStart();

			// /game forcestart --> bypass lobby waiting
			if (this.startCountdown.isRunning())
				this.startCountdown.cancel();

			try {
				this.onGameStart();

			} catch (final Throwable t) {
				Common.error(t, "Failed start game " + this.getName() + ", stopping for safety");

				this.stop();
			}

			// Close all players inventories
			this.forEachPlayerInAllModes(Player::closeInventory);

			this.broadcastInfo("Game " + this.getName() + " starts now! Players: " + this.players.size());
			Common.log("Started game " + this.getName());
		} finally {
			this.starting = false;
		}
	}

	public final void stop() {
		Valid.checkBoolean(this.state != GameState.STOPPED, "Cannot stop stopped game " + this.getName());

		this.stopping = true;

		try {

			if (this.state != GameState.EDITED) {
				if (this.startCountdown.isRunning())
					this.startCountdown.cancel();

				if (this.heartbeat.isRunning())
					this.heartbeat.cancel();

				this.forEachPlayerInAllModes(player -> this.leavePlayer(player, false));
			}

			this.scoreboard.onGameStop();
			this.cleanEntities();

			try {
				this.onGameStop();

			} catch (final Throwable t) {
				Common.error(t, "Failed to properly stop game " + this.getName());
			}

		} finally {
			this.state = GameState.STOPPED;
			this.players.clear();
			this.stopping = false;
			Common.log("Stopped game " + this.getName());
		}
	}

	private void cleanEntities() {

		if (!this.region.isWhole())
			return;

		final Set<String> ignoredEntities = Common.newSet("PLAYER", "ITEM_FRAME", "PAINTING", "ARMOR_STAND", "LEASH_HITCH", "ENDER_CRYSTAL");

		for (final Entity entity : this.region.getEntities())
			if (!ignoredEntities.contains(entity.getType().toString())) {
				Common.log("Removing " + entity.getType() + " from game " + this.getName() + "[" + Common.shortLocation(entity.getLocation()) + "]");

				entity.remove();
			}
	}


	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Player related stuff
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Joins the player in the game in the given mode
	 *
	 * @param player
	 * @param mode
	 * @return
	 */
	public final boolean joinPlayer(final Player player, final GameJoinMode mode) {
		final PlayerCache cache = PlayerCache.from(player);

		if (!this.canJoin(player, mode))
			return false;

		cache.setJoining(true);

		try {


			if (mode != GameJoinMode.EDITING) {
				PlayerUtil.storeState(player);

				PlayerUtil.normalize(player, true);
			}

			try {
				this.onGameJoin(player, mode);

			} catch (final Throwable t) {
				Common.error(t, "Failed to properly handle " + player.getName() + " joining to game " + this.getName() + ", aborting");

				return false;
			}

			cache.setCurrentGameMode(mode);
			cache.setCurrentGameName(this.getName());
			cache.clearTags();

			this.players.add(cache);

			if (mode != GameJoinMode.EDITING) {
				cache.setPlayerTag("PreviousLocation", player.getLocation());

				this.teleport(player, this.gameLobbyLocation);
			}

			// TODO
			//if (mode == GameJoinMode.SPECTATING)
			//	transformToSpectate(player);

			// Start countdown and change game mode
			if (this.state == GameState.STOPPED)
				if (mode == GameJoinMode.EDITING) {
					Valid.checkBoolean(!this.startCountdown.isRunning(), "Game start countdown already running for " + getName());

					this.state = GameState.EDITED;
					this.scoreboard.onEditStart();

					this.onGameEditStart();

				} else {
					Valid.checkBoolean(!this.startCountdown.isRunning(), "Game start countdown already running for " + this.getName());

					this.state = GameState.LOBBY;
					this.startCountdown.launch();
					this.scoreboard.onLobbyStart();

					this.onGameLobbyStart();
				}

			Messenger.success(player, "You are now " + mode.toString().toLowerCase() + " game '" + this.getName() + "'!");

			this.scoreboard.onPlayerJoin(player);
			this.checkIntegrity();
		} finally {
			cache.setJoining(false);
		}
		return true;
	}

	public final boolean canJoin(Player player, GameJoinMode mode) {
		final PlayerCache cache = PlayerCache.from(player);

		if (cache.getCurrentGame() != null) {
			Messenger.error(player, "You are already joined in game '" + cache.getCurrentGameName() + "'.");

			return false;
		}

		// Perhaps admins joining another player into an game?
		if (player.isDead()) {
			Messenger.error(player, "You cannot join game '" + this.getName() + "' while you are dead.");

			return false;
		}

		if (mode != GameJoinMode.EDITING && (!player.isOnGround() || player.getFallDistance() > 0)) {
			Messenger.error(player, "You cannot join game '" + this.getName() + "' while you are flying.");

			return false;
		}

		if (mode != GameJoinMode.EDITING && player.getFireTicks() > 0) {
			Messenger.error(player, "You cannot join game '" + this.getName() + "' while you are burning.");

			return false;
		}

		if (this.state == GameState.EDITED && mode != GameJoinMode.EDITING) {
			Messenger.error(player, "You cannot join game '" + this.getName() + "' for play while it is being edited.");

			return false;
		}

		if ((this.state == GameState.LOBBY || this.state == GameState.PLAYED) && mode == GameJoinMode.EDITING) {
			Messenger.error(player, "You edit game '" + this.getName() + "' for play while it is being played.");

			return false;
		}

		if (this.state != GameState.PLAYED && mode == GameJoinMode.SPECTATING) {
			Messenger.error(player, "Only games that are being played may be spectated.");

			return false;
		}

		if (this.state == GameState.PLAYED && mode == GameJoinMode.PLAYING) {
			Messenger.error(player, "This game has already started. Type '/game spectate " + this.getName() + "' to observe.");

			return false;
		}

		if (!this.isSetup() && mode != GameJoinMode.EDITING) {
			Messenger.error(player, "Game '" + this.getName() + "' is not yet configured. If you are an admin, run '/game edit " + this.getName() + "' to see what's missing.");

			return false;
		}

		if (mode == GameJoinMode.PLAYING && this.players.size() >= this.getMaxPlayers()) {
			Messenger.error(player, "Game '" + this.getName() + "' is full (" + this.getMaxPlayers() + " players)!");

			return false;
		}

		return true;
	}

	public final void leavePlayer(Player player) {
		this.leavePlayer(player, true);
	}

	private void leavePlayer(Player player, boolean stopIfLast) {
		final PlayerCache cache = PlayerCache.from(player);

		Valid.checkBoolean(!this.isStopped(), "Cannot leave player " + player.getName() + " from stopped game!");
		Valid.checkBoolean(cache.hasGame() && cache.getCurrentGame().equals(this), "Player " + player.getName() + " is not joined in game " + this.getName());

		cache.setLeaving(true);

		try {


			this.scoreboard.onPlayerLeave(player);

			this.players.remove(cache);

			try {
				onGameLeave(player);

			} catch (final Throwable t) {
				Common.error(t, "Failed to properly handle " + player.getName() + " leaving game " + this.getName() + ", stopping for safety");

				if (!this.isStopped()) {
					stop();

					return;
				}
			}

			if (!this.isEdited()) {
				PlayerUtil.normalize(player, true);
				PlayerUtil.restoreState(player);

				final Location previousLocation = cache.getPlayerTag("PreviousLocation");
				Valid.checkNotNull(previousLocation, "Unable to locate previous location for player " + player.getName());

				this.teleport(player, previousLocation);
			}

			// If we are not stopping, remove from the map automatically
			if (this.getPlayers(GameJoinMode.PLAYING).isEmpty() && stopIfLast)
				this.stop();

			Messenger.success(player, "You've left " + cache.getCurrentGameMode().toString().toLowerCase() + " the game '" + this.getName() + "'!");
		} finally {
			cache.setLeaving(false);
			cache.setCurrentGameMode(null);
			cache.setCurrentGameName(null);
			cache.clearTags();
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Messaging */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Sends a message to all players in the game
	 *
	 * @param message
	 */
	public final void broadcastInfo(final String message) {
		this.checkIntegrity();

		this.forEachPlayerInAllModes(player -> Messenger.info(player, message));
	}

	/**
	 * Sends a warning message to all players in the game
	 *
	 * @param message
	 */
	public final void broadcastWarn(final String message) {
		this.checkIntegrity();

		this.forEachPlayerInAllModes(player -> Messenger.warn(player, message));
	}

	/**
	 * Sends a generic no prefix message to all players
	 *
	 * @param message
	 */
	public final void broadcast(final String message) {
		this.checkIntegrity();

		this.forEachPlayerInAllModes(player -> Common.tellNoPrefix(player, message));
	}

	/**
	 * Teleport the player to the given location, or to the fallback location if failed
	 *
	 * @param player
	 * @param location
	 */
	protected final void teleport(final Player player, @NonNull final Location location) {
		Valid.checkBoolean(player != null && player.isOnline(), "Cannot teleport offline players!");
		Valid.checkBoolean(!player.isDead(), "Cannot teleport dead player " + player.getName());

		final Location topOfTheBlock = location.getBlock().getLocation().add(0.5, 1, 0.5);

		// Since we prevent players escaping the game, add a special invisible tag
		// that we use to check if we can actually enable the teleportation
		CompMetadata.setTempMetadata(player, TAG_TELEPORTING);

		final boolean success = player.teleport(topOfTheBlock, PlayerTeleportEvent.TeleportCause.PLUGIN);
		Valid.checkBoolean(success, "Failed to teleport " + player.getName() + " to both primary and fallback location, they may get stuck in the arena!");

		// Remove the tag after the teleport. Also remove in case of failure to clear up
		CompMetadata.removeTempMetadata(player, TAG_TELEPORTING);
	}

	/* ------------------------------------------------------------------------------- */
	/* Overridable methods */
	/* ------------------------------------------------------------------------------- */

	protected void onGameJoin(Player player, GameJoinMode mode) {
	}

	protected void onGameLeave(Player player) {
	}

	protected void onGameEditStart() {
	}

	protected void onGameLobbyStart() {
	}

	protected void onGameStart() {
	}

	protected void onGameStop() {
	}

	public void onPlayerChat(PlayerCache cache, AsyncPlayerChatEvent event) throws EventHandledException {
		final List<Player> gamePlayers = cache.getCurrentGame().getBukkitPlayersInAllModes();

		if (cache.getCurrentGameMode() == GameJoinMode.EDITING)
			throw new EventHandledException();

		event.getRecipients().removeIf(recipient -> !gamePlayers.contains(recipient));
		event.setFormat(Common.colorize("&8[&6" + cache.getCurrentGameName() + "&8] &f" + cache.getPlayerName() + "&8: &f" + event.getMessage()));
	}

	public void onPlayerDeath(PlayerCache cache, PlayerDeathEvent event) {
		Player player = event.getPlayer();

		Remain.respawn(player);
		event.setDeathMessage(null);
	}

	public void onPlayerRespawn(PlayerCache cache, PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Location respawnLocation = getRespawnLocation(player);

		if (this.state == GameState.LOBBY) {
			event.setRespawnLocation(this.getGameLobbyLocation());

			return;
		}
		
		if (respawnLocation != null)
			event.setRespawnLocation(respawnLocation);
	}

	public void onPlayerCommand(PlayerCache cache, PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		GameJoinMode mode = cache.getCurrentGameMode();

		if (mode == GameJoinMode.EDITING)
			returnHandled();

		String label = event.getMessage().split(" ")[0];
		this.checkBoolean(label.equals("/#flp") || Valid.isInList(label, SimpleSettings.MAIN_COMMAND_ALIASES), player, "You cannot execute this command while playing");
	}

	public void onPlayerInteract(PlayerCache cache, PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final GameJoinMode mode = cache.getCurrentGameMode();

		if (mode == GameJoinMode.EDITING)
			this.returnHandled();

		if (mode == GameJoinMode.SPECTATING)
			this.cancelEvent();

		final boolean isBoat = event.hasItem() && CompMaterial.isBoat(event.getItem().getType());
		final boolean isSoil = event.hasBlock() && event.getClickedBlock().getType() == CompMaterial.FARMLAND.getMaterial();

		if (isBoat || isSoil) {
			player.updateInventory();

			this.cancelEvent();
		}
	}

	public void onPvP(Player attacker, Player victim, EntityDamageByEntityEvent event) {
	}

	public void onPlayerDamage(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
	}

	public void onDamage(Entity attacker, Entity victim, EntityDamageByEntityEvent event) {
	}

	public void onDamage(Entity victim, EntityDamageEvent event) {
	}

	public void onPlayerKill(Player killer, LivingEntity victim, EntityDeathEvent event) {
	}

	public void onBlockPlace(Player player, Block block, BlockPlaceEvent event) {
		this.cancelEvent();
	}

	public void onBlockBreak(Player player, Block block, BlockBreakEvent event) {
		this.cancelEvent();
	}

	public void onEntityClick(Player player, Entity entity, PlayerInteractEntityEvent event) {
		if (entity instanceof ItemFrame)
			this.cancelEvent();
	}

	public void onExplosion(Location centerLocation, List<Block> blocks, Cancellable event) {
		if (!this.isPlayed())
			cancelEvent();

		if (event instanceof EntityExplodeEvent)
			((EntityExplodeEvent) event).setYield(0F);

		try {
			if (event instanceof BlockExplodeEvent) // 1.8.8
				((BlockExplodeEvent) event).setYield(0F);

		} catch (final Throwable t) {
			// Old MC
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @param name
	 * @param type
	 * @return
	 */
	public static Game createGame(@NonNull final String name, @NonNull final GameType type) {
		return loadedFiles.loadOrCreateItem(name, () -> type.instantiate(name));
	}

	// 1) /game new bedwars Arena1
	// 2) when we load a disk file

	public static void loadGames() {
		loadedFiles.loadItems();
	}

	/**
	 * 
	 * @param gameName
	 */
	public static void removeGame(final String gameName) {
		loadedFiles.removeItemByName(gameName);
	}

	/**
	 * @param name
	 * @return
	 */
	public static boolean isGameLoaded(final String name) {
		return loadedFiles.isItemLoaded(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public static Game findByName(@NonNull final String name) {
		return loadedFiles.findItem(name);
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	public static List<Game> findByType(final GameType type) {
		final List<Game> items = new ArrayList<>();

		for (final Game item : getGames())
			if (item.getType() == type)
				items.add(item);

		return items;
	}

	public static Game findByLocation(final Location location) {
		for (final Game game : getGames())
			if (game.getRegion().isWhole() && game.getRegion().isWithin(location))
				return game;

		return null;
	}

	public static Collection<? extends Game> getGames() {
		return loadedFiles.getItems();
	}

	public static Set<String> getGameNames() {
		return loadedFiles.getItemNames();
	}
}
