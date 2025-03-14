package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public record StringData(String value) implements DataContainer {
    public static MapCodec<StringData> TYPE_CODEC = Codec.STRING.xmap(StringData::new, StringData::value).fieldOf("value");

    public static final DataContainer EMPTY = new StringData("");

    public StringData(Identifier value) {
        this(value.toString());
    }

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
    public DataContainer extract(String field) {
        if (field.equals("length")) return new LongData(value.length());
        if (field.startsWith("charat:")) {
            try {
                var num = Integer.parseInt(field.substring("charat:".length()));

                if (num >= 0 && num < this.value.length()) {
                    return new StringData(Character.toString(this.value.charAt(num)));
                }
            } catch (Throwable ignored) {}
            return StringData.EMPTY;
        }
        if (field.startsWith("substring:")) {
            try {
                var num = field.substring("substring:".length()).split("\\.", 2);
                int start = Integer.parseInt(num[0]);
                int end = num.length == 2 ? Integer.parseInt(num[1]) : this.value.length();

                if (start >= 0 && end <= this.value.length()) {
                    return new StringData(this.value.substring(start, end));
                }
            } catch (Throwable ignored) {}
            return StringData.EMPTY;
        }
        return DataContainer.super.extract(field);
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return this.value.compareTo(o.asString());
    }
}