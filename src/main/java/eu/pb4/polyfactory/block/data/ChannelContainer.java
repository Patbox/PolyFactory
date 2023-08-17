package eu.pb4.polyfactory.block.data;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface ChannelContainer {
    int channel();
    void setChannel();
}
