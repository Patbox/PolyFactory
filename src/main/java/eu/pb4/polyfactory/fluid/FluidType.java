package eu.pb4.polyfactory.fluid;

import com.mojang.datafixers.util.Pair;
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
import java.util.OptionalInt;

import static eu.pb4.polyfactory.ModInit.id;

public record FluidType(int density, Optional<Fluid> backingFluid, Optional<Identifier> textureOverride, OptionalInt color) {
    public static final Comparator<FluidType> DENSITY_COMPARATOR = Comparator.comparingInt(FluidType::density);
    public static final Comparator<FluidType> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType> CODEC = FactoryRegistries.FLUID_TYPES.getCodec();

    public static FluidType of(int density) {
        return new FluidType(density,Optional.empty(), Optional.empty(), OptionalInt.empty());
    }
    public static FluidType of(int density, Identifier texture) {
        return new FluidType(density, Optional.empty(), Optional.of(texture), OptionalInt.empty());
    }
    public static FluidType of(int density, Identifier texture, int color) {
        return new FluidType(density, Optional.empty(), Optional.of(texture), OptionalInt.of(color));
    }

    public static FluidType of(int density, Fluid fluid) {
        if (!fluid.isStill(fluid.getDefaultState())) {
            throw new RuntimeException(fluid + " is not still!");
        }

        return new FluidType(density, Optional.of(fluid), Optional.of(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still")), OptionalInt.empty());
    }

    public static FluidType of(int density, Fluid fluid, int color) {
        if (!fluid.isStill(fluid.getDefaultState())) {
            throw new RuntimeException(fluid + " is not still!");
        }

        return new FluidType(density, Optional.of(fluid), Optional.of(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still")), OptionalInt.of(color));
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

    public Identifier texture() {
        if (this.textureOverride().isPresent()) {
            return this.textureOverride().get();
        }

        var id = FactoryRegistries.FLUID_TYPES.getId(this);
        if (id == null) {
            throw new RuntimeException("Unregistered fluid type!");
        }
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return id("block/fluid/" + id.getPath());
        } else {
            return id.withPrefixedPath("block/fluid/");
        }
    }
}
