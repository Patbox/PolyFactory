package eu.pb4.polyfactory.block.fluids;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.property.LazyEnumProperty;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeBlock extends NetworkBlock implements FactoryBlock, PipeConnectable, BarrierBasedWaterloggable, BlockEntityProvider, NetworkComponent.Pipe {

    public static final LazyEnumProperty<TriState> NORTH = FactoryProperties.TRI_STATE_NORTH;
    public static final LazyEnumProperty<TriState> SOUTH = FactoryProperties.TRI_STATE_SOUTH;

    public static final LazyEnumProperty<TriState> EAST = FactoryProperties.TRI_STATE_EAST;
    public static final LazyEnumProperty<TriState> WEST = FactoryProperties.TRI_STATE_WEST;
    public static final LazyEnumProperty<TriState> UP = FactoryProperties.TRI_STATE_UP;
    public static final LazyEnumProperty<TriState> DOWN = FactoryProperties.TRI_STATE_DOWN;
    public static final Map<Direction, LazyEnumProperty<TriState>> FACING_PROPERTIES = FactoryProperties.TRI_STATE_DIRECTIONS;

    public PipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, TriState.FALSE).with(SOUTH, TriState.FALSE)
                .with(EAST, TriState.FALSE).with(WEST, TriState.FALSE).with(UP, TriState.FALSE).with(DOWN, TriState.FALSE).with(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = this.getDefaultState();
        int i = 0;
        Direction lastDir = null;
        for (var direction : Direction.values()) {
            var neighborPos = ctx.getBlockPos().offset(direction);
            if (canConnectTo(ctx.getWorld(), neighborPos, ctx.getWorld().getBlockState(neighborPos), direction.getOpposite())) {
                state = state.with(FACING_PROPERTIES.get(direction), TriState.TRUE);
                i++;
                lastDir = direction;
            }
        }
        if (i == 0) {
            state = state.with(FACING_PROPERTIES.get(ctx.getSide()), TriState.DEFAULT);
            state = state.with(FACING_PROPERTIES.get(ctx.getSide().getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.with(FACING_PROPERTIES.get(lastDir.getOpposite()), TriState.DEFAULT);
        }

        return waterLog(ctx, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        NetworkComponent.Pipe.updatePipeAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Pipe;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        var i = 0;

        Direction lastKnown = null;

        state = state.with(FACING_PROPERTIES.get(direction), TriState.of(canConnectTo(world, neighborPos, neighborState, direction.getOpposite())));
        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir)).orElse(false)) {
                i++;
                lastKnown = dir;
            } else {
                state = state.with(FACING_PROPERTIES.get(dir), TriState.FALSE);
            }
        }

        if (i == 0) {
            state = state.with(FACING_PROPERTIES.get(direction), TriState.DEFAULT);
            state = state.with(FACING_PROPERTIES.get(direction.getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.with(FACING_PROPERTIES.get(lastKnown.getOpposite()), TriState.DEFAULT);
        }

        return state;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        /*NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runFlows(pos, () -> true, ((direction, strength) -> {
            player.sendMessage(Text.literal(direction.asString() + "=" + strength));
        }));*/

        return super.onUse(state, world, pos, player, hit);
    }

    protected boolean canConnectTo(WorldAccess world, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof PipeConnectable connectable && connectable.canPipeConnect(world, neighborPos, neighborState, direction);
    }

    public static EnumSet<Direction> getDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir)).orElse(true)) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new PipeModel(initialBlockState);
    }

    private static boolean checkModelDirection(BlockState state, Direction direction) {
        return state.get(FACING_PROPERTIES.get(direction)).orElse(true);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PipeBlockEntity::tick;
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(getDirections(state)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }

    public static class PipeModel extends BlockModel {
        private final ItemDisplayElement pipe;
        private BlockState state;

        public PipeModel(BlockState state) {
            this.pipe = ItemDisplayElementUtil.createSimple();
            this.pipe.setViewRange(0.5f);
            this.pipe.setYaw(180);
            this.state = state;
            updateModel();
            this.addElement(this.pipe);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        protected void setState(BlockState blockState) {
            this.state = blockState;
            updateModel();
        }

        protected void updateModel() {
            this.pipe.setItem(FactoryModels.PIPE.get(this.state, PipeBlock::checkModelDirection));

            if (this.pipe.getHolder() == this) {
                this.pipe.tick();
            }
        }
    }
}
