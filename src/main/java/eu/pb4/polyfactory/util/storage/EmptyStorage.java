package eu.pb4.polyfactory.util.storage;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public record EmptyStorage<T>(T resource) implements SingleSlotStorage<T> {
    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean isResourceBlank() {
        return true;
    }

    @Override
    public T getResource() {
        return null;
    }

    @Override
    public long getAmount() {
        return 0;
    }

    @Override
    public long getCapacity() {
        return 0;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<StorageView<T>> nonEmptyIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterable<StorageView<T>> nonEmptyViews() {
        return Collections::emptyIterator;
    }
}
