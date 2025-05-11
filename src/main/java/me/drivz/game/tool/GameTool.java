package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.visual.VisualTool;

import javax.annotation.Nullable;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GameTool extends VisualTool {

	@Override
	public void handleBlockClick(Player player, ClickType click, Block block) {
		final Game game = PlayerCache.from(player).getCurrentGame();

		if (game == null) {
			Messenger.error(player, "You must be editing a game to use this tool!");

			return;
		}

		if (!game.isEdited()) {
			Messenger.error(player, "You can only use this tool in an edited game!");

			return;
		}

		// Handle parent
		super.handleBlockClick(player, click, block);

		// Post to us
		this.onSuccessfulClick(player, game, block);

		// Save data
		game.save();
	}

	protected void onSuccessfulClick(Player player, Game game, Block block) {
	}

	@Override
	protected List<Location> getVisualizedPoints(Player player) {
		final List<Location> points = super.getVisualizedPoints(player);
		final Game game = PlayerCache.from(player).getCurrentGame();

		if (game != null) {
			final Location point = this.getGamePoint(player, game);

			if (point != null)
				points.add(point);
		}

		return points;
	}

	@Nullable
	protected Location getGamePoint(Player player, Game game) {
		return null;
	}
}
