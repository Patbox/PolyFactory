package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public interface DataContainer extends Comparable<DataContainer> {
    MapCodec<DataContainer> MAP_CODEC = DataType.CODEC.dispatchMap("type", DataContainer::type, DataType::codec);
    Codec<DataContainer> CODEC = MAP_CODEC.codec();

    List<String> GENERIC_EXTRACTS = List.of("decimal", "integer", "string", "boolean", "redstone");
    static DataContainer of(long count) {
        return new LongData(count);
    }
    static DataContainer of(boolean count) {
        return count ? BoolData.TRUE : BoolData.FALSE;
    }
    static DataContainer empty() {
        return EmptyData.INSTANCE;
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

    default DataContainer extract(String field) {
        return switch (field) {
            case "decimal" -> new DoubleData(asDouble());
            case "integer" -> new LongData(asLong());
            case "redstone" -> new LongData(asRedstoneOutput());
            case "string" -> new StringData(asString());
            case "boolean" -> BoolData.of(this.isTrue());
            case "" -> this;
            default -> empty();
        };
    }

    default boolean isTrue() {
        return this.asLong() != 0;
    }

    default int compareTo(DataContainer other) {
        return Long.compare(this.asLong(), other.asLong());
    }
}
