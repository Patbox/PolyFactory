package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.generic.SimpleDirectionNode;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;

public class ClutchBlock extends RotationalNetworkBlock implements FactoryBlock, ConfigurableBlock, BarrierBasedWaterloggable {
    public static final Property<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public ClutchBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(POWERED, false).setValue(INVERTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED, POWERED, INVERTED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onPlace(state,world,pos, oldState, notify);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updatePowered(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, super.getStateForPlacement(ctx).setValue(AXIS, ctx.getClickedFace().getAxis()));
    }

    private void updatePowered(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlock(pos, state.setValue(POWERED, powered), 4);
        }
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        var ax = state.getValue(AXIS);
        return state.getValue(POWERED) == state.getValue(INVERTED)
                ? List.of(new SimpleAxisNode(state.getValue(AXIS)))
                : List.of(
                new SimpleDirectionNode(Direction.fromAxisAndDirection(ax, Direction.AxisDirection.POSITIVE)),
                new SimpleDirectionNode(Direction.fromAxisAndDirection(ax, Direction.AxisDirection.NEGATIVE)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.AXIS, BlockConfig.INVERTED);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement left;
        private final ItemDisplayElement right;
        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));

            this.left = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate(), 0.3f, 0.6f);
            this.left.setViewRange(0.5f);
            this.left.setScale(new Vector3f(2));

            this.right = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate(), 0.3f, 0.6f);
            this.right.setViewRange(0.5f);
            this.right.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, 0,  state.getValue(AXIS));
            this.addElement(this.left);
            this.addElement(this.right);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(AXIS);
            float p = 0;
            float y = 0;

            if (dir == Direction.Axis.X) {
                p = 0;
                y = 90;
            } else if (dir == Direction.Axis.Z) {
                p = 0;
                y = 0;
            } else if (dir == Direction.Axis.Y) {
                p = 90;
            }


            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        private void updateAnimation(float left, float right, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(left);
            mat.scale(2, 2f, 2);
            this.left.setTransformation(mat);

            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.WEST.getRotation());
                case Z -> mat.rotate(Direction.NORTH.getRotation());
                case Y -> mat.rotateZ(Mth.PI);
            }

            mat.rotateY(-right);

            mat.scale(2, 2f, 2);
            this.right.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        @Override
        protected void onTick() {

            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                var state = this.blockState();


                float left, right;

                if (state.getValue(POWERED)) {
                    left = this.getRotationData(this::isNegative).rotation();
                    right = this.getRotationData(this::isPositive).rotation();
                } else {
                    left = this.getRotation();
                    right = left;
                }

                this.updateAnimation(left, right, this.blockState().getValue(AXIS));
                this.left.startInterpolationIfDirty();

                this.right.startInterpolationIfDirty();
            }
        }

        private boolean isPositive(NodeHolder<?> nodeHolder) {
            return nodeHolder.getNode() instanceof DirectionNode directionNode && directionNode.direction().getAxisDirection() == Direction.AxisDirection.POSITIVE;
        }

        private boolean isNegative(NodeHolder<?> nodeHolder) {
            return nodeHolder.getNode() instanceof DirectionNode directionNode && directionNode.direction().getAxisDirection() == Direction.AxisDirection.NEGATIVE;
        }
    }
}
