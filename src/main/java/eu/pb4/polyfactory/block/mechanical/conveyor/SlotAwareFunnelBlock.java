package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class SlotAwareFunnelBlock extends FunnelBlock {
    public SlotAwareFunnelBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof SlotAwareFunnelBlockEntity be) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlotAwareFunnelBlockEntity(pos, state);
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
        if (!(self.getBlockEntity() instanceof SlotAwareFunnelBlockEntity be)) {
            return false;
        }
        var stack = conveyor.getContainer();
        if (stack == null) {
            return false;
        }

        var list = new IntArrayList();
        for (int i = 0; i < 9; i++) {
            if (be.filter.get(i).test(stack.get())) {
                if (be.slotTargets[i] != -1) {
                    list.add(be.slotTargets[i]);
                }
            }
        }

        if (list.isEmpty()) {
            return selfDir.getAxis() == pushDirection.getAxis();
        }

        var stackToMove = stack.get();
        var copied = false;
        if (be.maxStackSize() < stackToMove.getCount()) {
            stackToMove = stackToMove.split(be.maxStackSize());
            copied = true;
        }


        if (FactoryUtil.tryInsertingIntoSlot(self.getWorld(), self.getPos().relative(selfState.getValue(FACING)), stackToMove, selfDir.getOpposite(), list) == -1) {
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
        var be = self.getBlockEntity() instanceof SlotAwareFunnelBlockEntity x ? x : null;
        if (be == null) {
            return;
        }

        var inv = HopperBlockEntity.getContainerAt(self.getWorld(), self.getPos().relative(selfFacing));
        var sided = inv instanceof WorldlyContainer s ? s : null;
        if (inv != null) {
            for (var a = 0; a < be.slotTargets.length; a++) {
                var i = be.slotTargets[a];
                if (i >= inv.getContainerSize() || i == -1) {
                    continue;
                }

                var stack = inv.getItem(i);
                if (!stack.isEmpty() && be.filter.get(a).test(stack) && (sided == null || sided.canTakeItemThroughFace(i, stack, selfFacing.getOpposite()))) {
                    inv.setChanged();
                    if (conveyor.pushNew(stack.split(Math.min(be.maxStackSize(), stack.getCount())))) {
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

            if (storage instanceof SlottedStorage<ItemVariant> slottedStorage) {
                for (var a = 0; a < be.slotTargets.length; a++) {
                    var i = be.slotTargets[a];
                    if (i >= slottedStorage.getSlotCount() || i == -1) {
                        continue;
                    }
                    var view = slottedStorage.getSlot(i);
                    if (view.isResourceBlank() || !be.filter.get(a).test(view.getResource().toStack())) {
                        continue;
                    }
                    try (var t = Transaction.openOuter()) {
                        var resource = view.getResource();
                        var val = view.extract(view.getResource(), Math.min(conveyor.getMaxStackCount(resource.toStack()), be.maxStackSize()), t);
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

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.mechanical.conveyor.SlotAwareFunnelBlock.Model(initialBlockState, pos);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    public static final class Model extends FunnelBlock.Model {
        private static final ItemStack MODEL_IN = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/slot_aware_funnel_in"));
        private static final ItemStack MODEL_OUT = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/slot_aware_funnel_out"));
        private Model(BlockState state, BlockPos pos) {
            super(state, pos);
        }

        @Override
        protected ItemStack getModel(boolean outModel) {
            return outModel ? MODEL_OUT : MODEL_IN;
        }
    }
}
