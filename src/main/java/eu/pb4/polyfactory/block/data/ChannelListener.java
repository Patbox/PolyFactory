package eu.pb4.polyfactory.block.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface ChannelListener {
    int channel(ServerLevel world, BlockPos pos, BlockState state);
    void setChannel(ServerLevel world, BlockPos pos, BlockState state, int i);
}
