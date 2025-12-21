package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class DataProviderBlock extends GenericDirectionalDataBlock implements DataProvider {
    public DataProviderBlock(Properties settings, boolean oppositeFacing) {
        super(settings, oppositeFacing);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(oppositeFacing ? state.getValue(FACING).getOpposite() : state.getValue(FACING), getChannel(world, pos)));
    }

    @Override
    public @Nullable DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }
}
