package eu.pb4.polyfactory.util;

import net.minecraft.util.DyeColor;

public interface DyeColorExtra {
    int polyfactory$getColor();

    static int getColor(DyeColor color) {
        return ((DyeColorExtra) (Object) color).polyfactory$getColor();
    }
}
