package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.drivz.game.game.Game;
import me.drivz.game.game.impl.BedWars;
import me.drivz.game.game.impl.BedWarsTeams;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.FileConfig.LocationList;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerSpawnPointTool extends GameTool {

    @Getter
    private static final PlayerSpawnPointTool instance = new PlayerSpawnPointTool();

    @Override
    public ItemStack getItem() {
        return ItemCreator.of(CompMaterial.IRON_SWORD,
                        "Player/Team Spawnpoint",
                        "",
                        "Click block to set",
                        "player spawnpoint")
                .glow(true)
                .make();
    }

    @Override
    protected CompMaterial getBlockMask(Block block, Player player) {
        return CompMaterial.BROWN_STAINED_GLASS;
    }

    @Override
    protected String getBlockName(Block block, Player player) {
        return "&8[&ePlayer Spawnpoint&8]";
    }

    @Override
    protected void onSuccessfulClick(Player player, Game game, Block block) {

        if (!(game instanceof BedWars)) {
            Messenger.error(player, "You can only use this tool for Bedwars games.");

            return;
        }

        final BedWars bedWars = (BedWars) game;
        final LocationList points = bedWars.getPlayerSpawnpoints();
        final boolean hasTeams = this.hasTeams(game);
        final int maxLimit = hasTeams ? ((BedWarsTeams) bedWars).getTeamAmount() : game.getMaxPlayers();

        if (points.size() >= maxLimit && !points.hasLocation(block.getLocation())) {
            Messenger.error(player, "Cannot place more points than game max players (" + maxLimit + ").");

            return;
        }

        final boolean added = points.toggle(block.getLocation());
        Messenger.success(player, "Player spawnpoint was " + (added ? "added" : "removed") + " (" + points.size()
                + "/" + maxLimit + ").");
    }

    @Override
    protected List<Location> getGamePoints(Player player, Game game) {
        return game instanceof BedWars ? ((BedWars) game).getPlayerSpawnpoints().getLocations() : null;
    }
}