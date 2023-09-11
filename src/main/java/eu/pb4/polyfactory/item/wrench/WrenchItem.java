package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.util.SimpleModeledPolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WrenchItem extends Item implements SimpleModeledPolymerItem {
    public WrenchItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            return WrenchHandler.of(player).useAction(player, context.getWorld(), context.getBlockPos(), context.getSide());
        }
        return ActionResult.FAIL;
    }

    @Override
    public Item getPolymerItem() {
        return Items.STONE_HOE;
    }

    public ActionResult handleBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getStackInHand(hand).isOf(this) && player instanceof ServerPlayerEntity player1) {
            WrenchHandler.of(player1).attackAction(player1, world, pos, direction);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

}
