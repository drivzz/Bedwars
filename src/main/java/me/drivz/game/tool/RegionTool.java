package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualizedRegion;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionTool extends GameTool {

	@Getter
	private static final RegionTool instance = new RegionTool();

	@Override
	public ItemStack getItem() {
		return ItemCreator.of(CompMaterial.WOODEN_AXE,
				"&6Region Tool")
				.lore(this.getItemLore())
				.glow(true)
				.make();
	}

	@Override
	protected void onSuccessfulClick(Player player, Game game, Block block) {

	}

	@Override
	protected CompMaterial getBlockMask(Block block, Player player) {
		return CompMaterial.GOLD_BLOCK;
	}

	@Override
	protected VisualizedRegion getVisualizedRegion(Player player) {
		final Game game = PlayerCache.from(player).getCurrentGame();

		return game != null ? game.getRegion() : null;
	}
}
