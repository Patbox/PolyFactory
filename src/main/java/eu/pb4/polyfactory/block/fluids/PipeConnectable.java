package eu.pb4.polyfactory.block.fluids;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public interface PipeConnectable {
    boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir);

}
