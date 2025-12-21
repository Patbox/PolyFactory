package eu.pb4.polyfactory.block.property;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.state.properties.Property;

public class LazyEnumProperty<T extends Enum<T>> extends Property<T> {
    private final List<T> values;
    private final Map<String, T> byName = Maps.newHashMap();
    private final Map<T, String> toName;

    protected LazyEnumProperty(String name, Class<T> type, Collection<T> values) {
        super(name, type);
        this.values = List.copyOf(values);
        this.toName = new EnumMap<>(type);
        for (var enum_ : values) {
            var string = enum_.name().toLowerCase(Locale.ROOT);
            if (this.byName.containsKey(string)) {
                throw new IllegalArgumentException("Multiple values have the same name '" + string + "'");
            }

            this.byName.put(string, enum_);
            this.toName.put(enum_, string);
        }

    }

    @Override
    public List<T> getPossibleValues() {
        return this.values;
    }

    @Override
    public Optional<T> getValue(String name) {
        return Optional.ofNullable(this.byName.get(name));
    }

    @Override
    public int getInternalIndex(T value) {
        return this.values.indexOf(value);
    }

    @Override
    public String getName(T enum_) {
        return this.toName.get(enum_);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            if (object instanceof LazyEnumProperty<?> enumProperty) {
                if (super.equals(object)) {
                    return this.values.equals(enumProperty.values) && this.byName.equals(enumProperty.byName);
                }
            }

            return false;
        }
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        i = 31 * i + this.values.hashCode();
        i = 31 * i + this.byName.hashCode();
        return i;
    }

    public static <T extends Enum<T>> LazyEnumProperty<T> of(String name, Class<T> type) {
        return of(name, type, (enum_) -> {
            return true;
        });
    }

    public static <T extends Enum<T>> LazyEnumProperty<T> of(String name, Class<T> type, Predicate<T> filter) {
        return of(name, type, Arrays.stream(type.getEnumConstants()).filter(filter).collect(Collectors.toList()));
    }
    public static <T extends Enum<T>> LazyEnumProperty<T> of(String name, Class<T> type, T... values) {
        return of(name, type, Lists.newArrayList(values));
    }

    public static <T extends Enum<T>> LazyEnumProperty<T> of(String name, Class<T> type, Collection<T> values) {
        return new LazyEnumProperty<>(name, type, values);
    }
}
