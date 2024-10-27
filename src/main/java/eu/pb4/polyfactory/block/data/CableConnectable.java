package eu.pb4.polyfactory.block.data;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public interface CableConnectable {
    boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir);

}
