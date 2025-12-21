package eu.pb4.polyfactory.item.configuration;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.item.util.SwitchActionItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WrenchItem extends SimplePolymerItem implements SwitchActionItem {
    public WrenchItem(Properties settings) {
        super(settings);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable("item.polyfactory.wrench.tooltip.1", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.polyfactory.wrench.tooltip.1.alt", Component.keybind("key.swapOffhand")).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.polyfactory.wrench.tooltip.2", Component.keybind("key.attack")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player && !context.getItemInHand().getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            return WrenchHandler.of(player).useBlockAction(player, context.getLevel(), context.getClickedPos(), context.getClickedFace(), false);
        }
        return InteractionResult.FAIL;
    }

    public InteractionResult handleBlockAttack(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player.getItemInHand(hand).is(this) && player instanceof ServerPlayer player1
                && !player.getItemInHand(hand).getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            WrenchHandler.of(player1).attackBlockAction(player1, world, pos, direction);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onSwitchAction(ServerPlayer player, ItemStack main, InteractionHand mainHand) {
        var raycast = WrenchHandler.getTarget(player);

        if (raycast.getType() == HitResult.Type.BLOCK
                && !main.getOrDefault(FactoryDataComponents.READ_ONLY, false)
                && raycast instanceof BlockHitResult result
                && WrenchHandler.of(player).useBlockAction(player, player.level(), result.getBlockPos(), result.getDirection(), true).consumesAction()) {
            player.swing(mainHand, true);
        }
        if (raycast.getType() == HitResult.Type.ENTITY
                && !main.getOrDefault(FactoryDataComponents.READ_ONLY, false)
                && raycast instanceof EntityHitResult result
                && WrenchHandler.of(player).useEntityAction(player, result.getEntity(), result.getLocation(), true).consumesAction()) {
            player.swing(mainHand, true);
        }

        return true;
    }

    public InteractionResult handleEntityAttack(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult entityHitResult) {
        if (player.getItemInHand(hand).is(this) && player instanceof ServerPlayer player1
                && !player.getItemInHand(hand).getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            WrenchHandler.of(player1).attackEntityAction(player1, entity, entityHitResult != null ? entityHitResult.getLocation() : Vec3.ZERO);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult handleEntityUse(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult entityHitResult) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(this)) {

            stack = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
            if (stack.is(this)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        }

        if (!stack.isEmpty() && player instanceof ServerPlayer player1
                && !stack.getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            if (entityHitResult == null) {
                return InteractionResult.FAIL;
            }

            var res = WrenchHandler.of(player1).useEntityAction(player1, entity, entityHitResult.getLocation(), false);
            if (res instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.SERVER) {
                player.swing(hand, true);
            }
            return res;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPolymerBlockInteraction(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult, InteractionResult actionResult) {
        return true;
    }

    @Override
    public boolean isPolymerEntityInteraction(ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, Entity entity, InteractionResult actionResult) {
        return true;
    }
}
