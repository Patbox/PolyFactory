package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.DispatchedMapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public interface DataContainer {
    MapCodec<DataContainer> CODEC = Codec.STRING.dispatchMap("type", data -> data.type().id(),
            type -> DataType.TYPES.get(type).codec()).orElse(StringData.EMPTY);
    static DataContainer of(long count) {
        return new LongData(count);
    }
    static DataContainer of(boolean count) {
        return count ? BoolData.TRUE : BoolData.FALSE;
    }

    DataType<? extends DataContainer> type();

    String asString();
    long asLong();
    double asDouble();
    default boolean isEmpty() {
        return false;
    }

    default int asRedstoneOutput() {
        return (int) MathHelper.clamp(asLong(), 0, 15);
    };
    default char padding() {
        return ' ';
    }

    default boolean forceRight() {
        return false;
    }

    static DataContainer fromNbt(NbtElement compound, RegistryWrapper.WrapperLookup lookup) {
        return CODEC.codec().decode(lookup.getOps(NbtOps.INSTANCE), compound).getOrThrow().getFirst();
    }

    @SuppressWarnings("unchecked")
    default NbtElement createNbt(RegistryWrapper.WrapperLookup lookup) {
        return ((MapCodec<DataContainer>) this.type().codec()).encoder().encodeStart(lookup.getOps(NbtOps.INSTANCE), this).getOrThrow();
    }

    default boolean isTrue() {
        return this.asLong() != 0;
    }
}
