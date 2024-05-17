package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;

public record DoubleData(double value) implements DataContainer {
    public static MapCodec<DoubleData> TYPE_CODEC = Codec.DOUBLE.xmap(DoubleData::new, DoubleData::value).fieldOf("value");

    @Override
    public DataType<DoubleData> type() {
        return DataType.DOUBLE;
    }

    @Override
    public String asString() {
        return "" + value;
    }

    @Override
    public long asLong() {
        return (long) value;
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
        return false;
    }
}