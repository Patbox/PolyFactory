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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

public class ClutchBlock extends RotationalNetworkBlock implements FactoryBlock, ConfigurableBlock, BarrierBasedWaterloggable {
    public static final Property<Direction.Axis> AXIS = Properties.AXIS;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty INVERTED = Properties.INVERTED;

    public ClutchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false).with(POWERED, false).with(INVERTED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED, POWERED, INVERTED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onBlockAdded(state,world,pos, oldState, notify);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        this.updatePowered(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, super.getPlacementState(ctx).with(AXIS, ctx.getSide().getAxis()));
    }

    private void updatePowered(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered), 4);
        }
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        var ax = state.get(AXIS);
        return state.get(POWERED) == state.get(INVERTED)
                ? List.of(new SimpleAxisNode(state.get(AXIS)))
                : List.of(
                new SimpleDirectionNode(Direction.from(ax, Direction.AxisDirection.POSITIVE)),
                new SimpleDirectionNode(Direction.from(ax, Direction.AxisDirection.NEGATIVE)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED));
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.AXIS, BlockConfig.INVERTED);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement left;
        private final ItemDisplayElement right;
        private Model(ServerWorld world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));

            this.left = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate(), 0.3f, 0.6f);
            this.left.setViewRange(0.5f);
            this.left.setScale(new Vector3f(2));

            this.right = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate(), 0.3f, 0.6f);
            this.right.setViewRange(0.5f);
            this.right.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, 0,  state.get(AXIS));
            this.addElement(this.left);
            this.addElement(this.right);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(AXIS);
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
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(left);
            mat.scale(2, 2f, 2);
            this.left.setTransformation(mat);

            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.WEST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.NORTH.getRotationQuaternion());
                case Y -> mat.rotateZ(MathHelper.PI);
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

            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                var state = this.blockState();


                float left, right;

                if (state.get(POWERED)) {
                    left = this.getRotationData(this::isNegative).rotation();
                    right = this.getRotationData(this::isPositive).rotation();
                } else {
                    left = this.getRotation();
                    right = left;
                }

                this.updateAnimation(left, right, this.blockState().get(AXIS));
                this.left.startInterpolationIfDirty();

                this.right.startInterpolationIfDirty();
            }
        }

        private boolean isPositive(NodeHolder<?> nodeHolder) {
            return nodeHolder.getNode() instanceof DirectionNode directionNode && directionNode.direction().getDirection() == Direction.AxisDirection.POSITIVE;
        }

        private boolean isNegative(NodeHolder<?> nodeHolder) {
            return nodeHolder.getNode() instanceof DirectionNode directionNode && directionNode.direction().getDirection() == Direction.AxisDirection.NEGATIVE;
        }
    }
}
