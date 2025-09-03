package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public record RedstoneData(int value) implements DataContainer {
    public static MapCodec<RedstoneData> TYPE_CODEC = Codec.INT.xmap(RedstoneData::new, RedstoneData::value).fieldOf("value");

    public static final RedstoneData ZERO = new RedstoneData(0);
    @Override
    public DataType<RedstoneData> type() {
        return DataType.REDSTONE;
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
    public float asProgress() {
        return this.value / 15f;
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