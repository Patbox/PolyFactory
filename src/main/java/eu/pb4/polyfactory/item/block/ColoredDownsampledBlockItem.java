package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

import java.util.List;

public class ColoredDownsampledBlockItem extends FactoryBlockItem implements FireworkStarColoredItem, ColoredItem {
    private final int defaultColor;

    public <T extends Block & PolymerBlock> ColoredDownsampledBlockItem(T block, int defaultColor, Settings settings) {
        super(block, settings, Items.FIREWORK_STAR);
        this.defaultColor = defaultColor;
    }

    @Override
    public Text getName(ItemStack stack) {
        if (ColoredItem.hasColor(stack)) {
            return getColoredName(stack, ColoredItem.getColor(stack));
        }

        return super.getName(stack);
    }

    protected Text getColoredName(ItemStack stack, int color) {
        return Text.translatable( this.getTranslationKey() + ".colored", ColoredItem.getColorName(ColoredItem.getColor(stack)));
    }

    protected Text getColorlessName(ItemStack stack) {
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (ColoredItem.hasColor(stack) && !DyeColorExtra.hasLang(ColoredItem.getColor(stack))) {
            tooltip.add(Text.translatable("item.color", ColoredItem.getHexName(ColoredItem.getColor(stack))).formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public int downSampleColor(int color, boolean isVanilla) {
        if (isVanilla || color == this.defaultColor) {
            return color;
        }

        var r = ColorHelper.Argb.getRed(color) & 0b11110000;
        var g = ColorHelper.Argb.getGreen(color) & 0b11110000;
        var b = ColorHelper.Argb.getBlue(color) & 0b11110000;

        var full = r + g + b;
        if (full < (1 << 5) * 2) {
            r += 1 << 5;
            g += 1 << 5;
            b += 1 << 5;
        }

        return ColorHelper.Argb.getArgb(0, r, g, b);
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
