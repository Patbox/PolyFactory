package eu.pb4.polyfactory.block.electric;

import com.kneelawk.graphlib.graph.BlockNode;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.nodes.electric.CablePlateNode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CablePlateBlock extends NetworkBlock implements PolymerBlock {
    public static final DirectionProperty FACING = Properties.FACING;

    public CablePlateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new CablePlateNode(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide(), -1));

        if (state.isOf(this) && ctx.getPlayer() != null && !ctx.getPlayer().isSneaking() && state.get(FACING).getAxis() != ctx.getSide().getAxis()) {
            return state;
        }

        return this.getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.IRON_TRAPDOOR;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        var facing = state.get(FACING);
        if (facing.getAxis() == Direction.Axis.Y) {
            return Blocks.IRON_TRAPDOOR.getDefaultState().with(TrapdoorBlock.HALF, facing.getDirection() == Direction.AxisDirection.POSITIVE ? BlockHalf.BOTTOM : BlockHalf.TOP);
        }

        return Blocks.IRON_TRAPDOOR.getDefaultState().with(TrapdoorBlock.FACING, state.get(FACING)).with(TrapdoorBlock.OPEN, true);
    }
}
