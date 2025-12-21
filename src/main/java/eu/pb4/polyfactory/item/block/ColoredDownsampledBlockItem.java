package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ColoredDownsampledBlockItem extends FactoryBlockItem implements SimpleColoredItem, ColoredItem {
    private final int defaultColor;

    public <T extends Block & PolymerBlock> ColoredDownsampledBlockItem(T block, int defaultColor, Properties settings) {
        super(block, settings, Items.FIREWORK_STAR);
        this.defaultColor = defaultColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (ColoredItem.hasColor(stack)) {
            return getColoredName(stack, ColoredItem.getColor(stack));
        }

        return super.getName(stack);
    }

    protected Component getColoredName(ItemStack stack, int color) {
        return Component.translatable( this.getDescriptionId() + ".colored", ColoredItem.getColorName(ColoredItem.getColor(stack)));
    }

    protected Component getColorlessName(ItemStack stack) {
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        if (ColoredItem.hasColor(stack) && !DyeColorExtra.hasLang(ColoredItem.getColor(stack))) {
            tooltip.accept(Component.translatable("item.color", ColoredItem.getHexName(ColoredItem.getColor(stack))).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int downSampleColor(int color, boolean isVanilla) {
        if (isVanilla || color == this.defaultColor) {
            return color;
        }

        var r = ARGB.red(color) & 0b11110000;
        var g = ARGB.green(color) & 0b11110000;
        var b = ARGB.blue(color) & 0b11110000;

        var full = r + g + b;
        if (full < (1 << 5) * 2) {
            r += 1 << 5;
            g += 1 << 5;
            b += 1 << 5;
        }

        return ARGB.color(0, r, g, b);
    }

    @Override
    public int getItemColor(ItemStack stack) {
        return ColoredItem.getColor(stack);
    }

    @Override
    public int getDefaultColor() {
        return this.defaultColor;
    }
}
