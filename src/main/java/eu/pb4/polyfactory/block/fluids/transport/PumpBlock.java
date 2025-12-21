package eu.pb4.polyfactory.block.fluids.transport;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.mechanical.GearPlacementAligner;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.nodes.mechanical.UnconnectedGearMechanicalNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SmallGearNode;
import eu.pb4.polyfactory.nodes.pipe.PumpNode;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import static eu.pb4.polyfactory.ModInit.id;

public class PumpBlock extends NetworkBlock implements FactoryBlock, RotationUser, ConfigurableBlock, PipeConnectable, BarrierBasedWaterloggable, EntityBlock,
        NetworkComponent.Pipe, NetworkComponent.Rotational, NetworkComponent.RotationalConnector, GearPlacementAligner {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public PumpBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
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
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        Pipe.updatePipeAt(world, pos);
        RotationalConnector.updateRotationalConnectorAt(world, pos);
        Rotational.updateRotationalAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Pipe || block instanceof RotationalConnector || block instanceof Rotational;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return dir.getAxis() == state.getValue(FACING).getAxis();
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
        return new PumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return PumpBlockEntity::tick;
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new PumpNode(state.getValue(FACING), false, PumpNode.DEFAULT_RANGE), new PumpNode(state.getValue(FACING), true, PumpNode.DEFAULT_RANGE));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new UnconnectedGearMechanicalNode());
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SmallGearNode(state.getValue(FACING).getAxis()));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        modifier.stress(5);
    }

    @Override
    public boolean isLargeGear(BlockState state) {
        return false;
    }

    @Override
    public Direction.Axis getGearAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack BLOCK_MODEL = ItemDisplayElementUtil.getModel(id("block/pump"));
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement gear;
        private final boolean offset;
        private Direction.Axis axis = Direction.Axis.Y;

        private Model(BlockState state, BlockPos pos) {
            this.mainElement = ItemDisplayElementUtil.createSimple(BLOCK_MODEL);
            this.mainElement.setScale(new Vector3f(2f));
            this.offset = (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0;
            this.gear = LodItemDisplayElement.createSimple(GenericParts.REGULAR_GEAR, this.getUpdateRate(), 0.3f, 0.6f);
            this.gear.setViewRange(0.7f);
            this.updateStatePos(state);
            this.updateAnimation(0);
            this.addElement(this.mainElement);
            this.addElement(this.gear);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            this.axis = dir.getAxis();
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
        }

        private void updateAnimation(float rotation) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(rotation + (this.offset ? Mth.PI / 8 : 0));
            this.gear.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotationData().rotation());
                this.gear.startInterpolationIfDirty();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }
    }
}
