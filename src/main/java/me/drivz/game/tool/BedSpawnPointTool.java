package me.drivz.game.tool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.drivz.game.game.Game;
import me.drivz.game.game.impl.BedWars;
import me.drivz.game.game.impl.BedWarsTeams;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedSpawnPointTool extends GameTool {

    @Getter
    private static final BedSpawnPointTool instance = new BedSpawnPointTool();

    @Override
    public ItemStack getItem() {
        return ItemCreator.of(CompMaterial.WHITE_BED,
                        "Bed Spawnpoint",
                        "",
                        "Click block to set a",
                        "bed for player/team")
                .glow(true)
                .make();
    }

    @Override
    protected CompMaterial getBlockMask(Block block, Player player) {
        return CompMaterial.WHITE_STAINED_GLASS;
    }

    @Override
    protected String getBlockName(Block block, Player player) {
        return "&8[&fBed Spawnpoint&8]";
    }

    @Override
    protected void onSuccessfulClick(Player player, Game game, Block block) {

        if (!(game instanceof BedWars)) {
            Messenger.error(player, "You can only use this tool for Bedwars games.");

            return;
        }

        final BedWars bedWars = (BedWars) game;
        final boolean hasTeams = this.hasTeams(game);
        final int maxLimit = hasTeams ? ((BedWarsTeams) bedWars).getTeamAmount() : game.getMaxPlayers();

        if (bedWars.getBedSpawnpoints().size() >= maxLimit && !bedWars.hasBed(block)) {
            Messenger.error(player, "Cannot place more beds than game max players (" + maxLimit + ").");

            return;
        }

        final boolean added = bedWars.toggleBedSpawnpoint(player, block);

        Messenger.success(player, "&bBed spawnpoint &7was " + (added ? "added" : "removed") + " (" + bedWars.getBedSpawnpoints().size()
                + "/" + maxLimit + ").");
    }

    @Override
    protected List<Location> getGamePoints(Player player, Game game) {
        Map<Location, BlockFace> beds = game instanceof BedWars ? ((BedWars) game).getBedSpawnpoints() : new HashMap<>();
        List<Location> highlightedPoints = new ArrayList<>();

        for (Map.Entry<Location, BlockFace> entry : beds.entrySet()) {
            Location bedStartLocation = entry.getKey();
            Location bedEndLocation = bedStartLocation.getBlock().getRelative(entry.getValue()).getLocation();

            highlightedPoints.add(bedStartLocation);
            highlightedPoints.add(bedEndLocation);
        }

        return highlightedPoints;
    }
}