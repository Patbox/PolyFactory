package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.util.inventory.MergedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class ItemThrower {
    private final Level level;
    private final BlockPos pos;
    private final Direction throwDirection;
    private final Iterable<Direction> outputDirs;
    boolean locateContainer = true;
    Container container = null;

    public ItemThrower(Level level, BlockPos pos, Direction throwDirection, Iterable<Direction> outputDirs) {
        this.level = level;
        this.pos = pos;
        this.throwDirection = throwDirection;
        this.outputDirs = outputDirs;
    }

    public void drop(ItemStack stack) {
        var output = getContainer();

        if (output != null) {
            FactoryUtil.tryInsertingRegular(output, stack);
        }

        if (!stack.isEmpty()) {
            spawnItem(stack);
        }
    }

    public void dropContents(Container container) {
        var output = getContainer();
        for (var i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (output != null) {
                FactoryUtil.tryInsertingRegular(output, stack);
            }

            if (!stack.isEmpty()) {
                spawnItem(stack.copy());
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private void spawnItem(ItemStack stack) {
        double x = this.pos.getX() + 0.5 + throwDirection.getStepX() / 2f;
        double y = this.pos.getY() + 0.5 + throwDirection.getStepY() / 2f;
        double z = this.pos.getZ() + 0.5 + throwDirection.getStepZ() / 2f;

        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        double g = level.random.nextDouble() * 0.1 + 0.2;
        itemEntity.setDeltaMovement(
                level.random.triangle(throwDirection.getStepX() * g * 0.1, 0.0172275),
                level.random.triangle(0.2, 0.0172275),
                level.random.triangle(throwDirection.getStepZ() * g * 0.1, 0.0172275)
        );
        level.addFreshEntity(itemEntity);
    }

    private Container getContainer() {
        if (locateContainer) {
            var list = new ArrayList<Container>();
            ItemOutputBufferBlock.findBuffers(this.level, this.pos, this.outputDirs, list::add);
            locateContainer = false;

            this.container = list.isEmpty() ? null : new MergedContainer(list);
        }

        return this.container;
    }
}