package eu.pb4.polyfactory.block.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface CableConnectable {
    boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir);

}
