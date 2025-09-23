package eu.pb4.polyfactory.item.configuration;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.item.util.SwitchActionItem;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class WrenchItem extends SimplePolymerItem implements SwitchActionItem {
    public WrenchItem(Settings settings) {
        super(settings);
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.polyfactory.wrench.tooltip.1", Text.keybind("key.use")).formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("item.polyfactory.wrench.tooltip.1.alt", Text.keybind("key.swapOffhand")).formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("item.polyfactory.wrench.tooltip.2", Text.keybind("key.attack")).formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player && !context.getStack().getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            return WrenchHandler.of(player).useBlockAction(player, context.getWorld(), context.getBlockPos(), context.getSide(), false);
        }
        return ActionResult.FAIL;
    }

    public ActionResult handleBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getStackInHand(hand).isOf(this) && player instanceof ServerPlayerEntity player1
                && !player.getStackInHand(hand).getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            WrenchHandler.of(player1).attackBlockAction(player1, world, pos, direction);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean onSwitchAction(ServerPlayerEntity player, ItemStack main, Hand mainHand) {
        var raycast = WrenchHandler.getTarget(player);

        if (raycast.getType() == HitResult.Type.BLOCK
                && !main.getOrDefault(FactoryDataComponents.READ_ONLY, false)
                && raycast instanceof BlockHitResult result
                && WrenchHandler.of(player).useBlockAction(player, player.getEntityWorld(), result.getBlockPos(), result.getSide(), true).isAccepted()) {
            player.swingHand(mainHand, true);
        }
        if (raycast.getType() == HitResult.Type.ENTITY
                && !main.getOrDefault(FactoryDataComponents.READ_ONLY, false)
                && raycast instanceof EntityHitResult result
                && WrenchHandler.of(player).useEntityAction(player, result.getEntity(), result.getPos(), true).isAccepted()) {
            player.swingHand(mainHand, true);
        }

        return true;
    }

    public ActionResult handleEntityAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (player.getStackInHand(hand).isOf(this) && player instanceof ServerPlayerEntity player1
                && !player.getStackInHand(hand).getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            WrenchHandler.of(player1).attackEntityAction(player1, entity, entityHitResult != null ? entityHitResult.getPos() : Vec3d.ZERO);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public ActionResult handleEntityUse(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        var stack = player.getStackInHand(hand);
        if (!stack.isOf(this)) {

            stack = hand == Hand.MAIN_HAND ? player.getOffHandStack() : player.getMainHandStack();
            if (stack.isOf(this)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        }

        if (!stack.isEmpty() && player instanceof ServerPlayerEntity player1
                && !stack.getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            if (entityHitResult == null) {
                return ActionResult.FAIL;
            }

            var res = WrenchHandler.of(player1).useEntityAction(player1, entity, entityHitResult.getPos(), false);
            if (res instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.SERVER) {
                player.swingHand(hand, true);
            }
            return res;
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean isPolymerBlockInteraction(BlockState state, ServerPlayerEntity player, Hand hand, ItemStack stack, ServerWorld world, BlockHitResult blockHitResult, ActionResult actionResult) {
        return true;
    }

    @Override
    public boolean isPolymerEntityInteraction(ServerPlayerEntity player, Hand hand, ItemStack stack, ServerWorld world, Entity entity, ActionResult actionResult) {
        return true;
    }
}
