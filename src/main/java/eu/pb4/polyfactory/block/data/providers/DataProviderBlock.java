package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DataProviderBlock extends GenericDirectionalDataBlock implements DataProvider {
    public DataProviderBlock(Settings settings, boolean oppositeFacing) {
        super(settings, oppositeFacing);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(oppositeFacing ? state.get(FACING).getOpposite() : state.get(FACING), getChannel(world, pos)));
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }
}
