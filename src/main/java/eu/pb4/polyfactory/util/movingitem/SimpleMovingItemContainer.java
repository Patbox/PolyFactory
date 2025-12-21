package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SimpleMovingItemContainer implements MovingItemContainerHolder {
    private static final AddedCallback NOOP_ADD = (a, b, c) -> {
    };
    private static final RemovedCallback NOOP_REMOVE = (a, b) -> {
    };
    private final int id;

    @Nullable
    private MovingItem movingItem;
    private AddedCallback added;
    private RemovedCallback removed;

    public SimpleMovingItemContainer() {
        this(-1, NOOP_ADD, NOOP_REMOVE);
    }

    public SimpleMovingItemContainer(int id, AddedCallback added, RemovedCallback removed) {
        this.id = id;
        this.added = added;
        this.removed = removed;
    }

    public static void readArray(SimpleMovingItemContainer[] containers, ValueInput.ValueInputList listView) {
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

    public static void writeArray(SimpleMovingItemContainer[] containers, ValueOutput.TypedOutputList<ItemStack> appender) {
        for (var container : containers) {
            appender.add(container.getStack());
        }
    }

    public void writeData(ValueOutput view) {
        if (this.movingItem != null && !this.movingItem.get().isEmpty()) {
            view.store(ItemStack.MAP_CODEC, this.getStack());
        }
    }

    public void readData(ValueInput view) {
        if (view.getStringOr("id", "").isEmpty()) {
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


    public static SimpleMovingItemContainer[] createArray(int size, AddedCallback added, RemovedCallback removed) {
        var arr = new SimpleMovingItemContainer[size];

        for (int i = 0; i < size; i++) {
            arr[i] = new SimpleMovingItemContainer(i, added, removed);
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
    public Vec3 getPos() {
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
