package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.util.ModeledItem;
import eu.pb4.polyfactory.item.util.SimpleModeledPolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WrenchItem extends ModeledItem {
    public WrenchItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.polyfactory.wrench.tooltip.1", Text.keybind("key.use")).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.polyfactory.wrench.tooltip.2", Text.keybind("key.attack")).formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            return WrenchHandler.of(player).useAction(player, context.getWorld(), context.getBlockPos(), context.getSide());
        }
        return ActionResult.FAIL;
    }

    public ActionResult handleBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getStackInHand(hand).isOf(this) && player instanceof ServerPlayerEntity player1) {
            WrenchHandler.of(player1).attackAction(player1, world, pos, direction);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

}
