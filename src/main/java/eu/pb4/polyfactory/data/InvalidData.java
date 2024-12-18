package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

public record InvalidData(String error) implements DataContainer {
    public static MapCodec<InvalidData> TYPE_CODEC = Codec.STRING.xmap(InvalidData::new, InvalidData::error).fieldOf("value");

    @Override
    public DataType<InvalidData> type() {
        return DataType.INVALID;
    }

    @Override
    public String asString() {
        return error;
    }

    @Override
    public long asLong() {
        return 0;
    }

    @Override
    public double asDouble() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return this.error.compareTo(o.asString());
    }
}