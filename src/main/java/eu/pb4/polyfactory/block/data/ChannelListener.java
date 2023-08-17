package eu.pb4.polyfactory.block.data;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface ChannelListener {
    int channel(ServerWorld world, BlockPos pos, BlockState state);
    void setChannel(ServerWorld world, BlockPos pos, BlockState state, int i);
}
