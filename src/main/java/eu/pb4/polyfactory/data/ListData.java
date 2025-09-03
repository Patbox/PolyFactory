package eu.pb4.polyfactory.data;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public record ListData(List<DataContainer> list) implements DataContainer {
    public static MapCodec<ListData> TYPE_CODEC = DataContainer.CODEC.listOf().xmap(ListData::new, ListData::list).fieldOf("list");
    @Override
    public DataType<ListData> type() {
        return DataType.LIST;
    }

    @Override
    public String asString() {
        var b = new StringBuilder();
        b.append("[ ");
        list.forEach(v -> b.append(v.asString()).append(' '));
        b.append(']');
        return b.toString();
    }

    @Override
    public long asLong() {
        return this.list.size();
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public DataContainer extract(String field) {
        if (field.equals("size")) {
            return new LongData(this.list.size());
        }
        if (field.startsWith("key:")) {
            var num = field.substring("key:".length());
            try {
                var index = Integer.parseInt(num);
                if (index >= 0 && index < this.list.size()) {
                    return this.list.get(index);
                }
            } catch (Throwable e) {
                // Ignored
            }
        }

        return DataContainer.super.extract(field);
    }


    @Override
    public int compareTo(@NotNull DataContainer o) {
        return asString().compareTo(o.asString());
    }

    public void forEachFlat(Consumer<DataContainer> consumer) {
        for (var e : this.list) {
            if (e instanceof ListData listData) {
                listData.forEachFlat(consumer);
            } else {
                consumer.accept(e);
            }
        }
    }

    public boolean forEachFlatBool(Predicate<DataContainer> consumer) {
        var out = false;
        for (var e : this.list) {
            if (e instanceof ListData listData) {
                out |= listData.forEachFlatBool(consumer);
            } else {
                out |= consumer.test(e);
            }
        }
        return out;
    }

}
