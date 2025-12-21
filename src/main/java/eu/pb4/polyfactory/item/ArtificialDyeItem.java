package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class ArtificialDyeItem extends Item implements SignApplicator, SimpleColoredItem, ColoredItem {
    public static final ThreadLocal<List<ItemStack>> CURRENT_DYES = ThreadLocal.withInitial(ArrayList::new);

    public ArtificialDyeItem(Properties settings) {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, tooltip, type);
        if (ColoredItem.hasColor(stack)) {
            tooltip.accept(Component.translatable("item.color", ColoredItem.getHexName(ColoredItem.getColor(stack))).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean tryApplyToSign(Level world, SignBlockEntity signBlockEntity, boolean front, Player player) {
        if (signBlockEntity.updateText((text) -> {
            var itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND).is(FactoryItems.ARTIFICIAL_DYE) ? player.getMainHandItem() : player.getOffhandItem();
            var color = ColoredItem.getColor(itemInHand);
            {
                var current = text.getMessage(0, false).getStyle().getColor();
                if (current != null && current.getValue() == color) {
                    return text;
                }
            }
            DyeColor dyeColor = DyeColor.WHITE;



            {
                double distance = Float.MAX_VALUE;

                for (var possibleColor : DyeColor.values()) {
                    var rgb = DyeColorExtra.getColor(possibleColor);

                    var possibleDistance = Math.sqrt(
                            Math.abs(ARGB.red(rgb) - ARGB.red(color)) +
                                    Math.abs(ARGB.green(rgb) - ARGB.green(color)) +
                                    Math.abs(ARGB.blue(rgb) - ARGB.blue(color))
                    );

                    if (possibleDistance < distance) {
                        dyeColor = possibleColor;
                        distance = possibleDistance;
                    }
                }

            }
            for (int i = 0; i < 4; i++) {
                text = text.setMessage(i, text.getMessage(i, false).copy().withStyle(x -> x.withColor(color)));
            }

            return text.setColor(dyeColor);
        }, front)) {
            world.playSound(null, signBlockEntity.getBlockPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}
