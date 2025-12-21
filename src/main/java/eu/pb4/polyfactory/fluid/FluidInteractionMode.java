package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum FluidInteractionMode implements StringRepresentable {
    INSERT,
    EXTRACT;

    public static final Codec<FluidInteractionMode> CODEC = Codec.stringResolver(FluidInteractionMode::name, FluidInteractionMode::valueOf);

    @Override
    public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
}