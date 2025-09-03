package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

public record ProgressData(float value) implements DataContainer {
    public static MapCodec<ProgressData> TYPE_CODEC = Codec.FLOAT.xmap(ProgressData::new, ProgressData::value).fieldOf("value");

    @Override
    public DataType<ProgressData> type() {
        return DataType.PROGRESS;
    }

    @Override
    public String asString() {
        return value * 100 + "%";
    }

    @Override
    public long asLong() {
        return (long) asDouble();
    }

    @Override
    public double asDouble() {
        return value * 100;
    }

    @Override
    public float asProgress() {
        return this.value;
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return Double.compare(this.value, o.asDouble());
    }
}