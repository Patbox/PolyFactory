package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.util.ModeledItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public class GearItem extends ModeledItem implements DyeableItem {
    public static boolean enable = false;
    public GearItem(Settings settings) {
        super(Items.IRON_INGOT, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!enable) {
            return super.useOnBlock(context);
        }

        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.isOf(FactoryBlocks.AXLE)) {
            context.getWorld().setBlockState(context.getBlockPos(), FactoryBlocks.AXLE_WITH_GEAR.getStateWithProperties(oldState));
            context.getStack().decrement(1);
            return ActionResult.SUCCESS;
        }

        return super.useOnBlock(context);
    }

}
