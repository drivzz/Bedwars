package me.drivz.game.game.impl;

import lombok.Getter;
import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;
import me.drivz.game.game.GameHeartbeat;
import me.drivz.game.game.GameScoreboard;
import me.drivz.game.game.GameType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.RandomNoRepeatPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BedWars extends Game {

    private Map<GeneratorType, LocationList> generators;
    private Map<Location, BlockFace> bedSpawnPoints;
    @Getter
    private LocationList playerSpawnPoints;

    private final RandomNoRepeatPicker<Location> playerSpawnPointPicker = RandomNoRepeatPicker.newPicker(Location.class);

    private BedWars(String name) {
        super(name);
    }

    private BedWars(String name, @Nullable GameType type) {
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
        this.playerSpawnPoints = this.getLocationList("Player_Spawn_Point");
        this.bedSpawnPoints = this.getMap("Bed_Spawn_Point", Location.class, BlockFace.class);

        super.onLoad();
    }

    @Override
    protected void onSave() {
        super.onSave();

        this.set("Generator", this.generators);
        this.set("Player_Spawn_Point", playerSpawnPoints);
        this.set("Bed_Spawn_Point", this.bedSpawnPoints);
    }

    @Override
    protected void onGameStart() {
        this.playerSpawnPointPicker.setItems(this.playerSpawnPoints.getLocations());
    }

    @Override
    protected void onGameStartFor(Player player, PlayerCache cache) {
        Location spawnPoint = playerSpawnPointPicker.pickRandom();

        if (spawnPoint != null) {
            this.teleport(player, spawnPoint);

            cache.setPlayerTag("SpawnPoint", spawnPoint);
        } else {
            Common.runLater(() -> {
                this.leavePlayer(player);

                throw new FoException("Unable to pick spawn point for " + player.getName() + ", kicking him from the game " + this.getName());
            });
        }
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

    @Override
    public boolean isSetup() {
        return super.isSetup() && this.playerSpawnPoints.size() >= this.getMaxPlayers();
    }

    @Override
    public Location getRespawnLocation(Player player) {
        return PlayerCache.from(player).getPlayerTag("SpawnPoint");
    }
}
