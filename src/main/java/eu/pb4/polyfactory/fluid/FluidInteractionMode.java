package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public enum FluidInteractionMode implements StringIdentifiable {
    INSERT,
    EXTRACT;

    public static final Codec<FluidInteractionMode> CODEC = Codec.stringResolver(FluidInteractionMode::name, FluidInteractionMode::valueOf);

    @Override
    public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
}