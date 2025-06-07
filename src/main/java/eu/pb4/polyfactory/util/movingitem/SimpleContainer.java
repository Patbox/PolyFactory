package eu.pb4.polyfactory.util.movingitem;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SimpleContainer implements ContainerHolder {
    private static final AddedCallback NOOP_ADD = (a, b, c) -> {
    };
    private static final RemovedCallback NOOP_REMOVE = (a, b) -> {
    };
    private final int id;

    @Nullable
    private MovingItem movingItem;
    private AddedCallback added;
    private RemovedCallback removed;

    public SimpleContainer() {
        this(-1, NOOP_ADD, NOOP_REMOVE);
    }

    public SimpleContainer(int id, AddedCallback added, RemovedCallback removed) {
        this.id = id;
        this.added = added;
        this.removed = removed;
    }

    public static void readArray(SimpleContainer[] containers, ReadView.ListReadView listView) {
        if (listView.isEmpty()) {
            for (var x : containers) {
                x.clearContainer();
            }
        } else {
            int i = 0;
            for (var view : listView) {
                containers[i].readData(view);
                if (i++ >= containers.length) {
                    break;
                }
            }
        }


    }

    public static void writeArray(SimpleContainer[] containers, WriteView.ListAppender<ItemStack> appender) {
        for (var container : containers) {
            appender.add(container.getStack());
        }
    }

    public void writeData(WriteView view) {
        if (this.movingItem != null && !this.movingItem.get().isEmpty()) {
            view.put(ItemStack.MAP_CODEC, this.getStack());
        }
    }

    public void readData(ReadView view) {
        if (view.getString("id", "").isEmpty()) {
            this.clearContainer();
        } else {
            var stack = view.read(ItemStack.MAP_CODEC);
            if (stack.isEmpty()) {
                this.clearContainer();
                return;
            }
            if (this.movingItem != null) {
                this.movingItem.set(stack.get());
            } else {
                this.setContainer(new MovingItem(stack.get()));
            }
        }
    }


    public static SimpleContainer[] createArray(int size, AddedCallback added, RemovedCallback removed) {
        var arr = new SimpleContainer[size];

        for (int i = 0; i < size; i++) {
            arr[i] = new SimpleContainer(i, added, removed);
        }
        return arr;
    }

    @Override
    public @Nullable MovingItem getContainer() {
        return this.movingItem;
    }

    @Override
    public void setContainer(@Nullable MovingItem container) {
        if (container == null) {
            this.removed.accept(this.movingItem, true);
            this.movingItem = null;
        } else {
            if (this.movingItem != null) {
                this.removed.accept(this.movingItem, true);
            }

            this.movingItem = container;
            this.added.accept(this.id, container, true);
        }
    }

    @Override
    public @Nullable MovingItem pullAndRemove() {
        var x = this.movingItem;
        this.movingItem = null;
        this.removed.accept(x, false);
        return x;
    }

    @Override
    public void pushAndAttach(MovingItem container) {
        if (this.movingItem != null) {
            this.removed.accept(this.movingItem, true);
        }
        this.movingItem = container;
        if (container != null) {
            this.added.accept(this.id, container, false);
        } else {
            this.setContainer(null);
        }
    }

    public void maybeAdd(ElementHolder model) {
        if (!this.isContainerEmpty()) {
            model.addElement(this.getContainer());
        }
    }

    public ItemStack getStack() {
        if (this.movingItem != null) {
            return this.movingItem.get();
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    public Vec3d getPos() {
        if (this.movingItem != null) {
            return this.movingItem.getCurrentPos();
        }
        return null;
    }

    public void tick() {
        if (this.movingItem != null) {
            this.movingItem.tick();
        }
    }

    public interface AddedCallback {
        void accept(int i, MovingItem item, boolean newlyAdded);
    }

    public interface RemovedCallback {
        void accept(MovingItem item, boolean destroy);
    }
}
