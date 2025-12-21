package eu.pb4.polyfactory.block.fluids.transport;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import static eu.pb4.polyfactory.ModInit.id;

public class FilteredPipeBlock extends NetworkBlock implements FactoryBlock, ConfigurableBlock, PipeConnectable, BarrierBasedWaterloggable, EntityBlock, NetworkComponent.Pipe {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public FilteredPipeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(INVERTED, false));
        Model.NEGATED.isEmpty();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FilledStateProvider be) {
            return (int) ((be.getFilledAmount() * 15) / be.getFillCapacity());
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, INVERTED, WATERLOGGED);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        Pipe.updatePipeAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Pipe;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        var func = FluidBehaviours.ITEM_TO_FLUID.get(stack.getItem());
        if (func != null && world.getBlockEntity(pos) instanceof FilteredPipeBlockEntity be) {
            be.setAllowedFluid(func.apply(stack));
            be.setChanged();
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return dir.getAxis() == state.getValue(AXIS);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState, pos);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteredPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return FilteredPipeBlockEntity::tick;
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SimpleAxisNode(state.getValue(AXIS)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.AXIS, BlockConfig.INVERTED);
    }

    public static final class Model extends RotationAwareModel {
        private static final ItemStack NEGATED = ItemDisplayElementUtil.getModel(id("block/filtered_pipe_negated"));
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement fluid;
        private Model(BlockState state, BlockPos pos) {
            this.mainElement = ItemDisplayElementUtil.createSimple(state.getValue(FilteredPipeBlock.INVERTED) ? NEGATED : ItemDisplayElementUtil.getModel(state.getBlock().asItem()));
            this.mainElement.setScale(new Vector3f(2f));
            this.fluid = ItemDisplayElementUtil.createSimple();
            this.fluid.setScale(new Vector3f(2f));
            this.fluid.setViewRange(0.4f);
            this.updateStatePos(state);
            this.addElement(this.mainElement);
            this.addElement(this.fluid);
        }

        private void updateStatePos(BlockState state) {
            var dir = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.mainElement.setYaw(y);
            this.mainElement.setPitch(p);
            this.fluid.setYaw(y);
            this.fluid.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.mainElement.setItem(this.blockState().getValue(FilteredPipeBlock.INVERTED) ? NEGATED : ItemDisplayElementUtil.getModel(this.blockState().getBlock().asItem()));
                updateStatePos(this.blockState());
            }
        }

        public void setAllowedFluid(FluidInstance<?> fluid) {
            if (fluid == null) {
                this.fluid.setItem(ItemStack.EMPTY);
            } else {
                this.fluid.setItem(FactoryModels.FLUID_FILTERED_PIPE.get(fluid));
            }
        }
    }
}
