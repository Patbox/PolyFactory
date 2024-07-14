package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;

import java.util.*;
import java.util.function.BiFunction;

import static eu.pb4.polyfactory.ModInit.id;

public record FluidType<T>(int density, Codec<T> dataCodec, T defaultData,
                           Optional<Fluid> backingFluid,
                           Optional<Identifier> textureOverride,
                           Optional<ColorProvider<T>> color,
                           Optional<BiFunction<FluidType<T>, T, Text>> name
) {

    private static final Map<Fluid, FluidType<?>> FLUID_TO_TYPE = new IdentityHashMap<>();
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR = Comparator.comparingInt(FluidType::density);
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType<?>> CODEC = FactoryRegistries.FLUID_TYPES.getCodec();

    public FluidType {
        backingFluid.ifPresent(x -> FLUID_TO_TYPE.put(x, this));
    }

    public static FluidType<Unit> of(int density) {
        return new FluidType<>(density, Unit.CODEC, Unit.INSTANCE, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    public static FluidType<Unit> of(int density, Identifier texture) {
        return new FluidType<>(density, Unit.CODEC, Unit.INSTANCE, Optional.empty(), Optional.of(texture), Optional.empty(), Optional.empty());
    }
    public static FluidType<Unit> of(int density, Identifier texture, int color) {
        return new FluidType<>(density, Unit.CODEC, Unit.INSTANCE, Optional.empty(), Optional.of(texture), Optional.of((d) -> color), Optional.empty());
    }

    public static <T> FluidType<T> of(int density, Codec<T> dataCodec, T defaultData, ColorProvider<T> color, BiFunction<FluidType<T>, T, Text> textFunction) {
        return new FluidType<T>(density, dataCodec, defaultData, Optional.empty(), Optional.empty(), Optional.of(color), Optional.of(textFunction));
    }

    public static <T> FluidType<T> of(int density, Codec<T> dataCodec, T defaultData, Identifier texture, ColorProvider<T> color, BiFunction<FluidType<T>, T, Text> textFunction) {
        return new FluidType<T>(density, dataCodec, defaultData, Optional.empty(), Optional.of(texture), Optional.of(color), Optional.of(textFunction));
    }

    public static FluidType<Unit> of(int density, Fluid fluid) {
        if (!fluid.isStill(fluid.getDefaultState())) {
            throw new RuntimeException(fluid + " is not still!");
        }

        return new FluidType<Unit>(density, Unit.CODEC, Unit.INSTANCE, Optional.of(fluid), Optional.of(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still")), Optional.empty(), Optional.empty());
    }

    public static FluidType<Unit> of(int density, Fluid fluid, int color) {
        if (!fluid.isStill(fluid.getDefaultState())) {
            throw new RuntimeException(fluid + " is not still!");
        }

        return new FluidType<Unit>(density, Unit.CODEC, Unit.INSTANCE, Optional.of(fluid), Optional.of(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still")), Optional.of((d) -> color), Optional.empty());
    }

    public static FluidType<?> get(Fluid fluid) {
        return FLUID_TO_TYPE.get(fluid);
    }

    public Text getName() {
        return Text.translatable(Util.createTranslationKey("fluid_type", FactoryRegistries.FLUID_TYPES.getId(this)));
    }

    public Text getName(T data) {
        return this.name.isEmpty() ? getName() : this.name.get().apply(this, data);
    }

    public MutableText toLabeledAmount(long amount, T data) {
        return Text.empty().append(getName(data)).append(": ").append(FactoryUtil.fluidText(amount));
    }

    public FluidStack<T> ofBottle() {
        return new FluidStack<T>(defaultInstance(), FluidConstants.BOTTLE);
    }

    public FluidStack<T> ofBucket() {
        return new FluidStack<>(defaultInstance(), FluidConstants.BUCKET);
    }

    public FluidStack<T> of(long amount) {
        return new FluidStack<T>(defaultInstance(), amount);
    }

    public FluidStack<T> ofBottle(T data) {
        return new FluidStack<T>(this, data, FluidConstants.BOTTLE);
    }

    public FluidStack<T> ofBucket(T data) {
        return new FluidStack<T>(this, data, FluidConstants.BUCKET);
    }

    public FluidStack<T> of(long amount, T data) {
        return new FluidStack<T>(this, data, amount);
    }

    public Identifier texture() {
        if (this.textureOverride().isPresent()) {
            return this.textureOverride().get();
        }

        var id = FactoryRegistries.FLUID_TYPES.getId(this);
        if (id == null) {
            throw new RuntimeException("Unregistered type type!");
        }
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return id("block/fluid/" + id.getPath());
        } else {
            return id.withPrefixedPath("block/fluid/");
        }
    }

    public FluidInstance<T> defaultInstance() {
        return FluidInstance.getDefault(this);
    }

    public FluidInstance<T> toInstance(T data) {
        return new FluidInstance<>(this, data);
    }


    public interface ColorProvider<T> {
        int getColor(T data);
    }
}
