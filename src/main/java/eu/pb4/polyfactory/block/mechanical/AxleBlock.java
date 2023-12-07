package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.item.AutoModeledPolymerItem;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.common.impl.CommonImplUtils;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.impl.networking.PolymerServerProtocol;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AxleBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker, WrenchableBlock, BarrierBasedWaterloggable {
    public static final Property<Direction.Axis> AXIS = Properties.AXIS;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public AxleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, super.getPlacementState(ctx).with(AXIS, ctx.getSide().getAxis()));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SimpleAxisNode(state.get(AXIS)));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.STRIPPED_OAK_LOG.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Blocks.LIGHTNING_ROD.getDefaultState().with(Properties.FACING, Direction.from(state.get(AXIS), Direction.AxisDirection.POSITIVE))
                .getCollisionShape(world, pos, context);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        var a = state.get(AXIS);

        if (a == Direction.Axis.Y || rotation == BlockRotation.NONE || rotation == BlockRotation.CLOCKWISE_180) {
            return state;
        }

        return state.with(AXIS, a == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    public void onPolymerBlockSend(BlockState blockState, BlockPos.Mutable pos, ServerPlayerEntity player) {
        if (pos.getSquaredDistanceFromCenter(player.getX(), player.getY(), player.getZ()) < 16 * 16) {
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos.toImmutable(), Blocks.LIGHTNING_ROD.getDefaultState()
                    .with(Properties.FACING, Direction.from(blockState.get(AXIS), Direction.AxisDirection.POSITIVE))
                    .with(LightningRodBlock.POWERED, true).with(WATERLOGGED, blockState.get(WATERLOGGED))));
            //noinspection UnstableApiUsage
            PolymerServerProtocol.sendBlockUpdate(player.networkHandler, pos, blockState);
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED));
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.AXIS);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL = new ItemStack(FactoryItems.AXLE.getPolymerItem());
        public static final ItemStack ITEM_MODEL_SHORT = new ItemStack(BaseItemProvider.requestItem());
        private final ItemDisplayElement mainElement;
        private final Set<ServerPlayNetworkHandler> viewingClose = new ObjectOpenCustomHashSet<>(CommonImplUtils.IDENTITY_HASH);
        private final List<ServerPlayNetworkHandler> sentRod = new ArrayList<>();
        private final List<ServerPlayNetworkHandler> sentBarrier = new ArrayList<>();
        private Model(ServerWorld world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.updateAnimation(0,  state.get(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(rotation);

            mat.scale(2, 2.005f, 2);
            this.mainElement.setTransformation(mat);
        }

        @Override
        public boolean stopWatching(ServerPlayNetworkHandler player) {
            if (super.stopWatching(player)) {
                this.viewingClose.remove(player);
                return true;
            }
            return false;
        }

        @Override
        protected void onTick() {
            var rodState = Blocks.LIGHTNING_ROD.getDefaultState()
                    .with(Properties.FACING, Direction.from(BlockBoundAttachment.get(this).getBlockState().get(AXIS),
                            Direction.AxisDirection.POSITIVE)).with(LightningRodBlock.POWERED, true).with(WATERLOGGED, BlockBoundAttachment.get(this).getBlockState().get(WATERLOGGED));
            var pos = BlockBoundAttachment.get(this).getBlockPos();
            var state = BlockBoundAttachment.get(this).getBlockState();
            for (var player : this.sentRod) {
                player.sendPacket(new BlockUpdateS2CPacket(pos, rodState));
                //noinspection UnstableApiUsage
                PolymerServerProtocol.sendBlockUpdate(player, pos, state);
            }
            this.sentRod.clear();
            for (var player : this.sentBarrier) {
                player.sendPacket(new BlockUpdateS2CPacket(pos, Blocks.BARRIER.getDefaultState().with(WATERLOGGED, BlockBoundAttachment.get(this).getBlockState().get(WATERLOGGED))));
                //noinspection UnstableApiUsage
                PolymerServerProtocol.sendBlockUpdate(player, pos, state);
            }
            this.sentBarrier.clear();

            for (var player : this.getWatchingPlayers()) {
                var d = this.getSquaredDistance(player);

                if (d < 16 * 16) {
                    if (!this.viewingClose.contains(player)) {
                        this.sentRod.add(player);
                        this.viewingClose.add(player);
                    }
                } else if (this.viewingClose.contains(player)) {
                    this.sentBarrier.add(player);
                    this.viewingClose.remove(player);
                }
            }

            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(), ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(AXIS));
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                }
            }
        }

        static {
            ITEM_MODEL.getOrCreateNbt().putInt("CustomModelData", AutoModeledPolymerItem.MODELS.get(FactoryItems.AXLE).value());
            ITEM_MODEL_SHORT.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(ITEM_MODEL_SHORT.getItem(), FactoryUtil.id("block/axle_short")).value());
        }
    }
}
