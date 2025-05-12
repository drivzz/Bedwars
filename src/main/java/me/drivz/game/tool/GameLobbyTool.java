package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.drivz.game.game.Game;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GameLobbyTool extends GameTool {

	@Getter
	private static final GameLobbyTool instance = new GameLobbyTool();

	@Override
	public ItemStack getItem() {
		return ItemCreator.of(CompMaterial.IRON_SHOVEL,
						"Game Lobby Tool",
				"",
				"&7Click to set game",
				"&7lobby point.")
				.glow(true)
				.make();
	}

	@Override
	protected CompMaterial getBlockMask(Block block, Player player) {
		return CompMaterial.BLACK_STAINED_GLASS;
	}

	@Override
	protected void onSuccessfulClick(Player player, Game game, Block block) {
		game.setGameLobbyLocation(block.getLocation());

		Messenger.success(player, "Game lobby point set.");
	}

	@Override
	protected Location getGamePoint(Player player, Game game) {
		return game.getGameLobbyLocation();
	}
}
