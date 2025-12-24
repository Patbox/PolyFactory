package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface DataReceiver {
    boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir, int dataId);
}
