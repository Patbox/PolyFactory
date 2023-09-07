package eu.pb4.polyfactory.block.data;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public interface CableConnectable {
    boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir);

}
