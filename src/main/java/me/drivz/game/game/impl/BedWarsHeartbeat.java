package me.drivz.game.game.impl;

import lombok.Getter;
import me.drivz.game.game.GameHeartbeat;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.FileConfig;

/**
 * The countdown responsible for ticking played games
 */
public class BedWarsHeartbeat extends GameHeartbeat {

    @Getter
    private int generatorsLevel = 1;

    /**
     * Create a new countdown
     *
     * @param game
     */
    public BedWarsHeartbeat(final BedWars game) {
        super(game);
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.generatorsLevel = 1;
    }

    @Override
    protected void onTick() {
        super.onTick();

        final int elapsedSeconds = getCountdownSeconds() - getTimeLeft();
        final BedWars bedWars = this.getGame();

        if (elapsedSeconds % 20 == 0) { // every 20 seconds there is an upgrade in how many items are spawned
            this.generatorsLevel++;

            getGame().broadcast("&7Generators were &dupgraded &7to &6Level " + MathUtil.toRoman(this.generatorsLevel) + "&7.");
        }

        if (elapsedSeconds % 2 == 0)
            dropItems(bedWars.getGenerators(GeneratorType.IRON), ItemCreator.of(
                    CompMaterial.IRON_INGOT,
                    "Iron Ingot",
                    "",
                    "Use this to buy items!"));

        if (elapsedSeconds % 4 == 0)
            dropItems(bedWars.getGenerators(GeneratorType.GOLD), ItemCreator.of(
                    CompMaterial.GOLD_INGOT,
                    "Gold Ingot",
                    "",
                    "Use this to buy better items!"));

        if (elapsedSeconds % 6 == 0)
            dropItems(bedWars.getGenerators(GeneratorType.DIAMOND), ItemCreator.of(
                    CompMaterial.DIAMOND,
                    "Diamond",
                    "",
                    "Use this to buy the best items!"));

        if (elapsedSeconds % 8 == 0)
            dropItems(bedWars.getGenerators(GeneratorType.EMERALD), ItemCreator.of(
                    CompMaterial.EMERALD,
                    "Emeral",
                    "",
                    "Use this to buy team upgrades!"));
    }

    /*
     * Drop items at the given locations 1 block above
     */
    private void dropItems(FileConfig.LocationList locations, ItemCreator item) {
        for (final Location location : locations) {
            final Item droppedItem = location.getWorld().dropItem(location.clone().add(0.5, 1, 0.5),
                    item.amount(this.generatorsLevel).make());

            droppedItem.setVelocity(new Vector(Math.random() / 100, 0.20, Math.random() / 100));
        }
    }

    @Override
    public BedWars getGame() {
        return (BedWars) super.getGame();
    }
}
