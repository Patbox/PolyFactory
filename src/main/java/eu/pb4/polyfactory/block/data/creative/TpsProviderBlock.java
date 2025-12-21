package eu.pb4.polyfactory.block.data.creative;

import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.providers.DataProviderBlock;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.DoubleData;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TpsProviderBlock extends DataProviderBlock {
    public TpsProviderBlock(Properties settings) {
        super(settings, false);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleTick(pos, this, 40);
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        DataProvider.sendData(world, pos, new DoubleData(Math.min((double) TimeUtil.NANOSECONDS_PER_SECOND / world.getServer().getAverageTickTimeNanos(), world.getServer().tickRateManager().tickrate())));
        world.scheduleTick(pos, this, 40);
    }
}
