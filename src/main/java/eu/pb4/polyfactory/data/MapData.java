package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record MapData(Map<String, DataContainer> map) implements DataContainer {
    public static MapCodec<MapData> TYPE_CODEC = Codec.unboundedMap(Codec.STRING, DataContainer.CODEC).xmap(MapData::new, MapData::map).fieldOf("map");
    @Override
    public DataType<MapData> type() {
        return DataType.MAP;
    }

    @Override
    public String asString() {
        var b = new StringBuilder();
        b.append("[ ");
        map.forEach((k,v) -> b.append(k).append("->").append(v.asString()).append(' '));
        b.append(']');
        return b.toString();
    }

    @Override
    public long asLong() {
        return this.map.size();
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public DataContainer extract(String field) {
        if (field.equals("size")) {
            return new LongData(this.map.size());
        }
        if (field.startsWith("key:")) {
            return this.map.getOrDefault(field.substring("key:".length()), DataContainer.empty());
        }
        if (field.startsWith("exist:")) {
            return BoolData.of(this.map.containsKey(field.substring("exists:".length())));
        }

        return DataContainer.super.extract(field);
    }


    @Override
    public int compareTo(@NotNull DataContainer o) {
        return asString().compareTo(o.asString());
    }
}
