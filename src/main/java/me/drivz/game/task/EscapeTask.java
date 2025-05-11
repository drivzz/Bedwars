package me.drivz.game.task;

import me.drivz.game.PlayerCache;
import me.drivz.game.model.Game;
import me.drivz.game.model.GameJoinMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

public final class EscapeTask extends BukkitRunnable {
    @Override
    public void run() {
        for (Player online : Remain.getOnlinePlayers()) {
            Location location = online.getLocation();
            int minHeight = MinecraftVersion.atLeast(MinecraftVersion.V.v1_16) ? location.getWorld().getMinHeight() : 0;
            if (online.isDead() || location.getY() < minHeight || CompMetadata.hasTempMetadata(online, Game.TAG_TELEPORTING))
                continue;
            PlayerCache cache = PlayerCache.from(online);

            if (cache.hasGame() && !cache.isJoining() && !cache.isLeaving() && cache.getCurrentGameMode() != GameJoinMode.EDITING) {
                Game game = cache.getCurrentGame();
                if (!game.isStarting() && !game.isStopping() && !game.getRegion().isWithin(location)) {
                    game.leavePlayer(online);

                    Messenger.warn(online, "You tried to escape so you were kicked from the game.");
                }
            }
        }
    }
}
