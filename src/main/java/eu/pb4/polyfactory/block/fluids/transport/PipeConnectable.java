package eu.pb4.polyfactory.block.fluids.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface PipeConnectable {
    boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir);

}
