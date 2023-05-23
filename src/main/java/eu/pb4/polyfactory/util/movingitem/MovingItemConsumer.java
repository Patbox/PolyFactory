package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface MovingItemConsumer {
    boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor);
}
