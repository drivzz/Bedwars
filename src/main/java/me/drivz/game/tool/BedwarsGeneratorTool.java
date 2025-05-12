package me.drivz.game.tool;

import java.util.List;

import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;
import me.drivz.game.game.impl.BedWars;
import me.drivz.game.game.impl.GeneratorType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.FileConfig.LocationList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedwarsGeneratorTool extends GameTool {

	@Getter
	private static final BedwarsGeneratorTool instance = new BedwarsGeneratorTool();

	@Override
	public ItemStack getItem() {
		return ItemCreator.of(CompMaterial.IRON_PICKAXE,
				"Generator Spawnpoint",
				"",
				"Click block to set BedWars item",
				"generator. Left/right click air",
				"to switch between items.")
				.glow(true)
				.make();
	}

	@Override
	protected CompMaterial getBlockMask(Block block, Player player) {
		final GeneratorType type = this.getGeneratorType(player, block);
		Valid.checkNotNull(type, "Type cannot be null!");

		return type.getMask();
	}

	@Override
	protected String getBlockName(Block block, Player player) {
		final GeneratorType type = this.getGeneratorType(player, block);
		Valid.checkNotNull(type, "Type cannot be null!");

		return "&8[" + type.getColor() + type.getKey() + "&8]";
	}

	@Override
	protected void onHotbarFocused(Player player) {
		super.onHotbarFocused(player);

		final GeneratorType type = this.getGeneratorType(player);

		Remain.sendActionBar(player, "&fPlacing " + type.getColor() + type.getKey() + " generators. &fRight/left click air to change.");
	}

	@Override
	protected void onSuccessfulAirClick(Player player, Game game, ClickType click) {
		GeneratorType type = this.getGeneratorType(player);

		final PlayerCache cache = PlayerCache.from(player);
		final boolean forward = click == ClickType.RIGHT;

		type = forward ? type.next() : type.previous();
		cache.setPlayerTag("Bedwars_Generator", type);

		Remain.sendActionBar(player, "&7" + type.previous().getKey() + " &7" + (!forward ? "&l<" : " ") + " " + type.getColor() + "&l"
				+ type.getKey() + " &7" + (forward ? "&l>" : " ") + " &7" + type.next().getKey());
	}

	@Override
	protected void onSuccessfulClick(Player player, Game game, Block block) {

		if (!(game instanceof BedWars)) {
			Messenger.error(player, "You can only use this tool for Bedwars games.");

			return;
		}

		final BedWars bedWars = (BedWars) game;
		final GeneratorType type = this.getGeneratorType(player);
		final LocationList locations = bedWars.getGenerators(type);

		final GeneratorType oldType = bedWars.findGeneratorType(block);
		final boolean added;

		if (oldType != null && oldType != type)
			added = bedWars.getGenerators(oldType).toggle(block.getLocation());
		else
			added = locations.toggle(block.getLocation());

		Messenger.success(player, (oldType != null ? oldType.getKey() : type.getKey()) + " point " + (added ? "added" : "removed") + ".");
	}

	@Override
	protected List<Location> getGamePoints(Player player, Game game) {
		return game instanceof BedWars ? ((BedWars) game).getAllGeneratorLocations() : null;
	}

	private GeneratorType getGeneratorType(Player player, Block block) {
		final Game game = this.getCurrentGame(player);

		return game instanceof BedWars ? ((BedWars) game).findGeneratorType(block) : null;
	}

	private GeneratorType getGeneratorType(Player player) {
		final PlayerCache cache = PlayerCache.from(player);
		GeneratorType spawnPoint = cache.getPlayerTag("Bedwars_Generator");

		if (spawnPoint == null) {
			spawnPoint = GeneratorType.values()[0];

			cache.setPlayerTag("Bedwars_Generator", spawnPoint);
		}

		return spawnPoint;
	}
}