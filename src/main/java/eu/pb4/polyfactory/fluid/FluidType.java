package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;

public record FluidType(int density, Optional<Fluid> backingFluid, Optional<Identifier> textureOverride, int color) {
    public static final Comparator<FluidType> DENSITY_COMPARATOR = Comparator.comparingInt(FluidType::density);
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType> CODEC = FactoryRegistries.FLUID_TYPES.getCodec();

    public static FluidType of(int density) {
        return new FluidType(density,Optional.empty(), Optional.empty(), 0xFFFFFF);
    }

    public static FluidType of(int density, Identifier texture, int color) {
        return new FluidType(density, Optional.empty(), Optional.of(texture), color);
    }

    public static FluidType of(int density, Fluid fluid, int color) {
        if (!fluid.isStill(fluid.getDefaultState())) {
            throw new RuntimeException(fluid + " is not still!");
        }

        return new FluidType(density, Optional.of(fluid), Optional.of(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still")), color);
    }

    public Text getName() {
        return Text.translatable(Util.createTranslationKey("fluid_type", FactoryRegistries.FLUID_TYPES.getId(this)));
    }

    public MutableText toLabeledAmount(long amount) {
        return Text.empty().append(getName()).append(": ").append(FactoryUtil.fluidText(amount));
    }

    public FluidStack ofBottle() {
        return new FluidStack(this, FluidConstants.BOTTLE);
    }

    public FluidStack ofBucket() {
        return new FluidStack(this, FluidConstants.BUCKET);
    }

    public FluidStack of(long amount) {
        return new FluidStack(this, amount);
    }
}
