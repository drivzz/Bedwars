package me.drivz.game.game.impl;

import me.drivz.game.PlayerCache;
import me.drivz.game.game.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.RandomNoRepeatPicker;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import java.util.*;

public class BedWars extends Game {

    private Map<GeneratorType, LocationList> generators;
    private LocationList playerSpawnpoints;
    private Map<Location, BlockFace> bedSpawnpoints;

    private final RandomNoRepeatPicker<Location> playerSpawnpointPicker = RandomNoRepeatPicker.newPicker(Location.class);
    private int bedsDestroyed = 0;

    protected BedWars(String name) {
        super(name);
    }

    protected BedWars(String name, @Nullable GameType type) {
        super(name, type);
    }

    @Override
    protected GameHeartbeat compileHeartbeat() {
        return new BedWarsHeartbeat(this);
    }

    @Override
    protected GameScoreboard compileScoreboard() {
        return new BedWarsScoreboard(this);
    }

    @Override
    protected void onLoad() {
        this.generators = this.getMap("Generator", GeneratorType.class, LocationList.class);
        this.playerSpawnpoints = this.getLocationList("Player_Spawnpoint");
        this.bedSpawnpoints = this.getMap("Bed_Spawnpoint", Location.class, BlockFace.class);

        super.onLoad();
    }

    @Override
    protected void onSave() {
        super.onSave();

        this.set("Generator", this.generators);
        this.set("Player_Spawnpoint", this.playerSpawnpoints);
        this.set("Bed_Spawnpoint", this.bedSpawnpoints);
    }

    public LocationList getGenerators(GeneratorType type) {

        if (!this.generators.containsKey(type))
            this.generators.put(type, new LocationList(this));

        return this.generators.get(type);
    }

    public List<Location> getAllGeneratorLocations() {
        final List<Location> locations = new ArrayList<>();

        for (final LocationList location : this.generators.values())
            locations.addAll(location.getLocations());

        return locations;
    }

    public GeneratorType findGeneratorType(Block block) {

        for (final Map.Entry<GeneratorType, LocationList> entry : this.generators.entrySet())
            if (entry.getValue().hasLocation(block.getLocation()))
                return entry.getKey();

        return null;
    }

    public LocationList getPlayerSpawnpoints() {
        return this.playerSpawnpoints;
    }

    public boolean hasBed(Block clickBlock) {
        return this.hasBed(clickBlock.getLocation());
    }

    public boolean hasBed(Location location) {
        return this.findBedStartLocation(location) != null;
    }

    public Location findBedStartLocation(Block block) {
        return this.findBedStartLocation(block.getLocation());
    }

    public Location findBedStartLocation(Location blockLocation) {

        for (Map.Entry<Location, BlockFace> entry : this.bedSpawnpoints.entrySet()) {
            Location bedStartLocation = entry.getKey();
            Location bedEndLocation = bedStartLocation.getBlock().getRelative(entry.getValue()).getLocation();

            if (Valid.locationEquals(blockLocation, bedStartLocation) || Valid.locationEquals(blockLocation, bedEndLocation))
                return bedStartLocation;
        }

        return null;
    }

    public boolean toggleBedSpawnpoint(Player player, Block clickedBlock) {
        Location previousBedBlock = this.findBedStartLocation(clickedBlock);

        if (previousBedBlock != null) {
            this.bedSpawnpoints.remove(previousBedBlock);
            this.save();

            return false; // old block was removed
        }

        this.bedSpawnpoints.put(clickedBlock.getLocation(), PlayerUtil.getFacing(player));
        this.save();

        return true; // new bed block was added
    }

    public Map<Location, BlockFace> getBedSpawnpoints() {
        return Collections.unmodifiableMap(this.bedSpawnpoints);
    }

    // ------------------------------------------------------------------------------------------
    // Game logic
    // ------------------------------------------------------------------------------------------

    @Override
    protected void onGameStart() {
        this.playerSpawnpointPicker.setItems(this.playerSpawnpoints.getLocations());
    }

    @Override
    protected void onGamePostStart() {
        Set<Location> usedBeds = new HashSet<>();

        forEachInAllModes(cache -> {
            Player player = cache.toPlayer();
            Location spawnpoint = cache.getPlayerTag("Spawnpoint");

            Location closestBed = null;
            BlockFace closestFace = null;

            for (Map.Entry<Location, BlockFace> entry : this.bedSpawnpoints.entrySet()) {
                Location bedLocation = entry.getKey();

                if (!usedBeds.contains(bedLocation) && (closestBed == null || bedLocation.distance(spawnpoint) < closestBed.distance(spawnpoint))) {
                    closestBed = bedLocation;
                    closestFace = entry.getValue();
                }
            }

            Valid.checkNotNull(closestFace, "Failed to assign a bed for player!");

            if (!CompMaterial.isBed(closestBed.getBlock()))
                Remain.setBed(closestBed, closestFace);

            Messenger.warn(player, "Your bed has been set to " + Common.shortLocation(closestBed));

            cache.setPlayerTag("BedLocation", closestBed);
            usedBeds.add(closestBed);
        });
    }

    @Override
    protected void onGameStartFor(Player player, PlayerCache cache) {
        Location spawnpoint = this.playerSpawnpointPicker.pickRandom();

        if (spawnpoint != null) {
            this.teleport(player, spawnpoint);

            cache.setPlayerTag("Spawnpoint", spawnpoint);
        } else {
            Common.runLater(() -> {
                this.leavePlayer(player);

                throw new FoException("Unable to pick spawnpoint for " + player.getName() + ", leaving him from " + this.getName());
            });
        }
    }

    @Override
    protected void onGameLeave(Player player) {
        super.onGameLeave(player);

        if (this.isPlayed()) {
            Common.runLater(2, () -> {
                if (this.isStopped())
                    return;

                if (this.getPlayers(GameJoinMode.PLAYING).size() == 1) {
                    Player lastPlayer = this.getPlayers(GameJoinMode.PLAYING).get(0).toPlayer();

                    this.stop(GameStopReason.BEDWARS_WIN);

                    if (this.bedsDestroyed > 0) {
                        BoxedMessage.tell(lastPlayer, "<center>&a&lCONGRATULATIONS\n\n"
                                + "<center>&7You are the last man standing\n"
                                + "<center>&7and &awon &7the game!");

                        Common.runLater(2, () -> {
                            // Give items as rewards ETC
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onGameLobbyStart() {
        this.bedsDestroyed = 0;
    }

    @Override
    public void onBlockBreak(Player player, Block block, BlockBreakEvent event) {

        Location bedLocation = this.findBedStartLocation(block);
        PlayerCache bedOwner = bedLocation != null ? this.findBedOwner(bedLocation) : null;

        if (bedLocation == null || bedOwner == null)
            super.onBlockBreak(player, block, event);

        if (player.getUniqueId().equals(bedOwner.getUniqueId())) {
            Messenger.error(player, "You cannot destroy your own bed, fool!");

            this.cancelEvent();
        }

        bedOwner.removePlayerTag("BedLocation");
        this.broadcastWarn("&e" + player.getName() + " has destroyed " + bedOwner.getPlayerName() + "'s bed!");

        BoxedMessage.tell(bedOwner.toPlayer(), "<center>&6&lBED DESTROYED\n\n",
                "<center>&cYour bed has been destroyed\n" +
                        "<center>&cyou won't respawn on death!");

        this.bedsDestroyed++;
    }

    @Override
    public void onItemSpawn(Item item, ItemSpawnEvent event) {
        if (CompMaterial.isBed(item.getItemStack().getType()))
            this.cancelEvent();
    }

    @Override
    public void onPlayerDeath(PlayerCache cache, PlayerDeathEvent event) {
        super.onPlayerDeath(cache, event);

        if (this.isPlayed()) {
            Player player = event.getEntity();

            if (!this.canRespawn(player, cache)) {
                this.leavePlayer(player);

                BoxedMessage.tell(player, "<center>&c&lGAME OVER\n\n"
                        + "<center>&cYour bed has been destroyed and\n"
                        + "<center>&cyou have died. Better luck next time!");
            }
        }
    }

    @Override
    protected final boolean canRespawn(Player player, PlayerCache cache) {
        return cache.hasPlayerTag("BedLocation");
    }

    @Override
    public final Location getRespawnLocation(Player player) {
        return PlayerCache.from(player).getPlayerTag("Spawnpoint");
    }

    @Override
    public final boolean isSetup() {
        return super.isSetup() && this.isBedWarsSetup();
    }

    protected boolean isBedWarsSetup() {
        return this.playerSpawnpoints.size() >= this.getMaxPlayers() && this.bedSpawnpoints.size() >= this.getMaxPlayers();
    }

    private PlayerCache findBedOwner(Location bedStart) {
        for (PlayerCache cache : this.getPlayers(GameJoinMode.PLAYING))
            if (Valid.locationEquals(bedStart, cache.getPlayerTag("BedLocation")))
                return cache;

        return null;
    }

    public int getRemainingBedCount() {
        int bedCount = 0;

        for (PlayerCache cache : this.getPlayers(GameJoinMode.PLAYING))
            if (cache.hasPlayerTag("BedLocation"))
                bedCount++;

        return bedCount++;
    }

    protected final RandomNoRepeatPicker<Location> getPlayerSpawnpointPicker() {
        return this.playerSpawnpointPicker;
    }

    public final int getBedsDestroyed() {
        return this.bedsDestroyed;
    }

    protected final void increaseBedsDestroyed() {
        this.bedsDestroyed++;
    }
}
