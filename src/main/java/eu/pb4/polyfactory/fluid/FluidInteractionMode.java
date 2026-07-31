package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum FluidInteractionMode {
    INSERT,
    EXTRACT,
    ANY,
    NONE;

    public static FluidInteractionMode get(boolean canInsert, boolean canExtract) {
        if (canInsert && canExtract) {
            return ANY;
        } else if (canExtract) {
            return EXTRACT;
        } else if (canInsert) {
            return INSERT;
        } else {
            return NONE;
        }
    }

    public boolean canExtract() {
        return this != INSERT && this != NONE;
    }

    public boolean canInsert() {
        return this != EXTRACT && this != NONE;
    }

    public FluidInteractionMode and(FluidInteractionMode mode) {
        return get(canInsert() && mode.canInsert(), canExtract() && mode.canExtract());
    }
}