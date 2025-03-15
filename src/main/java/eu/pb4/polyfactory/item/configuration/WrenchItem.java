package eu.pb4.polyfactory.item.configuration;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.item.util.SwitchActionItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class WrenchItem extends SimplePolymerItem implements SwitchActionItem {
    public WrenchItem(Settings settings) {
        super(settings);
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.polyfactory.wrench.tooltip.1", Text.keybind("key.use")).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.polyfactory.wrench.tooltip.1.alt", Text.keybind("key.swapOffhand")).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.polyfactory.wrench.tooltip.2", Text.keybind("key.attack")).formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player && !context.getStack().getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            return WrenchHandler.of(player).useAction(player, context.getWorld(), context.getBlockPos(), context.getSide(), false);
        }
        return ActionResult.FAIL;
    }

    public ActionResult handleBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getStackInHand(hand).isOf(this) && player instanceof ServerPlayerEntity player1
                && !player.getStackInHand(hand).getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            WrenchHandler.of(player1).attackAction(player1, world, pos, direction);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean onSwitchAction(ServerPlayerEntity player, ItemStack main, Hand mainHand) {
        var raycast = player.raycast(player.getBlockInteractionRange(), 0, false);

        if (raycast.getType() == HitResult.Type.BLOCK
                && !main.getOrDefault(FactoryDataComponents.READ_ONLY, false)
                && raycast instanceof BlockHitResult result
                && WrenchHandler.of(player).useAction(player, player.getServerWorld(), result.getBlockPos(), result.getSide(), true).isAccepted()) {
            player.swingHand(mainHand, true);
        }


        return true;
    }
}
