package eu.pb4.polyfactory.block.network;

import eu.pb4.polyfactory.block.base.AxisAndFacingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class AxisAndFacingNetworkBlock extends AxisAndFacingBlock implements NetworkComponent {
    public AxisAndFacingNetworkBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if ((isSameNetworkType(neighborState.getBlock()) || neighborState.isAir())) {
            updateNetworkAt(world, pos);
        }

        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        updateNetworkAt(world, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        this.updateNetworkAt(world, pos);
    }

    protected abstract void updateNetworkAt(LevelReader world, BlockPos pos);
    protected abstract boolean isSameNetworkType(Block block);
}
