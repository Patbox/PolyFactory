package eu.pb4.polyfactory.util.storage;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Iterator;
import java.util.List;

public interface WrappingStorage<T> extends Storage<T> {
    Storage<T> storage();

    @Override
    default boolean supportsInsertion() {
        return this.storage().supportsInsertion();
    }

    @Override
    default long insert(T resource, long maxAmount, TransactionContext transaction) {
        return this.storage().insert(resource, maxAmount, transaction);
    }

    @Override
    default boolean supportsExtraction() {
        return this.storage().supportsExtraction();
    }

    @Override
    default long extract(T resource, long maxAmount, TransactionContext transaction) {
        return this.storage().extract(resource, maxAmount, transaction);
    }

    @Override
    default Iterator<StorageView<T>> iterator() {
        return this.storage().iterator();
    }

    @Override
    default Iterator<StorageView<T>> nonEmptyIterator() {
        return this.storage().nonEmptyIterator();
    }

    @Override
    default Iterable<StorageView<T>> nonEmptyViews() {
        return this.storage().nonEmptyViews();
    }

    @Override
    default long getVersion() {
        return this.storage().getVersion();
    }

    interface Slotted<T> extends WrappingStorage<T>, SlottedStorage<T> {
        @Override
        SlottedStorage<T> storage();

        @Override
        default int getSlotCount() {
            return this.storage().getSlotCount();
        }

        @Override
        default SingleSlotStorage<T> getSlot(int slot) {
            return this.storage().getSlot(slot);
        }

        @Override
        default @UnmodifiableView List<SingleSlotStorage<T>> getSlots() {
            return this.storage().getSlots();
        }
    }

    @Nullable
    static <T> Storage<T> withModifyCallback(@Nullable Storage<T> storage, Runnable onInsertOrExtract) {
        if (storage == null) {
            return null;
        }

        if (storage instanceof SlottedStorage<T> slottedStorage) {
            return new Slotted<T>() {
                @Override
                public SlottedStorage<T> storage() {
                    return slottedStorage;
                }

                @Override
                public long insert(T resource, long maxAmount, TransactionContext transaction) {
                    var i = Slotted.super.insert(resource, maxAmount, transaction);
                    if (i > 0) {
                        onInsertOrExtract.run();
                    }
                    return i;
                }

                @Override
                public long extract(T resource, long maxAmount, TransactionContext transaction) {
                    var i = Slotted.super.extract(resource, maxAmount, transaction);
                    if (i > 0) {
                        onInsertOrExtract.run();
                    }
                    return i;
                }
            };
        }
        return new WrappingStorage<T>() {
            @Override
            public Storage<T> storage() {
                return storage;
            }

            @Override
            public long insert(T resource, long maxAmount, TransactionContext transaction) {
                var i = WrappingStorage.super.insert(resource, maxAmount, transaction);
                if (i > 0) {
                    onInsertOrExtract.run();
                }
                return i;
            }

            @Override
            public long extract(T resource, long maxAmount, TransactionContext transaction) {
                var i = WrappingStorage.super.extract(resource, maxAmount, transaction);
                if (i > 0) {
                    onInsertOrExtract.run();
                }
                return i;
            }
        };
    }
}
