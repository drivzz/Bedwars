package me.drivz.game.game.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.DyeColor;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompColor;

@Getter
@RequiredArgsConstructor
public enum Team {

    RED("Red", CompColor.RED),
    GREEN("Green", CompColor.GREEN),
    BLUE("Blue", CompColor.BLUE),
    YELLOW("Yellow", CompColor.YELLOW);

    private final String name;
    private final CompColor color;

    public CompChatColor getChatColor() {
        return this.color.getChatColor();
    }

    public DyeColor getDye() {
        return this.color.getDye();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
