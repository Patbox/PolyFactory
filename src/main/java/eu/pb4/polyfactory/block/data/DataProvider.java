package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public interface DataProvider {
    @Nullable
    DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node);

    static int sendData(WorldView world, BlockPos selfPos, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be) {
            be.setCachedData(data);
            return sendData(world, selfPos, be.channel(), data);
        }
        return 0;
    }

    static int sendData(WorldView world, BlockPos selfPos, int channel, DataContainer data) {
        return sendData(world, selfPos, channel, null, data);
    }

    static int sendData(WorldView world, BlockPos selfPos, int channel, @Nullable Direction direction, DataContainer data) {
        if (world instanceof ServerWorld serverWorld) {
            return NetworkComponent.Data.getLogic(serverWorld, selfPos).pushDataUpdate(selfPos, channel, data, direction);
        }
        return 0;
    }
}
