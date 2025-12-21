package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.other.BlockWithTooltip;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class TooltippedBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> TooltippedBlockItem(T block, Properties settings, Item item) {
        super(block, settings, item);
    }

    public <T extends Block & PolymerBlock> TooltippedBlockItem(T block, Properties settings) {
        super(block, settings);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        if (this.getBlock() instanceof BlockWithTooltip appender) {
            appender.appendTooltip(stack, context, displayComponent, textConsumer, type);
        }

        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
