package eu.pb4.polyfactory.block.network;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public abstract class NetworkBlock extends Block implements NetworkComponent {
    protected NetworkBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if ((isSameNetworkType(neighborState.getBlock()) || neighborState.isAir())) {
            updateNetworkAt(world, pos);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }


    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        updateNetworkAt(world, pos);
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        this.updateNetworkAt(world, pos);
    }

    protected abstract void updateNetworkAt(WorldAccess world, BlockPos pos);
    protected abstract boolean isSameNetworkType(Block block);

}