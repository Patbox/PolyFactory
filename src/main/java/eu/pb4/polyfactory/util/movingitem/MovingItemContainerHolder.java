package eu.pb4.polyfactory.util.movingitem;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

public interface MovingItemContainerHolder {
    @Nullable
    MovingItem getContainer();

    void setContainer(@Nullable MovingItem container);

    default void clearContainer() {
        setContainer(null);
    }

    @Nullable
    MovingItem pullAndRemove();

    void pushAndAttach(MovingItem container);

    default boolean isContainerEmpty() {
        return getContainer() == null || getContainer().get().isEmpty();
    }

    default double movementDelta() {
        return 1;
    }

    default void setMovementPosition(double pos) {
    }

    default int getMaxStackCount(ItemStack stack) {
        return stack.getMaxStackSize();
    }

    default boolean pushNew(ItemStack stack) {
        if (!isContainerEmpty()) {
            return false;
        }

        var i = Math.min(stack.getCount(), getMaxStackCount(stack));
        var moving = new MovingItem(stack.copyWithCount(i));
        stack.shrink(i);

        setContainer(moving);
        return true;
    }

    default MovingItem pullAndDestroy() {
        var x = this.getContainer();
        this.clearContainer();
        return x;
    }
}
