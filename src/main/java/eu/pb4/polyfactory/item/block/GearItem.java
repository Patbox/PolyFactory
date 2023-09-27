package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.util.ModeledItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class GearItem extends ModeledItem implements DyeableItem {
    private final AxleWithGearBlock block;

    public GearItem(AxleWithGearBlock block, Settings settings) {
        super(settings);
        this.block = block;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.isOf(FactoryBlocks.AXLE)) {
            context.getWorld().setBlockState(context.getBlockPos(), this.block.getStateWithProperties(oldState));
            NetworkComponent.RotationalConnector.updateRotationalConnectorAt(context.getWorld(), context.getBlockPos());
            context.getStack().decrement(1);
            return ActionResult.SUCCESS;
        }

        return super.useOnBlock(context);
    }

}
