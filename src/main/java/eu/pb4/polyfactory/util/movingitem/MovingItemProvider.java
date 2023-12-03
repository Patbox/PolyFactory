package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.factorytools.api.util.WorldPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface MovingItemProvider {
    void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor);
}
