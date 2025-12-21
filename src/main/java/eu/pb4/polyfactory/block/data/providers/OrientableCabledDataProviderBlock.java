package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelProviderSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class OrientableCabledDataProviderBlock extends OrientableCabledDataBlock implements DataProvider {
    public OrientableCabledDataProviderBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelProviderSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public @Nullable DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }
}
