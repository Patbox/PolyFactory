package eu.pb4.polyfactory.block.fluids;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.property.LazyEnumProperty;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class PipeBaseBlock extends NetworkBlock implements FactoryBlock, PipeConnectable, BarrierBasedWaterloggable, BlockEntityProvider, NetworkComponent.Pipe {
    public PipeBaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        Pipe.updatePipeAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Pipe;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return checkModelDirection(state, dir);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new PipeModel(initialBlockState);
    }

    public abstract EnumSet<Direction> getDirections(BlockState state);

    public abstract boolean checkModelDirection(BlockState state, Direction direction);

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
            this.pipe.setItem(FactoryModels.PIPE.get(this.state, ((PipeBaseBlock) this.state.getBlock())::checkModelDirection));

            if (this.pipe.getHolder() == this) {
                this.pipe.tick();
            }
        }
    }
}
