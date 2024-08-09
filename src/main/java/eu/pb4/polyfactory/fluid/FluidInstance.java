package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.FactoryRegistries;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.World;

import java.util.*;

public record FluidInstance<T>(FluidType<T> type, T data) {
    public static final Comparator<FluidInstance<?>> DENSITY_COMPARATOR = Comparator.comparingInt(FluidInstance::density);

    public static final Comparator<FluidInstance<?>> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    private static final Map<FluidType<?>, MapCodec<FluidInstance<?>>> BASE_CODECS = new IdentityHashMap<>();
    private static final Map<FluidType<?>, FluidInstance<Object>> DEFAULTS = new IdentityHashMap<>();

    public static final MapCodec<FluidInstance<?>> CODEC = FluidType.CODEC.dispatchMap(FluidInstance::type, BASE_CODECS::get);

    public static <T> FluidInstance<T> getDefault(FluidType<T> tFluidType) {
        //noinspection unchecked
        return (FluidInstance<T>) DEFAULTS.get(tFluidType);
    }

    public int density() {
        return type.density();
    }


    public boolean equals(FluidInstance<?> that) {
        if (that == null) return false;
        if (this == that) return true;
        return type == that.type && Objects.equals(data, that.data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FluidInstance<?> that = (FluidInstance<?>) o;
        return type == that.type && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(type) + 31 * data.hashCode();
    }

    static {
        for (var type : FactoryRegistries.FLUID_TYPES) {
            setup(type);
        }
        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            setup(object);
        });
    }

    private static void setup(FluidType<?> type) {
        //noinspection unchecked
        DEFAULTS.put(type, new FluidInstance<>((FluidType<Object>) type, type.defaultData()));
        if (type.dataCodec() == Unit.CODEC) {
            BASE_CODECS.put(type, MapCodec.unit(type.defaultInstance()));
        } else {
            //noinspection unchecked
            var x = (FluidType<Object>) type;
            BASE_CODECS.put(type, x.dataCodec().optionalFieldOf("data", x.defaultData()).xmap(x::toInstance, FluidInstance::data));
        }
    }

    public MutableText toLabeledAmount(long amount) {
        return this.type.toLabeledAmount(amount, this.data);
    }

    public Text getName() {
        return this.type.getName(data);
    }

    public boolean isDefault() {
        return Objects.equals(this.type.defaultData(), this.data);
    }

    public FluidStack<T> stackOf(long amount) {
        return new FluidStack<>(this, amount);
    }

    public FluidStack<T> ofBottle() {
        return new FluidStack<T>(this, FluidConstants.BOTTLE);
    }

    public FluidStack<T> ofBucket() {
        return new FluidStack<>(this, FluidConstants.BUCKET);
    }

    public Identifier texture() {
        return this.type.texture();
    }

    public float heat() {
        return this.type.heat();
    }

    public ParticleEffect particle() {
        return this.type.particleGetter().apply(this);
    }

    public long getMaxFlow(ServerWorld world) {
        return this.type.maxFlow().getMaxFlow(world, this.data);
    }

    public double getFlowSpeedMultiplier(ServerWorld world) {
        return this.type.flowSpeedMultiplier().getSpeedMultiplier(world, this.data);
    }

    public Optional<Brightness> brightness() {
        return this.type.brightness();
    }

    public boolean isIn(TagKey<FluidType<?>> tag) {
        return FactoryRegistries.FLUID_TYPES.getEntry(this.type).isIn(tag);
    }
}
