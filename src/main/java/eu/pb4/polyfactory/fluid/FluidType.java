package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.fluid.shooting.FluidShootingBehavior;
import eu.pb4.polyfactory.fluid.shooting.NoOpFluidShootingBehavior;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public record FluidType<T>(int density, float heat, Codec<T> dataCodec, T defaultData,
                           Optional<Fluid> backingFluid,
                           Optional<Identifier> textureOverride,
                           Optional<Identifier> solidTexture,
                           ModelRenderType modelRenderType,
                           Optional<ColorProvider<T>> color,
                           Optional<BiFunction<FluidType<T>, T, Component>> name,
                           Function<FluidInstance<T>, ParticleOptions> particleGetter,
                           MaxFlowProvider<T> maxFlow,
                           FlowSpeedProvider<T> flowSpeedMultiplier,
                           Optional<Brightness> brightness,
                           FluidShootingBehavior<T> shootingBehavior
) {

    private static final Map<Fluid, FluidType<?>> FLUID_TO_TYPE = new IdentityHashMap<>();
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR = Comparator.comparingInt(FluidType::density);
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType<?>> CODEC = FactoryRegistries.FLUID_TYPES.byNameCodec();

    public FluidType {
        backingFluid.ifPresent(x -> FLUID_TO_TYPE.put(x, this));
    }

    public static FluidType<?> get(Fluid fluid) {
        return FLUID_TO_TYPE.get(fluid);
    }

    public Component getName() {
        return Component.translatable(Util.makeDescriptionId("fluid_type", FactoryRegistries.FLUID_TYPES.getKey(this)));
    }

    public Component getName(T data) {
        return this.name.isEmpty() ? getName() : this.name.get().apply(this, data);
    }

    public MutableComponent toLabeledAmount(long amount, T data) {
        return Component.empty().append(getName(data)).append(": ").append(getAmountText(amount, data));
    }

    public MutableComponent getAmountText(long amount, T data) {
        if (FactoryRegistries.FLUID_TYPES.wrapAsHolder(this).is(FactoryFluidTags.USE_INGOTS_FOR_AMOUNT)) {
            return FactoryUtil.fluidTextIngots(amount);
        }

        return FactoryUtil.fluidTextGeneric(amount);
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

    public FluidStack<T> ofIngot() {
        return new FluidStack<>(defaultInstance(), FluidConstants.INGOT);
    }

    public FluidStack<T> ofIngots(int mult) {
        return new FluidStack<>(defaultInstance(), FluidConstants.INGOT * mult);
    }

    public FluidStack<T> ofIngot(T data) {
        return new FluidStack<>(this, data, FluidConstants.INGOT);
    }

    public FluidStack<T> ofNugget() {
        return new FluidStack<>(defaultInstance(), FluidConstants.NUGGET);
    }

    public FluidStack<T> ofNuggets(int mult) {
        return new FluidStack<>(defaultInstance(), FluidConstants.NUGGET * mult);
    }

    public FluidStack<T> ofNugget(T data) {
        return new FluidStack<>(this, data, FluidConstants.NUGGET);
    }

    public FluidStack<T> of(long amount, T data) {
        return new FluidStack<T>(this, data, amount);
    }

    public Identifier texture() {
        if (this.textureOverride().isPresent()) {
            return this.textureOverride().get();
        }

        var id = FactoryRegistries.FLUID_TYPES.getKey(this);
        if (id == null) {
            throw new RuntimeException("Unregistered type type!");
        }
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return id("block/fluid/" + id.getPath());
        } else {
            return id.withPrefix("block/fluid/");
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

    public interface MaxFlowProvider<T> {
        long getMaxFlow(@Nullable ServerLevel world, T data);
    }


    public interface FlowSpeedProvider<T> {
        double getSpeedMultiplier(@Nullable ServerLevel world, T data);
    }

    public static <T> Builder<T> of(Codec<T> dataCodec, T defaultData) {
        return new Builder<T>(dataCodec, defaultData);
    }

    public static Builder<Unit> of() {
        return new Builder<>(Unit.CODEC, Unit.INSTANCE);
    }

    public static final class Builder<T> {
        private final Codec<T> dataCodec;
        private final T defaultData;
        private int density = 0;
        private float heat = 0;
        private ModelRenderType modelRenderType = ModelRenderType.SOLID;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Fluid> fluid = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Brightness> brightness = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Identifier> texture = Optional.empty();
        private Optional<Identifier> solidTexture = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<ColorProvider<T>> color = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<BiFunction<FluidType<T>, T, Component>> name = Optional.empty();
        private Function<FluidInstance<T>, ParticleOptions> particleGetter = (x) -> new ItemParticleOption(ParticleTypes.ITEM, FactoryModels.FLUID_FLAT_FULL.get(x));
        private MaxFlowProvider<T> maxFlow = (w, x) -> FluidConstants.BOTTLE;
        private FlowSpeedProvider<T> flowSpeedMultiplier = (w, x) -> 1;
        private FluidShootingBehavior<T> shootingBehavior = new NoOpFluidShootingBehavior<>();

        private Builder(Codec<T> dataCodec, T defaultData) {
            this.dataCodec = dataCodec;
            this.defaultData = defaultData;
        }

        public Builder<T> density(int density) {
            this.density = density;
            return this;
        }

        public Builder<T> heat(float heat) {
            this.heat = heat;
            return this;
        }

        public Builder<T> fluid(Fluid fluid) {
            this.fluid = Optional.ofNullable(fluid);
            if (this.texture.isEmpty()) {
                this.texture(BuiltInRegistries.FLUID.getKey(fluid).withPrefix("block/").withSuffix("_still"));
            }
            return this;
        }

        public Builder<T> texture(Identifier identifier) {
            this.texture = Optional.ofNullable(identifier);
            return this;
        }

        public Builder<T> solidTexture(Identifier identifier) {
            this.solidTexture = Optional.ofNullable(identifier);
            return this;
        }

        public Builder<T> modelRenderType(ModelRenderType type) {
            this.modelRenderType = type;
            return this;
        }

        public Builder<T> transparent() {
            return this.modelRenderType == ModelRenderType.SOLID ? this.modelRenderType(ModelRenderType.TRANSPARENT) : this;
        }

        public Builder<T> color(int color) {
            this.color = Optional.of((x) -> color);
            return this;
        }

        public Builder<T> color(ColorProvider<T> colorProvider) {
            this.color = Optional.of(colorProvider);
            return this;
        }

        public Builder<T> name(BiFunction<FluidType<T>, T, Component> name) {
            this.name = Optional.of(name);
            return this;
        }

        public Builder<T> brightness(int light) {
            return this.brightness(new Brightness(light, light));
        }

        public Builder<T> brightness(Brightness brightness) {
            this.brightness = Optional.ofNullable(brightness);
            return this;
        }

        public Builder<T> particle(ParticleOptions particle) {
            return particle(x -> particle);
        }
        public Builder<T> particle(Function<FluidInstance<T>, ParticleOptions> particle) {
            this.particleGetter = particle;
            return this;
        }

        public Builder<T> maxFlow(long maxFlow) {
            return maxFlow((w, x) -> maxFlow);
        }
        public Builder<T> maxFlow(MaxFlowProvider<T> maxFlow) {
            this.maxFlow = maxFlow;
            return this;
        }

        public Builder<T> flowSpeedMultiplier(double multiplier) {
            return flowSpeedMultiplier((w, x) -> multiplier);
        }
        public Builder<T> flowSpeedMultiplier(FlowSpeedProvider<T> multiplier) {
            this.flowSpeedMultiplier = multiplier;
            return this;
        }

        public Builder<T> shootingBehavior(FluidShootingBehavior<T> behavior) {
            this.shootingBehavior = behavior;
            return this;
        }

        public FluidType<T> build() {
            return new FluidType<>(this.density, this.heat, this.dataCodec, this.defaultData, this.fluid, this.texture, this.solidTexture, this.modelRenderType,
                    this.color, this.name, this.particleGetter, this.maxFlow, this.flowSpeedMultiplier, this.brightness, this.shootingBehavior);
        }
    }
}
