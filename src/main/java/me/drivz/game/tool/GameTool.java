package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.drivz.game.PlayerCache;
import me.drivz.game.game.Game;
import me.drivz.game.game.impl.BedWarsTeams;
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

	protected void onSuccessfulAirClick(Player player, Game game, ClickType clickType) {
	}

	@Override
	protected void handleAirClick(Player player, ClickType click) {
		Game game = this.getCurrentGame(player);

		if (game != null && game.isEdited())
			this.onSuccessfulAirClick(player, game, click);
	}

	@Override
	protected List<Location> getVisualizedPoints(Player player) {
		final List<Location> points = super.getVisualizedPoints(player);
		final Game game = this.getCurrentGame(player);

		if (game != null) {
			final Location point = this.getGamePoint(player, game);

			if (point != null)
				points.add(point);

			final List<Location> additionalPoints = this.getGamePoints(player, game);

			if (additionalPoints != null)
				points.addAll(additionalPoints);
		}

		return points;
	}

	@Nullable
	protected Location getGamePoint(Player player, Game game) {
		return null;
	}

	@Nullable
	protected List<Location> getGamePoints(Player player, Game game) {
		return null;
	}

	protected final Game getCurrentGame(Player player) {
		return PlayerCache.from(player).getCurrentGame();
	}

	protected final boolean hasTeams(Player player) {
		return this.hasTeams(this.getCurrentGame(player));
	}

	protected final boolean hasTeams(Game game) {
		return game instanceof BedWarsTeams;
	}
}
