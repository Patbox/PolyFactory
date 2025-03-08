package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class SlotAwareFunnelBlock extends FunnelBlock {
    public SlotAwareFunnelBlock(Settings settings) {
        super(settings);
        Model.MODEL_IN.isEmpty();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking() && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof SlotAwareFunnelBlockEntity be) {
            be.openGui(serverPlayer);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SlotAwareFunnelBlockEntity(pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof SlotAwareFunnelBlockEntity be) {
                ItemScatterer.spawn(world, pos, be.asInventory());
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var selfState = self.getBlockState();
        if (!selfState.get(ENABLED)) {
            return false;
        }

        var selfDir = selfState.get(FACING);
        var mode = selfState.get(MODE);

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


        if (FactoryUtil.tryInsertingIntoSlot(self.getWorld(), self.getPos().offset(selfState.get(FACING)), stack.get(), selfDir.getOpposite(), list) == -1) {
            return selfDir.getAxis() == pushDirection.getAxis();
        }

        if (stack.get().isEmpty()) {
            conveyor.clearContainer();
        }


        return selfDir.getAxis() == pushDirection.getAxis();
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        if (relative != Direction.DOWN || !conveyor.isContainerEmpty()) {
            return;
        }

        var selfState = self.getBlockState();
        var mode = selfState.get(MODE);
        var selfFacing = selfState.get(FACING);
        if (!selfState.get(ENABLED) || !mode.toConveyor || pushDirection == selfFacing) {
            return;
        }
        var be = self.getBlockEntity() instanceof SlotAwareFunnelBlockEntity x ? x : null;
        if (be == null) {
            return;
        }

        var inv = HopperBlockEntity.getInventoryAt(self.getWorld(), self.getPos().offset(selfFacing));
        var sided = inv instanceof SidedInventory s ? s : null;
        if (inv != null) {
            for (var a = 0; a < be.slotTargets.length; a++) {
                var i = be.slotTargets[a];
                if (i >= inv.size() || i == -1) {
                    continue;
                }

                var stack = inv.getStack(i);
                if (!stack.isEmpty() && be.filter.get(a).test(stack) && (sided == null || sided.canExtract(i, stack, selfFacing.getOpposite()))) {
                    if (conveyor.pushNew(stack)) {
                        inv.markDirty();
                        if (stack.isEmpty()) {
                            inv.setStack(i, ItemStack.EMPTY);
                        }
                        conveyor.setMovementPosition(pushDirection.getOpposite() == selfFacing ? 0.15 : 0.5);
                        return;
                    }
                }
            }
        } else {
            var storage = ItemStorage.SIDED.find(self.getWorld(), self.getPos().offset(selfFacing), selfFacing);

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

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState, pos);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    public static final class Model extends FunnelBlock.Model {
        private static final ItemStack MODEL_IN = BaseItemProvider.requestModel(FactoryUtil.id("block/slot_aware_funnel_in"));
        private static final ItemStack MODEL_OUT = BaseItemProvider.requestModel(FactoryUtil.id("block/slot_aware_funnel_out"));
        private Model(BlockState state, BlockPos pos) {
            super(state, pos);
        }

        @Override
        protected ItemStack getModel(boolean outModel) {
            return outModel ? MODEL_OUT : MODEL_IN;
        }
    }
}
