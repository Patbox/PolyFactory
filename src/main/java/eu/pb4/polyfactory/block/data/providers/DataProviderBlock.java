package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.util.DataCacheBlockEntity;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DataProviderBlock extends GenericDirectionalDataBlock implements DataProvider {
    public DataProviderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel) {
        if (world.getBlockEntity(selfPos) instanceof DataCacheBlockEntity be && be.channel() == channel) {
            return be.lastData;
        }
        return null;
    }

    public void sendData(ServerWorld world, BlockPos selfPos, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof DataCacheBlockEntity be) {
            be.lastData = data;
            NetworkComponent.Data.getLogic(world, selfPos).pushDataUpdate(be.channel(), data);

        }
    }
}
