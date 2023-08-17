package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.data.FactoryData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface DataReceiver {
    boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, FactoryData data);
}
