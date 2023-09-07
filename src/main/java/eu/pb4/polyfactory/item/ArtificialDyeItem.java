package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.item.util.FireworkStarColoredItem;
import eu.pb4.polyfactory.item.util.ModeledItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignChangingItem;
import net.minecraft.nbt.NbtElement;
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
import java.util.Locale;

public class ArtificialDyeItem extends ModeledItem implements SignChangingItem, FireworkStarColoredItem {
    public static final ThreadLocal<List<ItemStack>> CURRENT_DYES = ThreadLocal.withInitial(ArrayList::new);

    public ArtificialDyeItem(Settings settings) {
        super(Items.FIREWORK_STAR, settings);
    }


    public static ItemStack of(int rgb) {
        var stack = new ItemStack(FactoryItems.ARTIFICIAL_DYE);
        stack.getOrCreateNbt().putInt("color", rgb);
        return stack;
    }

    public static int getColor(ItemStack stack) {
        if (hasColor(stack)) {
            return stack.getNbt().getInt("color");
        }

        return 0xFFFFFF;
    }

    @Override
    public int getItemColor(ItemStack stack) {
        return getColor(stack);
    }

    public static boolean hasColor(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains("color", NbtElement.NUMBER_TYPE);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (hasColor(stack)) {
            tooltip.add(Text.translatable("item.color", String.format(Locale.ROOT, "#%06X", getColor(stack))).formatted(Formatting.GRAY));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }


    @Override
    public boolean useOnSign(World world, SignBlockEntity signBlockEntity, boolean front, PlayerEntity player) {
        if (signBlockEntity.changeText((text) -> {
            var itemInHand = player.getStackInHand(Hand.MAIN_HAND).isOf(FactoryItems.ARTIFICIAL_DYE) ? player.getMainHandStack() : player.getOffHandStack();
            var color = getColor(itemInHand);
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
                            Math.abs(ColorHelper.Argb.getRed(rgb) - ColorHelper.Argb.getRed(color)) +
                                    Math.abs(ColorHelper.Argb.getGreen(rgb) - ColorHelper.Argb.getGreen(color)) +
                                    Math.abs(ColorHelper.Argb.getBlue(rgb) - ColorHelper.Argb.getBlue(color))
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
