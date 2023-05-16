package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.util.MovingItemContainer;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface MovingItemConsumer {
    boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainer.Aware conveyor);
}
