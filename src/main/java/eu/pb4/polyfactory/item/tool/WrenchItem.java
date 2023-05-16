package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.util.SimpleModeledPolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WrenchItem extends Item implements SimpleModeledPolymerItem {
    public WrenchItem() {
        super(new Settings().maxCount(1).maxDamage(1000));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof Wrenchable wrenchable) {
            return wrenchable.useWithWrench(context);
        }

        return ActionResult.FAIL;
    }

    @Override
    public Item getPolymerItem() {
        return Items.STONE_HOE;
    }

    public ActionResult handleBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (player.getStackInHand(hand).isOf(this)) {
            if (world.getBlockState(pos).getBlock() instanceof Wrenchable wrenchable) {
                wrenchable.attackWithWrench(player, world, hand, pos, direction);
            }

            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public interface Wrenchable {
        default ActionResult useWithWrench(ItemUsageContext context) {
            return ActionResult.FAIL;
        };

        default void attackWithWrench(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {};
    }
}
