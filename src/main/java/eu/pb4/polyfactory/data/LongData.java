package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;

public record LongData(long value) implements DataContainer {
    public static MapCodec<LongData> TYPE_CODEC = Codec.LONG.xmap(LongData::new, LongData::value).fieldOf("value");

    public static final LongData ZERO = new LongData(0);
    @Override
    public DataType<LongData> type() {
        return DataType.LONG;
    }

    @Override
    public String asString() {
        return "" + value;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public char padding() {
        return '0';
    }

    @Override
    public boolean forceRight() {
        return true;
    }

    @Override
    public int compareTo(DataContainer other) {
        return Long.compare(this.asLong(), other.asLong());
    }
}