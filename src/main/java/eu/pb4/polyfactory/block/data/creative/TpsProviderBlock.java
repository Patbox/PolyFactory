package eu.pb4.polyfactory.block.data.creative;

import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.providers.DataProviderBlock;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.DoubleData;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TpsProviderBlock extends DataProviderBlock {
    public TpsProviderBlock(Settings settings) {
        super(settings, false);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleBlockTick(pos, this, 40);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        DataProvider.sendData(world, pos, new DoubleData(Math.min((double) TimeHelper.SECOND_IN_NANOS / world.getServer().getAverageNanosPerTick(), world.getServer().getTickManager().getTickRate())));
        world.scheduleBlockTick(pos, this, 40);
    }
}
