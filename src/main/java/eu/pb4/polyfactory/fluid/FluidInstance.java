package eu.pb4.polyfactory.fluid;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.fluid.shooting.FluidShootingBehavior;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Brightness;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record FluidInstance<T>(FluidType<T> type, T data) {
    public static final Comparator<FluidInstance<?>> DENSITY_COMPARATOR = Comparator.comparingInt(FluidInstance::density);

    public static final Comparator<FluidInstance<?>> DENSITY_COMPARATOR_REVERSED = DENSITY_COMPARATOR.reversed();
    private static final Map<FluidType<?>, MapCodec<FluidInstance<?>>> BASE_CODECS = new IdentityHashMap<>();
    private static final Map<FluidType<?>, FluidInstance<Object>> DEFAULTS = new IdentityHashMap<>();

    public static final MapCodec<FluidInstance<?>> MAP_CODEC = FluidType.CODEC.dispatchMap(FluidInstance::type, BASE_CODECS::get);
    public static final Codec<FluidInstance<?>> CODEC = MAP_CODEC.codec();

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

    public MutableComponent toLabeledAmount(long amount) {
        return this.type.toLabeledAmount(amount, this.data);
    }

    public MutableComponent getAmountText(long amount) {
        return this.type.getAmountText(amount, this.data);
    }

    public Component getName() {
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

    public ParticleOptions particle() {
        return this.type.particleGetter().apply(this);
    }

    public long getMaxFlow(ServerLevel world) {
        return this.type.maxFlow().getMaxFlow(world, this.data);
    }

    public double getFlowSpeedMultiplier(ServerLevel world) {
        return this.type.flowSpeedMultiplier().getSpeedMultiplier(world, this.data);
    }

    public Optional<Brightness> brightness() {
        return this.type.brightness();
    }

    public boolean isIn(TagKey<FluidType<?>> tag) {
        return FactoryRegistries.FLUID_TYPES.wrapAsHolder(this.type).is(tag);
    }

    public FluidShootingBehavior<T> shootingBehavior() {
        return this.type.shootingBehavior();
    }

    public Tag toNbt(HolderLookup.Provider lookup) {
        return CODEC.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    @Nullable
    public static FluidInstance<?> fromNbt(HolderLookup.Provider lookup, Tag element) {
        if (element == null) {
            return null;
        }
        return CODEC.decode(lookup.createSerializationContext(NbtOps.INSTANCE), element).result().map(Pair::getFirst).orElse(null);
    }
}
