package me.drivz.game.command;

import me.drivz.game.game.Game;
import me.drivz.game.game.GameStopReason;

import java.util.List;

final class GameStopCommand extends GameSubCommand {

    GameStopCommand() {
        super("start/s");

        this.setDescription("Force stop a game in lobby.");
        this.setUsage("[name]");
    }

    @Override
    protected void onCommand() {
        this.checkConsole();
        Game game = this.findGameFromLocationOrFirstArg();

        this.checkBoolean(!game.isStopped(), "Can only stop non-stopped games!");

        game.stop(GameStopReason.COMMAND);
    }

    @Override
    protected List<String> tabComplete() {
        return this.args.length == 1 ? this.completeLastWord(Game.getGameNames()) : NO_COMPLETE;
    }
}