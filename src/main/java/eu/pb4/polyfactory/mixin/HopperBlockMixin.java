package eu.pb4.polyfactory.mixin;

import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.FilterItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
    private void polyfactory$dontDropItems(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (hand != Hand.MAIN_HAND) {
            return;
        }

        var be = world.getBlockEntity(pos) instanceof FilteredBlockEntity x ? x : null;

        if (be == null) {
            return;
        }

        var stack = player.getStackInHand(hand);
        if (stack.isOf(FactoryItems.ITEM_FILTER) && !FilterItem.getStack(stack).isEmpty()) {
            if (!be.polyfactory$getFilter().isEmpty()) {
                player.getInventory().offerOrDrop(be.polyfactory$getFilter());
            }
            be.polyfactory$setFilter(stack.copyWithCount(1));
            stack.decrement(1);
            cir.setReturnValue(ActionResult.SUCCESS);
        } else if (stack.isEmpty() && !be.polyfactory$getFilter().isEmpty() && player.isSneaking()) {
            player.setStackInHand(hand, be.polyfactory$getFilter());
            be.polyfactory$setFilter(ItemStack.EMPTY);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "onStateReplaced", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ItemScatterer;spawn(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/inventory/Inventory;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void polyfactory$dropFilter(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci, BlockEntity blockEntity) {
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,  ((FilteredBlockEntity) blockEntity).polyfactory$getFilter());
    }
}
