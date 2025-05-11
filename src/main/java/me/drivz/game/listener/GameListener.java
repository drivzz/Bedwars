package me.drivz.game.listener;

import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import me.drivz.game.model.GameJoinMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.event.RocketExplosionEvent;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

@AutoRegister
public final class GameListener implements Listener {

	/**
	 * Listen for player join and loads his data
	 *
	 * @param event
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Game game = Game.findByLocation(player.getLocation());

		PlayerCache.from(player); // Load player's cache

		if (game != null) {
			Valid.checkBoolean(!game.isJoined(player), "Found disconnected dude " + player.getName() + " in game " + game.getName() + " that just joined the server");

			if (!player.isOp()) {
				Location returnLocation = Common.getOrDefault(game.getReturnBackLocation(), player.getWorld().getSpawnLocation());

				player.teleport(returnLocation.add(0.5, 1, 0.5));
				Messenger.warn(player, "You have been teleported away from a stopped game's region.");
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onTeleport(final PlayerTeleportEvent event) {
		final Player player = event.getPlayer();

		if (CompMetadata.hasTempMetadata(player, Game.TAG_TELEPORTING))
			return;

		final SerializedMap moveData = calculateMoveData(event.getFrom(), event.getTo(), player);
		final PlayerCache cache = PlayerCache.from(player);
		String errorMessage = null;

		if (!cache.hasGame() && moveData.getBoolean("toIsArena") && !event.getFrom().equals(event.getTo())) {
			errorMessage = "You cannot teleport into the game unless you edit it with '/game edit'.";

		} else if (cache.hasGame() && cache.getCurrentGameMode() != GameJoinMode.EDITING) {
			if (moveData.getBoolean("leaving")) // prevents /tpa
				errorMessage = "You cannot teleport away from game unless you leave with '/game leave' first.";

			else if (moveData.getBoolean("entering"))
				errorMessage = "Your destination is in a game. Edit it with '/game edit' first.";
		}

		if (errorMessage != null) {
			Messenger.error(player, errorMessage);

			event.setCancelled(true);
		}
	}

	/**
	 * Automatically unload player's cache on his exit to save memory.
	 *
	 * @param event
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final PlayerCache cache = PlayerCache.from(player);

		if (cache.hasGame())
			cache.getCurrentGame().leavePlayer(player);

		cache.save();
		cache.removeFromMemory(); // Unload player's cache
	}

	/**
	 *
	 * @param event
	 */
	// if chat formatting plugin has NORMAL priority, it comes after us, and already sees filtered players
	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();
		final PlayerCache cache = PlayerCache.from(player);

		if (cache.hasGame()) {
			try {
				cache.getCurrentGame().onPlayerChat(cache, event);
			} catch (EventHandledException ex) {
				event.setCancelled(ex.isCancelled());
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getPlayer();
		PlayerCache cache = PlayerCache.from(player);

		if (cache.hasGame()) {
			try {
				cache.getCurrentGame().onPlayerDeath(cache, event);
			} catch (EventHandledException ex) {
				// let bukkit take care of the rest
			}
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		PlayerCache cache = PlayerCache.from(player);

		if (cache.hasGame()) {
			try {
				cache.getCurrentGame().onPlayerRespawn(cache, event);
			} catch (EventHandledException ex) {
				// let bukkit take care of the rest
			}
		}
	}

	@EventHandler
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		PlayerCache cache = PlayerCache.from(player);

		if (cache.hasGame()) {
			try {
				cache.getCurrentGame().onPlayerCommand(cache, event);
			} catch (EventHandledException ex) {
				event.setCancelled(ex.isCancelled());
			}
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Action action = event.getAction();
		Game game = Game.findByLocation(event.hasBlock() ? event.getClickedBlock().getLocation() : player.getLocation());

		if (game != null) {
			PlayerCache gamePlayer = game.findPlayer(player);

			if (gamePlayer == null) {

				if (!action.toString().contains("AIR") && action != Action.PHYSICAL && !Remain.isInteractEventPrimaryHand(event))
					Messenger.warn(player, "Use '/game edit' to make changes to this game.");

				event.setCancelled(true);
				player.updateInventory();
			}

			else {
				try {
					gamePlayer.getCurrentGame().onPlayerInteract(gamePlayer, event);

				} catch (EventHandledException ex) {
					// Handled upstream
				}
			}
		}
	}

	@EventHandler
	public void onFlight(PlayerToggleFlightEvent event) {
		this.executeIfPlayingGame(event, ((player, cache) -> {
			if (cache.getCurrentGameMode() == GameJoinMode.SPECTATING || cache.isJoining() || cache.isLeaving())
				return;

			player.setAllowFlight(false);
			player.setFlying(false);

			Messenger.error(player, "You cannot fly while playing a game.");
		}));
	}

	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		this.executeIfPlayingGame(event, ((player, cache) -> {
			if (cache.isJoining() || cache.isLeaving())
				return;

			event.setCancelled(true);
			Messenger.error(player, "You cannot change gamemode while playing a game.");
		}));
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		PlayerCache cache = PlayerCache.from(player);
		GameJoinMode mode = cache.getCurrentGameMode();

		if (cache.hasGame() && mode != GameJoinMode.EDITING)
			if (mode == GameJoinMode.SPECTATING && Menu.getMenu(player) == null)
				event.setCancelled(true);
	}

	@EventHandler
	public void onExpChange(PlayerExpChangeEvent event) {
		this.executeIfPlayingGame(event, (player, cache) -> event.setAmount(0));
	}

	@EventHandler
	public void onBedEnter(PlayerBedEnterEvent event) {
		Game arena = Game.findByLocation(event.getBed().getLocation());

		if (arena != null) {
			PlayerCache cache = PlayerCache.from(event.getPlayer());

			if (cache.getCurrentGameMode() == GameJoinMode.EDITING)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onVehicleCreate(VehicleCreateEvent event) {
		if (event instanceof Cancellable)
			this.cancelIfInStoppedGameOrLobby(event, event.getVehicle());
	}

	@EventHandler
	public void onEntityByEntityCombust(EntityCombustByEntityEvent event) {
		this.cancelIfInStoppedGameOrLobby(event, event.getCombuster());
	}

	@EventHandler
	public void onEntityInteract(final EntityInteractEvent event) {
		this.cancelIfInStoppedGameOrLobby(event, event.getEntity());
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		this.cancelIfInGame(event, event.getEntity().getLocation());
	}

	@EventHandler
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		this.cancelIfInStoppedGameOrLobby(event, event.getEntity());

		if (!event.isCancelled() && event.getEntity() instanceof EnderCrystal) {
			Game arena = Game.findByLocation(event.getEntity().getLocation());

			if (arena != null)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent event) {
		this.preventBlockGrief(event, event.getLocation(), event.blockList());

		if (event.getEntity() instanceof EnderCrystal) {
			final Game game = Game.findByLocation(event.getLocation());

			if (game != null)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityTarget(final EntityTargetEvent event) {
		final Entity from = event.getEntity();
		final Entity target = event.getTarget();

		final Game fromGame = Game.findByLocation(from.getLocation());

		// Prevent exp from being drawn into players
		if (from instanceof ExperienceOrb && fromGame != null) {
			from.remove();

			return;
		}

		final Game targetGame = target != null ? Game.findByLocation(target.getLocation()) : null;

		if (targetGame != null) {
			if (!targetGame.isPlayed() && !targetGame.isEdited())
				event.setCancelled(true);

			else if (fromGame == null || !fromGame.equals(targetGame))
				event.setCancelled(true);
		}

		if (target instanceof Player) {
			final PlayerCache cache = PlayerCache.from((Player) target);

			// Prevent players in editing or spectating mode from being targeted
			if (cache.hasGame() && cache.getCurrentGameMode() != GameJoinMode.PLAYING)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageEvent event) {
		final Entity victim = event.getEntity();
		final Game game = Game.findByLocation(victim.getLocation());

		if (game != null) {
			if (!game.isPlayed() && !game.isEdited()) {
				event.setCancelled(true);

				return;
			}

			if (victim instanceof Player) {
				final Player player = (Player) victim;
				final PlayerCache cache = PlayerCache.from(player);

				if (cache.getCurrentGameMode() == GameJoinMode.SPECTATING) {
					event.setCancelled(true);

					player.setFireTicks(0);
					return;
				}
			}

			try {
				game.onDamage(victim, event);

			} catch (final EventHandledException ex) {
				// Handled
			}
		}
	}

	@EventHandler
	public void onEntityDeath(final EntityDeathEvent event) {
		final LivingEntity victim = event.getEntity();
		final Game game = Game.findByLocation(victim.getLocation());

		if (game != null) {
			final Player killer = victim.getKiller();
			final PlayerCache killerCache = killer != null ? PlayerCache.from(killer) : null;

			event.setDroppedExp(0);
			event.getDrops().clear();

			// If the killer is a player who's playing in the same arena, call the method
			if (killerCache != null && killerCache.getCurrentGameMode() == GameJoinMode.PLAYING && killerCache.getCurrentGame().equals(game)) {
				try {
					game.onPlayerKill(killer, victim, event);

				} catch (final EventHandledException ex) {
					// Handled
				}
			}
		}
	}

	@EventHandler
	public void onCreatureSpawn(final CreatureSpawnEvent event) {
		final Game game = Game.findByLocation(event.getLocation());

		if (game != null && !game.isPlayed() && !game.isEdited())
			event.setCancelled(true);
	}

	@EventHandler
	public void onItemSpawn(final ItemSpawnEvent event) {
		this.cancelIfInStoppedGameOrLobby(event, event.getLocation());

		if (event.isCancelled())
			return;

		Entity entity = event.getEntity();
		Game game = Game.findByLocation(entity.getLocation());

		if (game == null)
			EntityUtil.trackFlying(entity, () -> {
				final Game gameInNewLocation = Game.findByLocation(entity.getLocation());

				if (gameInNewLocation != null)
					entity.remove();
			});
	}

	@EventHandler
	public void onBlockDispense(final BlockDispenseEvent event) {
		this.cancelIfInStoppedGameOrLobby(event, event.getBlock().getLocation());
	}

	@EventHandler
	public void onBucketFill(final PlayerBucketFillEvent event) {
		preventBucketGrief(event.getBlockClicked().getLocation(), event);
	}

	@EventHandler
	public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
		preventBucketGrief(event.getBlockClicked().getLocation(), event);
	}

	private <T extends PlayerEvent & Cancellable> void preventBucketGrief(final Location location, final T event) {
		final Game game = Game.findByLocation(location);

		if (game != null) {
			final PlayerCache cache = game.findPlayer(event.getPlayer());

			if (cache == null || cache.getCurrentGameMode() != GameJoinMode.EDITING)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		this.cancelIfInGame(event, event.getBlock().getLocation());
	}

	@EventHandler
	public void onDoorBreak(final EntityBreakDoorEvent event) {
		this.cancelIfInGame(event, event.getBlock().getLocation());
	}

	@EventHandler
	public void onEggThrow(final PlayerEggThrowEvent event) {
		final Egg egg = event.getEgg();
		final Game game = Game.findByLocation(egg.getLocation());

		// Prevent spawning chickens in arenas from eggs
		if (game != null)
			event.setHatching(false);
	}

	@EventHandler
	public void onPotionSplash(final PotionSplashEvent event) {
		preventProjectileGrief(event.getEntity());
	}

	@EventHandler
	public void onProjectileLaunch(final ProjectileLaunchEvent event) {
		preventProjectileGrief(event.getEntity());
	}

	@EventHandler
	public void onProjectileHit(final ProjectileHitEvent event) {
		preventProjectileGrief(event.getEntity());
	}

	@EventHandler
	public void onRocketExplosion(final RocketExplosionEvent event) {
		preventProjectileGrief(event.getProjectile());
	}

	private void preventProjectileGrief(final Projectile projectile) {
		final Game game = Game.findByLocation(projectile.getLocation());

		if (game != null) {

			if (!game.isPlayed() && !game.isEdited())
				projectile.remove();

			else if (projectile.getShooter() instanceof Player) {
				final PlayerCache cache = game.findPlayer((Player) projectile.getShooter());

				if (cache == null || cache.getCurrentGameMode() == GameJoinMode.SPECTATING) {
					projectile.remove();

					try {
						if (projectile instanceof Arrow)
							((Arrow) projectile).setDamage(0);
					} catch (final Throwable t) {
						// Old MC
					}
				}
			}
		}
	}

	@EventHandler
	public void onHangingPlace(final HangingPlaceEvent event) {
		preventHangingGrief(event.getEntity(), event);
	}

	@EventHandler
	public void onHangingBreak(final HangingBreakEvent event) {
		preventHangingGrief(event.getEntity(), event);
	}

	private void preventHangingGrief(final Entity hanging, final Cancellable event) {
		final Game game = Game.findByLocation(hanging.getLocation());

		if (game != null && !game.isEdited())
			event.setCancelled(true);
	}

	@EventHandler
	public void onPistonExtend(final BlockPistonExtendEvent event) {
		preventPistonMovement(event, event.getBlocks());
	}

	@EventHandler
	public void onPistonRetract(final BlockPistonRetractEvent event) {
		try {
			preventPistonMovement(event, event.getBlocks());
		} catch (final NoSuchMethodError ex) {
			// Old MC lack the event.getBlocks method
		}
	}

	private void preventPistonMovement(final BlockPistonEvent event, List<Block> blocks) {
		final BlockFace direction = event.getDirection();
		final Game pistonGame = Game.findByLocation(event.getBlock().getLocation());

		// Clone the list otherwise it wont work
		blocks = new ArrayList<>(blocks);

		// Calculate blocks ONE step ahed in the push/pull direction
		for (int i = 0; i < blocks.size(); i++) {
			final Block block = blocks.get(i);

			blocks.set(i, block.getRelative(direction));
		}

		for (final Block block : blocks) {
			final Game game = Game.findByLocation(block.getLocation());

			if (game != null && pistonGame == null || game == null && pistonGame != null) {
				event.setCancelled(true);

				break;
			}
		}
	}

	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		preventBuild(event.getPlayer(), event, false);
	}

	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		preventBuild(event.getPlayer(), event, true);
	}

	private <T extends BlockEvent & Cancellable> void preventBuild(final Player player, final T event, boolean place) {
		final Game game = Game.findByLocation(event.getBlock().getLocation());

		if (game != null) {
			final PlayerCache gamePlayer = game.findPlayer(player);
			final Block block = event.getBlock();

			if (gamePlayer == null) {
				Messenger.warn(player, "You cannot build unless you do '/game edit' first.");

				event.setCancelled(true);
				return;
			}

			if (gamePlayer.getCurrentGameMode() == GameJoinMode.EDITING)
				return;

			if (gamePlayer.getCurrentGameMode() == GameJoinMode.SPECTATING) {
				event.setCancelled(true);

				return;
			}

			try {
				if (place)
					game.onBlockPlace(player, block, (BlockPlaceEvent) event);
				else
					game.onBlockBreak(player, block, (BlockBreakEvent) event);

			} catch (final EventHandledException ex) {
				event.setCancelled(ex.isCancelled());
			}
		}
	}

	@EventHandler
	public void onItemDrop(final PlayerDropItemEvent event) {
		preventItemGrief(event, event.getItemDrop());
	}

	private <T extends PlayerEvent & Cancellable> void preventItemGrief(final T event, final Item item) {
		preventItemGrief(event.getPlayer(), event, item);
	}

	private <T extends PlayerEvent & Cancellable> void preventItemGrief(final Player player, final Cancellable event, final Item item) {
		final Game gameAtLocation = Game.findByLocation(item.getLocation());

		if (gameAtLocation == null)
			return;

		if (!gameAtLocation.isEdited() && !gameAtLocation.isPlayed()) {
			event.setCancelled(true);

			return;
		}

		final PlayerCache cache = gameAtLocation.findPlayer(player);

		if (cache == null || cache.getCurrentGameMode() == GameJoinMode.SPECTATING)
			event.setCancelled(true);
	}

	@EventHandler
	public void onBlockTeleport(BlockFromToEvent event) {
		final Block block = event.getBlock();
		final SerializedMap moveData = calculateMoveData(block.getLocation(), event.getToBlock().getLocation());
		final Game fromArena = Game.findByName(moveData.getString("from"));

		if (moveData.getBoolean("leaving") || moveData.getBoolean("entering")
				|| (fromArena != null && block.getType() == CompMaterial.DRAGON_EGG.getMaterial()))
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onTeleport(final EntityTeleportEvent event) {
		final Entity entity = event.getEntity();

		if (entity instanceof Player)
			return;

		final SerializedMap moveData = calculateMoveData(event.getFrom(), event.getFrom());

		if (moveData.getBoolean("leaving") || moveData.getBoolean("entering"))
			event.setCancelled(true);
	}

	private SerializedMap calculateMoveData(final Location from, final Location to) {
		return calculateMoveData(from, to, null);
	}

	private SerializedMap calculateMoveData(final Location from, final Location to, @Nullable final Player fromPlayer) {
		final Game gameFrom = fromPlayer != null ? PlayerCache.from(fromPlayer).getCurrentGame() : Game.findByLocation(from);
		final Game gameTo = Game.findByLocation(to);

		final boolean isLeaving = gameFrom != null && (gameTo == null || !gameFrom.equals(gameTo));
		final boolean isEntering = gameTo != null && (gameFrom == null || !gameTo.equals(gameFrom));

		return SerializedMap.ofArray(
				"to", gameTo != null ? gameTo.getName() : "",
				"from", gameFrom != null ? gameFrom.getName() : "",
				"toIsArena", gameTo != null,
				"fromIsArena", gameFrom != null,
				"leaving", isLeaving,
				"entering", isEntering);
	}

	private void preventBlockGrief(final Cancellable event, final Location centerLocation, final List<Block> blocks) {
		final Game fromGame = Game.findByLocation(centerLocation);

		if (fromGame != null) {
			try {
				fromGame.onExplosion(centerLocation, blocks, event);

			} catch (final EventHandledException ex) {
				if (ex.isCancelled()) {
					event.setCancelled(true);

					return;
				}
			}
		}

		for (final Iterator<Block> it = blocks.iterator(); it.hasNext();) {
			final Block block = it.next();
			final Game otherGame = Game.findByLocation(block.getLocation());

			if (otherGame == null && fromGame != null) {
				it.remove();

				continue;
			}

			if (otherGame != null && (!otherGame.isDestructionEnabled() || !otherGame.isPlayed()))
				it.remove();
		}
	}

	private void cancelIfInGame(Cancellable event, Location location) {
		Game game = Game.findByLocation(location);

		if (game != null)
			event.setCancelled(true);
	}

	private void cancelIfInStoppedGameOrLobby(Cancellable event, Entity entity) {
		this.cancelIfInStoppedGameOrLobby(event, entity.getLocation());
	}

	private void cancelIfInStoppedGameOrLobby(Cancellable event, Location location) {
		Game game = Game.findByLocation(location);

		if (game != null && !game.isPlayed() && !game.isEdited())
			event.setCancelled(true);
	}

	 private void executeIfPlayingGame(PlayerEvent event, BiConsumer<Player, PlayerCache> consumer) {
		 Player player = event.getPlayer();
		 PlayerCache cache = PlayerCache.from(player);

		 if (cache.hasGame() && cache.getCurrentGameMode() != GameJoinMode.EDITING)
			consumer.accept(player, cache);
	 }
}
