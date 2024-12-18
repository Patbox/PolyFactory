package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

public record StringData(String value) implements DataContainer {
    public static MapCodec<StringData> TYPE_CODEC = Codec.STRING.xmap(StringData::new, StringData::value).fieldOf("value");

    public static final DataContainer EMPTY = new StringData("");

    public static StringData ofLimited(String s) {
        return new StringData(s.substring(0, Math.min(s.length(), 512)));
    }

    @Override
    public DataType<StringData> type() {
        return DataType.STRING;
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public long asLong() {
        return value.length();
    }

    @Override
    public double asDouble() {
        return value.length();
    }

    @Override
    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    @Override
    public boolean forceRight() {
        return false;
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return this.value.compareTo(o.asString());
    }
}