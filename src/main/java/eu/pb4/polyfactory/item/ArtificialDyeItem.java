package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignChangingItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArtificialDyeItem extends Item implements SignChangingItem, SimpleColoredItem, ColoredItem {
    public static final ThreadLocal<List<ItemStack>> CURRENT_DYES = ThreadLocal.withInitial(ArrayList::new);

    public ArtificialDyeItem(Settings settings) {
        super(settings);
    }


    public static ItemStack of(int rgb) {
        var stack = new ItemStack(FactoryItems.ARTIFICIAL_DYE);
        ColoredItem.setColor(stack, rgb);
        return stack;
    }

    @Override
    public int getItemColor(ItemStack stack) {
        return ColoredItem.getColor(stack);
    }

    @Override
    public int getDefaultColor() {
        return 0xFFFFFF;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (ColoredItem.hasColor(stack)) {
            tooltip.add(Text.translatable("item.color", ColoredItem.getHexName(ColoredItem.getColor(stack))).formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public boolean useOnSign(World world, SignBlockEntity signBlockEntity, boolean front, PlayerEntity player) {
        if (signBlockEntity.changeText((text) -> {
            var itemInHand = player.getStackInHand(Hand.MAIN_HAND).isOf(FactoryItems.ARTIFICIAL_DYE) ? player.getMainHandStack() : player.getOffHandStack();
            var color = ColoredItem.getColor(itemInHand);
            {
                var current = text.getMessage(0, false).getStyle().getColor();
                if (current != null && current.getRgb() == color) {
                    return text;
                }
            }
            DyeColor dyeColor = DyeColor.WHITE;



            {
                double distance = Float.MAX_VALUE;

                for (var possibleColor : DyeColor.values()) {
                    var rgb = DyeColorExtra.getColor(possibleColor);

                    var possibleDistance = Math.sqrt(
                            Math.abs(ColorHelper.getRed(rgb) - ColorHelper.getRed(color)) +
                                    Math.abs(ColorHelper.getGreen(rgb) - ColorHelper.getGreen(color)) +
                                    Math.abs(ColorHelper.getBlue(rgb) - ColorHelper.getBlue(color))
                    );

                    if (possibleDistance < distance) {
                        dyeColor = possibleColor;
                        distance = possibleDistance;
                    }
                }

            }
            for (int i = 0; i < 4; i++) {
                text = text.withMessage(i, text.getMessage(i, false).copy().styled(x -> x.withColor(color)));
            }

            return text.withColor(dyeColor);
        }, front)) {
            world.playSound(null, signBlockEntity.getPos(), SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}
