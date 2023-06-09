package eu.pb4.polyfactory.block.network;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public abstract class RotationalNetworkBlock extends Block implements NetworkComponent.Rotational {
    protected RotationalNetworkBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if ((neighborState.getBlock() instanceof NetworkComponent || neighborState.isAir())) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        this.onStateReplacedUpdateConnections(state, world, pos, newState, moved);
    }

    protected void onStateReplacedUpdateConnections(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
    }
}