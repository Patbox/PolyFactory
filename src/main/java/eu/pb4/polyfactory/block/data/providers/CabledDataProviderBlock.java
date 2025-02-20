package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelProviderSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CabledDataProviderBlock extends GenericCabledDataBlock implements DataProvider {
    public CabledDataProviderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }
}
