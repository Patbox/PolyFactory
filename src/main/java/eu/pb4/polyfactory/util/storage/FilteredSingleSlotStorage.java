package eu.pb4.polyfactory.util.storage;

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.function.Predicate;

public record FilteredSingleSlotStorage<T>(SingleSlotStorage<T> storage, int slot, FilteredRedirectedSlottedStorage.SlottedPredicate<T> predicate) implements SingleSlotStorage<T>, RedirectingStorage {
    @Override
    public long insert(T t, long l, TransactionContext transactionContext) {
        return this.predicate.test(slot, t) ? this.storage.insert(t, l, transactionContext) : 0;
    }

    @Override
    public long extract(T t, long l, TransactionContext transactionContext) {
        return this.predicate.test(slot, t) ? this.storage.extract(t, l, transactionContext) : 0;
    }

    @Override
    public boolean isResourceBlank() {
        return this.storage.isResourceBlank();
    }

    @Override
    public T getResource() {
        return this.storage.getResource();
    }

    @Override
    public long getAmount() {
        return this.storage.getAmount();
    }

    @Override
    public long getCapacity() {
        return this.storage.getCapacity();
    }
}
