package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.LazyItemStack;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.item.configuration.WrenchHandler;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import eu.pb4.polyfactory.models.FilterIcon;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.List;


public class FunnelBlock extends Block implements FactoryBlock, MovingItemConsumer, MovingItemProvider, ConfigurableBlock, EntityBlock, BarrierBasedWaterloggable, PolymerTexturedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final EnumProperty<ConveyorLikeDirectional.TransferMode> MODE = EnumProperty.create("mode", ConveyorLikeDirectional.TransferMode.class,
            ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR, ConveyorLikeDirectional.TransferMode.TO_CONVEYOR);
    private static final BlockConfig<?> MODE_ACTION = BlockConfig.of("mode", MODE, (t, world, pos, side, state) -> Component.translatable("item.polyfactory.wrench.action.mode.transfer_mode." + t.getSerializedName()));
    private static final BlockConfig<?> MAX_STACK_SIZE_ACTION = BlockConfig.ofBlockEntityInt("max_stack_size", CommonBlockEntity.class, 1, 64, 0, CommonBlockEntity::maxStackSize, CommonBlockEntity::setMaxStackSize);
    private static final BlockConfig<?> MIN_STACK_SIZE_ACTION = BlockConfig.ofBlockEntityInt("min_stack_size", CommonBlockEntity.class, 1, 64, 0, CommonBlockEntity::minStackSize, CommonBlockEntity::setMinStackSize);

    public FunnelBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(ENABLED, true));
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
        if (!(be instanceof FunnelBlockEntity funnelBlockEntity) || !funnelBlockEntity.matches(conveyor.getContainer().get())) {
            return false;
        }

        var stack = conveyor.getContainer();
        var stackToMove = stack.get();
        if (stackToMove.getCount() < funnelBlockEntity.minStackSize()) {
            return false;
        }

        var copied = false;
        if (stackToMove.getCount() > funnelBlockEntity.maxStackSize()) {
            stackToMove = stackToMove.split(funnelBlockEntity.maxStackSize());
            copied = true;
        }


        if (FactoryUtil.tryInserting(self.getWorld(), self.getPos().relative(selfState.getValue(FACING)), stackToMove, selfDir.getOpposite()) == -1) {
            return selfDir.getAxis() == pushDirection.getAxis();
        }

        if (copied && !stackToMove.isEmpty()) {
            stack.get().grow(stackToMove.getCount());
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
                if (!stack.isEmpty() && stack.getCount() >= be.minStackSize() && be.matches(stack) && (sided == null || sided.canTakeItemThroughFace(i, stack, selfFacing.getOpposite()))) {
                    inv.setChanged();
                    var split = stack.split(getStackSizeToPush(conveyor, be, stack, stack.getCount()));
                    if (conveyor.pushNew(split)) {
                        if (stack.isEmpty()) {
                            inv.setItem(i, ItemStack.EMPTY);
                        }
                        conveyor.setMovementPosition(pushDirection.getOpposite() == selfFacing ? 0.15 : 0.5);
                        return;
                    } else {
                        stack.grow(split.getCount());
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
                        var val = view.extract(view.getResource(), getStackSizeToPush(conveyor, be, view.getResource().toStack(), view.getAmount()), t);
                        if (val != 0 && val >= be.minStackSize()) {
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

    protected static int getStackSizeToPush(MovingItemContainerHolder conveyor, CommonBlockEntity be, ItemStack stack, long count) {
        var res = Math.toIntExact(Math.min(count, be.maxStackSize()));
        return res < be.minStackSize() ? 0 : Math.min(conveyor.getMaxStackCount(stack), res);
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

    // Not really logical, but needed for old world support
    @Override
    protected @NonNull VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
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
        return (state.getValue(WATERLOGGED) ? FactoryUtil.TRAPDOOR_WATERLOGGED : FactoryUtil.TRAPDOOR_REGULAR).get(state.getValue(FACING).getOpposite());
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
        return List.of(BlockConfig.FACING, MODE_ACTION, MIN_STACK_SIZE_ACTION, MAX_STACK_SIZE_ACTION);
    }

    public interface CommonBlockEntity {
        int maxStackSize();
        int minStackSize();

        void setMaxStackSize(int maxStackSize);
        void setMinStackSize(int minStackSize);

        default boolean matchesStackSize(int count) {
            return count <= this.maxStackSize() && count >= this.minStackSize();
        }
    }

    public static class Model extends BlockModel implements WrenchHandler.ExtraModelCallbacks {
        private static final LazyItemStack MODEL_IN = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/funnel_in"));
        private static final LazyItemStack MODEL_OUT = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/funnel_out"));
        final FilterIcon filterElement = new FilterIcon(this);
        private final ItemDisplayElement mainElement;
        private final float offset;
        public TextDisplayElement maxCount;
        public TextDisplayElement minCount;

        protected Model(BlockState state, BlockPos pos) {
            this.mainElement = new LodItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setItemDisplayContext(ItemDisplayContext.FIXED);
            this.mainElement.setInvisible(true);
            this.mainElement.setViewRange(0.8f);

            this.maxCount = new TextDisplayElement();
            this.maxCount.setShadow(true);
            this.maxCount.setBackground(0);
            this.maxCount.setViewRange(0);

            this.minCount = new TextDisplayElement();
            this.minCount.setShadow(true);
            this.minCount.setBackground(0);
            this.minCount.setViewRange(0);

            this.offset = pos.distManhattan(BlockPos.ZERO) % 2 == 0 ? 0.002f : 0;
            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.maxCount);
            this.addElement(this.minCount);
        }

        public void updateFacing(BlockState facing) {
            var rot = facing.getValue(FACING).getRotation().mul(Direction.NORTH.getRotation());
            var mat = matStack();
            mat.rotate(rot);
            mat.translate(0, this.offset / 2, this.offset);
            mat.scale(2.01f);
            var outModel = facing.getValue(MODE) == ConveyorLikeDirectional.TransferMode.FROM_CONVEYOR;

            this.mainElement.setItem(getModel(outModel));
            this.mainElement.setTransformation(mat);

            mat.identity();
            mat.rotate(rot).rotateY(Mth.PI);
            if (outModel) {
                mat.rotateX(-12.5f * Mth.DEG_TO_RAD);
                mat.translate(0, 7.25f / 16f, 1.5f / 16f);
            } else {
                mat.translate(0, 8.5f / 16f, 3 / 16f - 0.005f);
            }
            mat.scale(0.3f, 0.3f, 0.005f);

            var items = this.filterElement.getCount();
            var offset = items == 0 ? 0.5f : (items / 2f + 0.5f) * (items > 3 ? 3f / items : 1) + 0.25f;

            mat.pushMatrix();
            mat.translate(-offset, -3f / 16f, 0);
            mat.rotateY(Mth.PI);
            mat.scale(1.5f);
            this.maxCount.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(offset, -3f / 16f, 0);
            mat.rotateY(Mth.PI);
            mat.scale(1.5f);
            this.minCount.setTransformation(mat);
            mat.popMatrix();


            this.filterElement.setTransformation(mat);

            this.tick();
        }

        protected ItemStack getModel(boolean outModel) {
            return (outModel ? MODEL_OUT : MODEL_IN).get();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }

        @Override
        public void startTargetting(ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityDataPacket(this.maxCount.getEntityId(), List.of(
                    SynchedEntityData.DataValue.create(DisplayEntityData.VIEW_RANGE, 1f)
            )));
            player.connection.send(new ClientboundSetEntityDataPacket(this.minCount.getEntityId(), List.of(
                    SynchedEntityData.DataValue.create(DisplayEntityData.VIEW_RANGE, 1f)
            )));
        }

        @Override
        public void stopTargetting(ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityDataPacket(this.maxCount.getEntityId(), List.of(
                    SynchedEntityData.DataValue.create(DisplayEntityData.VIEW_RANGE, 0f)
            )));
            player.connection.send(new ClientboundSetEntityDataPacket(this.minCount.getEntityId(), List.of(
                    SynchedEntityData.DataValue.create(DisplayEntityData.VIEW_RANGE, 0f)
            )));
        }
    }

}
