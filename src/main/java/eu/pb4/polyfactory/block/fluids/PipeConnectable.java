package eu.pb4.polyfactory.block.fluids;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public interface PipeConnectable {
    boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir);

}
