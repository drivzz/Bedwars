package me.drivz.game.game.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;

@Getter
@RequiredArgsConstructor
public enum GeneratorType {
    IRON("Iron", CompChatColor.WHITE, CompMaterial.IRON_BLOCK),
    GOLD("Gold", CompChatColor.GOLD, CompMaterial.GOLD_BLOCK),
    DIAMOND("Diamond", CompChatColor.AQUA, CompMaterial.DIAMOND_BLOCK),
    EMERALD("Emerald", CompChatColor.GREEN, CompMaterial.EMERALD_BLOCK);

    private final String key;
    private final CompChatColor color;
    private final CompMaterial mask;

    public GeneratorType next() {
        return Common.getNext(this, GeneratorType.values(), true);
    }

    public GeneratorType previous() {
        return Common.getNext(this, GeneratorType.values(), false);
    }
}
