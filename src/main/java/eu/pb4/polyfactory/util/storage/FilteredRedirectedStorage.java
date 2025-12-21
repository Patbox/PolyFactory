package eu.pb4.polyfactory.util.storage;

import com.google.common.collect.Iterators;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FilteredRedirectedStorage<T extends TransferVariant<?>> implements Storage<T>, RedirectingStorage {
    private final Supplier<Level> world;
    private final Supplier<BlockPos> pos;
    private final Supplier<Direction> direction;
    private final BlockApiLookup<Storage<T>, Direction> lookup;
    private final Predicate<T> predicate;

    public FilteredRedirectedStorage(BlockApiLookup<Storage<T>, @Nullable Direction> lookup, Supplier<Level> world, Supplier<BlockPos> pos, Supplier<Direction> direction, Predicate<T> predicate) {
        this.lookup = lookup;
        this.world = world;
        this.pos = pos;
        this.direction = direction;
        this.predicate = predicate;
    }

    @Nullable
    public Storage<T> getTargetStorage() {
        var dir = direction.get();
        var storage = this.lookup.find(world.get(), pos.get().relative(dir), dir.getOpposite());

        return storage instanceof RedirectingStorage ? null : storage;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        var storage = getTargetStorage();
        if (storage == null || !this.predicate.test(resource)) {
            return 0;
        }
        return storage.insert(resource, maxAmount, transaction);
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        var storage = getTargetStorage();
        if (storage == null || !this.predicate.test(resource)) {
            return 0;
        }
        return storage.extract(resource, maxAmount, transaction);
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        var storage = getTargetStorage();
        if (storage == null) {
            return Collections.emptyIterator();
        }
        return Iterators.filter(storage.iterator(), this::checkView) ;
    }

    private boolean checkView(StorageView<T> tStorageView) {
        return this.predicate.test(tStorageView.getResource());
    }
}
