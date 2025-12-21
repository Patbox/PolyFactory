package eu.pb4.polyfactory.util.storage;

import com.google.common.collect.Iterators;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FilteredRedirectedSlottedStorage<T extends TransferVariant<?>> implements SlottedStorage<T>, RedirectingStorage {
    private final Supplier<Level> world;
    private final Supplier<BlockPos> pos;
    private final Supplier<Direction> direction;
    private final BlockApiLookup<Storage<T>, Direction> lookup;
    private final SlottedPredicate<T> predicate;
    private final int[] slotMap;
    private final T defaultValue;

    public FilteredRedirectedSlottedStorage(BlockApiLookup<Storage<T>, @Nullable Direction> lookup, Supplier<Level> world,
                                            Supplier<BlockPos> pos, Supplier<Direction> direction,
                                            T defaultValue,
                                            int[] slotMap,
                                            SlottedPredicate<T> predicate) {
        this.slotMap = slotMap;
        this.lookup = lookup;
        this.world = world;
        this.pos = pos;
        this.direction = direction;
        this.predicate = predicate;
        this.defaultValue = defaultValue;
    }

    @Nullable
    public SlottedStorage<T> getTargetStorage() {
        var dir = direction.get();
        var storage = this.lookup.find(world.get(), pos.get().relative(dir), dir.getOpposite());

        return storage instanceof RedirectingStorage ? null : (storage instanceof SlottedStorage<T> s ? s : null);
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        var out = 0L;

        for (var slot : getSlots()) {
            out += slot.insert(resource, maxAmount - out, transaction);
            if (out == maxAmount) {
                return out;
            }
        }

        return out;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        var out = 0L;

        for (var slot : getSlots()) {
            out += slot.extract(resource, maxAmount - out, transaction);
            if (out == maxAmount) {
                return out;
            }
        }

        return out;
    }

    @Override
    public @NotNull Iterator<StorageView<T>> iterator() {
        var storage = getTargetStorage();
        if (storage == null) {
            return Collections.emptyIterator();
        }
        var slots = new ArrayList<StorageView<T>>();

        for (int i = 0; i < this.slotMap.length; i++) {
            var x = this.slotMap[i];
            if (x == -1 || x >= storage.getSlotCount()) {
                continue;
            }

            var slot = storage.getSlot(x);
            if (this.predicate.test(i, slot.getResource())) {
                slots.add(slot);
            }
        }


        return slots.iterator();
    }


    @Override
    public int getSlotCount() {
        return this.slotMap.length;
    }

    @Override
    public SingleSlotStorage<T> getSlot(int i) {
        if (this.slotMap[i] == -1) {
            return new EmptyStorage<>(defaultValue);
        }

        var storage = this.getTargetStorage();

        if (storage == null || storage.getSlotCount() >= this.slotMap[i]) {
            return new EmptyStorage<>(defaultValue);
        }

        return new FilteredSingleSlotStorage<>(storage.getSlot(this.slotMap[i]), i, this.predicate);
    }


    public interface SlottedPredicate<T> {
        boolean test(int slot, T resource);
    }
}
