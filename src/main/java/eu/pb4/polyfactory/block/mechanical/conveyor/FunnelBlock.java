package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.FilterIcon;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;


public class FunnelBlock extends Block implements FactoryBlock, MovingItemConsumer, MovingItemProvider, ConfigurableBlock, EntityBlock, BarrierBasedWaterloggable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final EnumProperty<ConveyorLikeDirectional.TransferMode> MODE = EnumProperty.create("mode", ConveyorLikeDirectional.TransferMode.class,
            ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR, ConveyorLikeDirectional.TransferMode.TO_CONVEYOR);

    private static final BlockConfig MODE_ACTION = BlockConfig.of("mode", MODE, (t, world, pos, side, state) -> Component.translatable("item.polyfactory.wrench.action.mode.transfer_mode." + t.getSerializedName()));

    public FunnelBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(ENABLED, true));
        Model.MODEL_OUT.isEmpty();
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, MODE, ENABLED);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var selfState = self.getBlockState();
        if (!selfState.getValue(ENABLED)) {
            return false;
        }

        var selfDir = selfState.getValue(FACING);
        var mode = selfState.getValue(MODE);

        if (!mode.fromConveyor || relative != Direction.UP || selfDir.getOpposite() == pushDirection || conveyor.movementDelta() < (selfDir == pushDirection ? 0.90 : 0.48) || selfDir.getAxis() == Direction.Axis.Y) {
            return false;
        }
        var be = self.getBlockEntity();
        if (be instanceof FunnelBlockEntity funnelBlockEntity && !funnelBlockEntity.matches(conveyor.getContainer().get())) {
            return false;
        }
        var stack = conveyor.getContainer();

        if (FactoryUtil.tryInserting(self.getWorld(), self.getPos().relative(selfState.getValue(FACING)), stack.get(), selfDir.getOpposite()) == -1) {
            return selfDir.getAxis() == pushDirection.getAxis();
        }

        if (stack.get().isEmpty()) {
            conveyor.clearContainer();
        }


        return selfDir.getAxis() == pushDirection.getAxis();
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (relative != Direction.DOWN || !conveyor.isContainerEmpty()) {
            return;
        }

        var selfState = self.getBlockState();
        var mode = selfState.getValue(MODE);
        var selfFacing = selfState.getValue(FACING);
        if (!selfState.getValue(ENABLED) || !mode.toConveyor || pushDirection == selfFacing) {
            return;
        }
        var be = self.getBlockEntity() instanceof FunnelBlockEntity x ? x : null;
        if (be == null) {
            return;
        }

        var inv = HopperBlockEntity.getContainerAt(self.getWorld(), self.getPos().relative(selfFacing));
        var sided = inv instanceof WorldlyContainer s ? s : null;
        if (inv != null) {
            for (var i = 0; i < inv.getContainerSize(); i++) {
                var stack = inv.getItem(i);
                if (!stack.isEmpty() && be.matches(stack) && (sided == null || sided.canTakeItemThroughFace(i, stack, selfFacing.getOpposite()))) {
                    if (conveyor.pushNew(stack)) {
                        inv.setChanged();
                        if (stack.isEmpty()) {
                            inv.setItem(i, ItemStack.EMPTY);
                        }
                        conveyor.setMovementPosition(pushDirection.getOpposite() == selfFacing ? 0.15 : 0.5);
                        return;
                    }
                }
            }
        } else {
            var storage = ItemStorage.SIDED.find(self.getWorld(), self.getPos().relative(selfFacing), selfFacing);

            if (storage != null) {
                for (var view : storage) {
                    if (view.isResourceBlank() || !be.matches(view.getResource().toStack())) {
                        continue;
                    }

                    try (var t = Transaction.openOuter()) {
                        var resource = view.getResource();
                        var val = view.extract(view.getResource(), conveyor.getMaxStackCount(resource.toStack()), t);
                        if (val != 0) {
                            t.commit();

                            if (conveyor.pushNew(resource.toStack((int) val))) {
                                conveyor.setMovementPosition(pushDirection.getOpposite() == selfFacing ? 0.15 : 0.5);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered == state.getValue(ENABLED)) {
            world.setBlock(pos, state.setValue(ENABLED, !powered), 4);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace() == Direction.DOWN) {
            this.defaultBlockState().setValue(FACING, Direction.UP).setValue(MODE, ConveyorLikeDirectional.TransferMode.TO_CONVEYOR);
        }

        var dir = ctx.getClickedFace().getOpposite();

        if (dir == Direction.DOWN) {
            dir = ctx.getHorizontalDirection();
        }

        var selfPos = ctx.getClickedPos();
        if (ctx.getClickedFace() != Direction.UP) {
            selfPos = selfPos.relative(ctx.getClickedFace());
        }

        selfPos = selfPos.below();
        var below = ctx.getLevel().getBlockState(selfPos);
        var mode = below.getBlock() instanceof ConveyorLikeDirectional directional
                ? directional.getTransferMode(below, dir.getOpposite())
                : ConveyorLikeDirectional.TransferMode.TO_CONVEYOR;
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, dir).setValue(MODE, mode));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);


        var be = world.getBlockEntity(pos) instanceof FunnelBlockEntity x ? x : null;

        if (be == null || !be.checkUnlocked(player)) {
            return InteractionResult.FAIL;
        }

        if (stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack)) {
            if (!be.getFilter().isEmpty()) {
                player.getInventory().placeItemBackInInventory(be.getFilter());
            }
            be.setFilter(stack.copyWithCount(1));
            if (player instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.ITEM_FILTER_USE);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        } else if (stack.isEmpty() && !be.getFilter().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, be.getFilter());
            be.setFilter(ItemStack.EMPTY);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
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
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.SPRUCE_PLANKS.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FunnelBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState, pos);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING, MODE_ACTION);
    }

    public static class Model extends BlockModel {
        private static final ItemStack MODEL_IN = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/funnel_in"));
        private static final ItemStack MODEL_OUT = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/funnel_out"));
        final FilterIcon filterElement = new FilterIcon(this);
        private final ItemDisplayElement mainElement;
        private final float offset;

        protected Model(BlockState state, BlockPos pos) {
            this.mainElement = new LodItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setItemDisplayContext(ItemDisplayContext.FIXED);
            this.mainElement.setInvisible(true);
            this.mainElement.setViewRange(0.8f);
            this.offset = pos.distManhattan(BlockPos.ZERO) % 2 == 0 ? 0.002f : 0;
            this.updateFacing(state);
            this.addElement(this.mainElement);
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.getValue(FACING).getRotation().mul(Direction.NORTH.getRotation());
            var mat = mat();
            mat.rotate(rot);
            mat.translate(0, this.offset / 2, this.offset);
            mat.scale(2.01f);
            var outModel = facing.getValue(MODE) == ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR;

            this.mainElement.setItem(getModel(outModel));
            this.mainElement.setTransformation(mat);

            mat.identity();
            mat.rotate(rot).rotateY(Mth.PI);
            if (outModel) {
                mat.rotateX(-22.5f * Mth.DEG_TO_RAD);
                mat.translate(0, 7.25f / 16f, 0.008f);
            } else {
                mat.translate(0, 8.5f / 16f, 0.025f);
            }
            mat.scale(0.3f, 0.3f, 0.005f);
            this.filterElement.setTransformation(mat);
            this.tick();
        }

        protected ItemStack getModel(boolean outModel) {
            return outModel ? MODEL_OUT : MODEL_IN;
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }

}
