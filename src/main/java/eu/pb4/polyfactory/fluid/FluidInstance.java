package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.FactoryRegistries;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.text.MutableText;
import net.minecraft.util.Unit;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

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
}
