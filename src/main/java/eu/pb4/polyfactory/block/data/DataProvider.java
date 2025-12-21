package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface DataProvider {
    @Nullable
    DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node);

    static int sendData(@Nullable LevelReader world, BlockPos selfPos, DataContainer data) {
        if (world != null && world.getBlockEntity(selfPos) instanceof ChanneledDataCache be) {
            be.setCachedData(data);
            return sendData(world, selfPos, be.channel(), data);
        }
        return 0;
    }

    static int sendData(@Nullable LevelReader world, BlockPos selfPos, int channel, DataContainer data) {
        return sendData(world, selfPos, channel, null, data);
    }

    static int sendData(@Nullable LevelReader world, BlockPos selfPos, int channel, @Nullable Direction direction, DataContainer data) {
        if (world instanceof ServerLevel serverWorld) {
            return NetworkComponent.Data.getLogic(serverWorld, selfPos, x -> x.getNode() instanceof DataProviderNode).pushDataUpdate(selfPos, channel, data, direction);
        }
        return 0;
    }
}
