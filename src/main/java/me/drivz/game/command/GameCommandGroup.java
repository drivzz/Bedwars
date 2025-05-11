package me.drivz.game.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

@AutoRegister
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GameCommandGroup extends SimpleCommandGroup {

	@Getter(value = AccessLevel.PRIVATE)
	private static final GameCommandGroup instance = new GameCommandGroup();

	@Override
	protected String getCredits() {
		return "Visit mywebsite.com";
	}

	@Override
	protected void registerSubcommands() {

		registerSubcommand(GameSubCommand.class);

		registerSubcommand(new DebugCommand("game.command.debug"));
		registerSubcommand(new ReloadCommand("game.command.reload"));
	}
}
