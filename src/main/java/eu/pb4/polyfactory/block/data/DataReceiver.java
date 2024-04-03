package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface DataReceiver {
    boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir);
}
