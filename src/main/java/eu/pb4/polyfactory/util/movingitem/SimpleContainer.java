package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class SimpleContainer implements ContainerHolder {
    private static final AddedCallback NOOP_ADD = (a, b, c) -> {};
    private static final RemovedCallback NOOP_REMOVE = (a, b) -> {};
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

    public static void readArray(SimpleContainer[] containers, NbtList items, RegistryWrapper.WrapperLookup lookup) {
        var l = Math.min(containers.length, items.size());

        for (int i = 0; i < l; i++) {
            if (items.get(i) instanceof NbtCompound compound) {
                containers[i].readNbt(compound, lookup);
            }
        }
    }

    public static NbtElement writeArray(SimpleContainer[] containers, RegistryWrapper.WrapperLookup lookup) {
        var list = new NbtList();
        for (var cotnainer : containers) {
            list.add(cotnainer.writeNbt(lookup));
        }
        return list;
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

    public NbtElement writeNbt(RegistryWrapper.WrapperLookup lookup) {
        return this.movingItem == null ? new NbtCompound() : this.movingItem.get().encodeAllowEmpty(lookup);
    }

    public void readNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
        var itemStack = ItemStack.fromNbtOrEmpty(lookup, compound);

        if (itemStack == ItemStack.EMPTY) {
            clearContainer();
        } else {
            setContainer(new MovingItem(itemStack));
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
