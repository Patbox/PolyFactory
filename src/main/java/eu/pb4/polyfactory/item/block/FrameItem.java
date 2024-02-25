package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;

public class FrameItem extends ModeledItem {
    public static final BooleanProperty PROPERTY = BooleanProperty.of("framed");

    public FrameItem(Settings settings) {
        super(settings);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.contains(PROPERTY) && !oldState.get(PROPERTY)) {
            context.getWorld().setBlockState(context.getBlockPos(), oldState.with(PROPERTY, true));
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}