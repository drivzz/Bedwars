package me.drivz.game.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mineacademy.fo.ReflectionUtil;

import java.lang.reflect.Constructor;

@RequiredArgsConstructor
public enum GameType {

	BEDWARS("BedWars", GameBedWars.class);

	@Getter
	private final String name;

	@Getter
	private final Class<? extends Game> instanceClass;

	protected <T extends Game> T instantiate(String name) {
		final Constructor<?> constructor = ReflectionUtil.getConstructor(this.instanceClass, String.class, GameType.class);

		return (T) ReflectionUtil.instantiate(constructor, name, this);
	}
}
