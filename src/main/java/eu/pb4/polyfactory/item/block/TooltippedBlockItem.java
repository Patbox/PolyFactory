package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.other.BlockWithTooltip;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class TooltippedBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> TooltippedBlockItem(T block, Settings settings, Item item) {
        super(block, settings, item);
    }

    public <T extends Block & PolymerBlock> TooltippedBlockItem(T block, Settings settings) {
        super(block, settings);
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if (this.getBlock() instanceof BlockWithTooltip appender) {
            appender.appendTooltip(stack, context, displayComponent, textConsumer, type);
        }

        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
