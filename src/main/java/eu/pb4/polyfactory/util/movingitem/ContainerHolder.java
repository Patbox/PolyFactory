package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface ContainerHolder {
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
        return stack.getMaxCount();
    }

    default boolean pushNew(ItemStack stack) {
        if (!isContainerEmpty()) {
            return false;
        }

        var i = Math.min(stack.getCount(), getMaxStackCount(stack));
        var moving = new MovingItem(stack.copyWithCount(i));
        stack.decrement(i);

        setContainer(moving);
        return true;
    }
}
