package me.drivz.game.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GameBedWars extends Game {

	private GameBedWars(String name) {
		super(name);
	}

	private GameBedWars(String name, @Nullable GameType type) {
		super(name, type);
	}

	@Override
	public Location getRespawnLocation(Player player) {
		return null;
	}
}
