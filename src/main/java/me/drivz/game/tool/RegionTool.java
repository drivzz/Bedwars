package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;
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
		return ItemCreator.of(CompMaterial.IRON_AXE,
						"Region Tool")
				.lore(this.getItemLore())
				.glow(true)
				.make();
	}

	@Override
	protected CompMaterial getBlockMask(Block block, Player player) {
		return CompMaterial.GREEN_STAINED_GLASS;
	}

	@Override
	protected VisualizedRegion getVisualizedRegion(Player player) {
		final Game game = PlayerCache.from(player).getCurrentGame();

		return game != null ? game.getRegion() : null;
	}
}
