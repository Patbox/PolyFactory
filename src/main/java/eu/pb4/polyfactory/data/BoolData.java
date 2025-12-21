package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

public record BoolData(boolean value) implements DataContainer {
    public static BoolData TRUE = new BoolData(true);
    public static BoolData FALSE = new BoolData(false);

    public static MapCodec<BoolData> TYPE_CODEC = Codec.BOOL.xmap(BoolData::of, BoolData::value).fieldOf("value");

    public static BoolData of(boolean count) {
        return count ? BoolData.TRUE : BoolData.FALSE;
    }

    @Override
    public DataType type() {
        return DataType.BOOL;
    }

    @Override
    public String asString() {
        return value ? "true" : "false";
    }

    @Override
    public long asLong() {
        return value ? 1 : 0;
    }

    @Override
    public int asRedstoneOutput() {
        return value ? 15 : 0;
    }

    @Override
    public double asDouble() {
        return value ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isTrue() {
        return this.value;
    }
}
