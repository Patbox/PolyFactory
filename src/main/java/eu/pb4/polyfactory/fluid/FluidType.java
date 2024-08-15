package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.fluid.Fluid;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public record FluidType<T>(int density, float heat, Codec<T> dataCodec, T defaultData,
                           Optional<Fluid> backingFluid,
                           Optional<Identifier> textureOverride,
                           ModelRenderType modelRenderType,
                           Optional<ColorProvider<T>> color,
                           Optional<BiFunction<FluidType<T>, T, Text>> name,
                           Function<FluidInstance<T>, ParticleEffect> particleGetter,
                           MaxFlowProvider<T> maxFlow,
                           FlowSpeedProvider<T> flowSpeedMultiplier,
                           Optional<Brightness> brightness
) {

    private static final Map<Fluid, FluidType<?>> FLUID_TO_TYPE = new IdentityHashMap<>();
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR = Comparator.comparingInt(FluidType::density);
    public static final Comparator<FluidType<?>> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType<?>> CODEC = FactoryRegistries.FLUID_TYPES.getCodec();

    public FluidType {
        backingFluid.ifPresent(x -> FLUID_TO_TYPE.put(x, this));
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

    public interface MaxFlowProvider<T> {
        long getMaxFlow(@Nullable ServerWorld world, T data);
    }


    public interface FlowSpeedProvider<T> {
        double getSpeedMultiplier(@Nullable ServerWorld world, T data);
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
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<ColorProvider<T>> color = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<BiFunction<FluidType<T>, T, Text>> name = Optional.empty();
        private Function<FluidInstance<T>, ParticleEffect> particleGetter = (x) -> new ItemStackParticleEffect(ParticleTypes.ITEM, FactoryModels.FLUID_FLAT_FULL.get(x));
        private MaxFlowProvider<T> maxFlow = (w, x) -> FluidConstants.BOTTLE;
        private FlowSpeedProvider<T> flowSpeedMultiplier = (w, x) -> 1;

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
                this.texture(Registries.FLUID.getId(fluid).withPrefixedPath("block/").withSuffixedPath("_still"));
            }
            return this;
        }

        public Builder<T> texture(Identifier identifier) {
            this.texture = Optional.ofNullable(identifier);
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
            this.modelRenderType = ModelRenderType.COLORED;
            return this;
        }

        public Builder<T> color(ColorProvider<T> colorProvider) {
            this.color = Optional.of(colorProvider);
            this.modelRenderType = ModelRenderType.COLORED;
            return this;
        }

        public Builder<T> name(BiFunction<FluidType<T>, T, Text> name) {
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

        public Builder<T> particle(ParticleEffect particle) {
            return particle(x -> particle);
        }
        public Builder<T> particle(Function<FluidInstance<T>, ParticleEffect> particle) {
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

        public FluidType<T> build() {
            return new FluidType<>(this.density, this.heat, this.dataCodec, this.defaultData, this.fluid, this.texture, this.modelRenderType,
                    this.color, this.name, this.particleGetter, this.maxFlow, this.flowSpeedMultiplier, this.brightness);
        }
    }
}
