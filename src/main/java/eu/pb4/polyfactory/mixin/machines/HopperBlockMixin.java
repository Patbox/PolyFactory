package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void polyfactory$dontDropItems(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        var be = world.getBlockEntity(pos) instanceof FilteredBlockEntity x ? x : null;

        if (be == null) {
            return;
        }

        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack)) {
            if (!be.polyfactory$getFilter().isEmpty()) {
                player.getInventory().placeItemBackInInventory(be.polyfactory$getFilter());
            }
            be.polyfactory$setFilter(stack.copyWithCount(1));
            stack.shrink(1);
            if (player instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.ITEM_FILTER_USE);
            }
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        } else if (stack.isEmpty() && !be.polyfactory$getFilter().isEmpty() && player.isShiftKeyDown()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, be.polyfactory$getFilter());
            be.polyfactory$setFilter(ItemStack.EMPTY);
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }
}
