package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FluidContainerImpl implements FluidContainer {
    private final Object2LongMap<FluidInstance<?>> storedFluids = new Object2LongOpenHashMap<>();
    private final List<FluidInstance<?>> fluids = new ArrayList<>();
    private final long capacity;
    private final Runnable markDirty;
    private final BiPredicate<FluidContainer, FluidInstance<?>> canInsert;
    private int updateId = 0;
    private long stored = 0;
    private float fluidTemperature;

    public FluidContainerImpl(long maxStorage, Runnable markDirty) {
        this(maxStorage, markDirty, (a, x) -> true);
    }

    public FluidContainerImpl(long maxStorage, Runnable markDirty, BiPredicate<FluidContainer, FluidInstance<?>> canInsert) {
        this.capacity = maxStorage;
        this.canInsert = canInsert;
        this.markDirty = markDirty;
    }

    public static FluidContainerImpl singleFluid(long maxStorage, Runnable markDirty) {
        return new FluidContainerImpl(maxStorage, markDirty, (self, type) -> self.isEmpty() || self.contains(type));
    }

    public static FluidContainerImpl filtered(long maxStorage, Predicate<FluidInstance<?>> filter, Runnable markDirty) {
        return new FluidContainerImpl(maxStorage, markDirty, (self, type) -> filter.test(type));
    }

    public static FluidContainer onlyInTag(long maxStorage, TagKey<FluidType<?>> tag, Runnable markDirty) {
        return new FluidContainerImpl(maxStorage, markDirty, (self, type) -> type.isIn(tag));
    }

    @Override
    public long get(FluidInstance<?> type) {
        return this.storedFluids.getOrDefault(type, 0);
    }

    @Override
    public long set(FluidInstance<?> type, long amount) {
        if (amount == 0) {
            this.fluids.remove(type);
            var current = this.storedFluids.removeLong(amount);
            this.stored -= current;
            this.updateTemperature();
            this.updateId++;
            this.markDirty.run();
            return current;
        }
        var current = this.storedFluids.put(type, amount);
        this.stored += amount - current;
        if (current == 0) {
            this.fluids.add(type);
            this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        }
        this.updateTemperature();
        this.updateId++;
        this.markDirty.run();
        return current;
    }

    @Override
    public boolean canInsert(FluidInstance<?> type, long amount, boolean exact) {
        return this.canInsert.test(this, type) && (exact ? this.stored + amount <= this.capacity : this.stored != this.capacity);
    }

    @Override
    public long insert(FluidInstance<?> type, long amount, boolean exact) {
        if (!canInsert(type, amount, exact) || amount == 0) {
            return amount;
        }

        var next = Math.min(this.stored + amount, this.capacity);
        var inserted = next - this.stored;

        var current = this.storedFluids.getOrDefault(type, 0);
        this.storedFluids.put(type, current + inserted);
        if (current == 0) {
            this.fluids.add(type);
            this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        }
        this.stored = next;
        this.updateId++;
        this.updateTemperature();
        this.markDirty.run();
        return amount - inserted;
    }

    @Override
    public boolean canExtract(FluidInstance<?> type, long amount, boolean exact) {
        return exact ? this.get(type) >= amount : this.get(type) != 0;
    }

    @Override
    public long extract(FluidInstance<?> type, long amount, boolean exact) {
        if (!canExtract(type, amount, exact) || amount == 0) {
            return 0;
        }

        var current = this.get(type);
        var extracted = Math.min(current, amount);

        if (current == extracted) {
            this.storedFluids.removeLong(type);
            this.fluids.remove(type);
        } else {
            this.storedFluids.put(type, current - extracted);
        }
        this.stored -= extracted;
        this.updateId++;
        this.updateTemperature();
        this.markDirty.run();
        return extracted;
    }

    @Override
    @Nullable
    public FluidInstance<?> topFluid() {
        return this.fluids.isEmpty() ? null : this.fluids.getLast();
    }

    @Override
    @Nullable
    public FluidInstance<?> bottomFluid() {
        return this.fluids.isEmpty() ? null : this.fluids.getFirst();
    }

    @Override
    public void forEach(BiConsumer<FluidInstance<?>, Long> consumer) {
        for (var f : this.fluids) {
            consumer.accept(f, this.storedFluids.getOrDefault(f, 0));
        }
    }

    @Override
    public void forEachReversed(BiConsumer<FluidInstance<?>, Long> consumer) {
        for (var f : this.fluids.reversed()) {
            consumer.accept(f, this.storedFluids.getOrDefault(f, 0));
        }
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public long stored() {
        return this.stored;
    }

    public float fluidTemperature() {
        return this.fluidTemperature;
    }

    public void writeData(ValueOutput view, String key) {
        var out = view.childrenList(key);
        storedFluids.forEach((a, b) -> {
            var x = out.addChild();
            x.store(FluidInstance.MAP_CODEC, a);
            x.putLong("amount", b);
        });
    }

    public void readData(ValueInput view, String fluidKey) {
        var nbt = view.childrenListOrEmpty(fluidKey);
        this.storedFluids.clear();
        this.fluids.clear();
        this.stored = 0;
        for (var t : nbt) {
            var type = t.read(FluidInstance.MAP_CODEC);
            if (type.isPresent()) {
                var value = t.getLongOr("amount", 0);
                if (value != 0) {
                    this.storedFluids.put(type.get(), value);
                    this.stored += value;
                }
            }
        }
        this.updateTemperature();
        this.fluids.addAll(this.storedFluids.keySet());
        this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        this.updateId++;
    }

    private void updateTemperature() {
        float temperature = 0;
        for (var fluid : fluids) {
            temperature += (fluid.heat() * get(fluid)) / this.stored;
        }
        this.fluidTemperature = temperature;
    }

    public int updateId() {
        return this.updateId;
    }

    @Override
    public Object2LongMap<FluidInstance<?>> asMap() {
        return Object2LongMaps.unmodifiable(this.storedFluids);
    }

    @Override
    public List<FluidInstance<?>> fluids() {
        return Collections.unmodifiableList(this.fluids);
    }

    @Override
    public void clear() {
        this.storedFluids.clear();
        this.fluids.clear();
        this.stored = 0;
        this.markDirty.run();
    }
}
