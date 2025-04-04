package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void polyfactory$dontDropItems(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        var be = world.getBlockEntity(pos) instanceof FilteredBlockEntity x ? x : null;

        if (be == null) {
            return;
        }

        var stack = player.getStackInHand(Hand.MAIN_HAND);
        if (stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack)) {
            if (!be.polyfactory$getFilter().isEmpty()) {
                player.getInventory().offerOrDrop(be.polyfactory$getFilter());
            }
            be.polyfactory$setFilter(stack.copyWithCount(1));
            stack.decrement(1);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.ITEM_FILTER_USE);
            }
            cir.setReturnValue(ActionResult.SUCCESS_SERVER);
        } else if (stack.isEmpty() && !be.polyfactory$getFilter().isEmpty() && player.isSneaking()) {
            player.setStackInHand(Hand.MAIN_HAND, be.polyfactory$getFilter());
            be.polyfactory$setFilter(ItemStack.EMPTY);
            cir.setReturnValue(ActionResult.SUCCESS_SERVER);
        }
    }

    @Inject(method = "onStateReplaced", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ItemScatterer;onStateReplaced(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void polyfactory$dropFilter(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof FilteredBlockEntity blockEntity) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, blockEntity.polyfactory$getFilter());
            blockEntity.polyfactory$setFilter(ItemStack.EMPTY);
        }
    }
}
