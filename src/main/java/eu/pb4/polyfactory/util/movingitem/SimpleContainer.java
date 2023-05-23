package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.polyfactory.block.machines.PressBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SimpleContainer implements ContainerHolder {
    private static final BiConsumer<MovingItem, Boolean> NOOP = (a, b) -> {};

    @Nullable
    private MovingItem movingItem;
    private BiConsumer<MovingItem, Boolean> added;
    private BiConsumer<MovingItem, Boolean> removed;

    public SimpleContainer() {
        this(NOOP, NOOP);
    }
    public SimpleContainer(BiConsumer<MovingItem, Boolean> added, BiConsumer<MovingItem, Boolean> removed) {
        this.added = added;
        this.removed = removed;
    }

    public static void readArray(SimpleContainer[] containers, NbtList items) {
        var l = Math.min(containers.length, items.size());

        for (int i = 0; i < l; i++) {
            if (items.get(i) instanceof NbtCompound compound) {
                containers[i].readNbt(compound);
            }
        }
    }

    public static NbtElement writeArray(SimpleContainer[] containers) {
        var list = new NbtList();
        for (var cotnainer : containers) {
            list.add(cotnainer.writeNbt());
        }
        return list;
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
            this.added.accept(container, true);
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
        this.added.accept(container, false);
    }

    public NbtCompound writeNbt() {
        return this.movingItem == null ? new NbtCompound() : this.movingItem.get().writeNbt(new NbtCompound());
    }

    public void readNbt(NbtCompound compound) {
        var itemStack = ItemStack.fromNbt(compound);

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
}
