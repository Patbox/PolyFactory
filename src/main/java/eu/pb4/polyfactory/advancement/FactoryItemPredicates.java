package eu.pb4.polyfactory.advancement;

import eu.pb4.factorytools.api.util.ExtraItemPredicates;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.function.Predicate;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactoryItemPredicates {
    public static Identifier CUSTOM_FIREWORK_COLOR = id("custom_firework_color");


    public static void register() {
        ExtraItemPredicates.PREDICATES.put(CUSTOM_FIREWORK_COLOR, stack -> {
            var nbt = stack.getNbt();
            if (nbt == null) {
                return false;
            }

            if (!nbt.contains(FireworkRocketItem.EXPLOSION_KEY, NbtElement.COMPOUND_TYPE)) {
                return false;
            }

            var exp = nbt.getCompound(FireworkRocketItem.EXPLOSION_KEY);

            if (exp.contains(FireworkRocketItem.COLORS_KEY, NbtElement.INT_ARRAY_TYPE)) {
                for (var c : exp.getIntArray(FireworkRocketItem.COLORS_KEY)) {
                    if (!DyeColorExtra.BY_COLOR.containsKey(c)) {
                        return true;
                    }
                }
            }

            if (exp.contains(FireworkRocketItem.FADE_COLORS_KEY, NbtElement.INT_ARRAY_TYPE)) {
                for (var c : exp.getIntArray(FireworkRocketItem.FADE_COLORS_KEY)) {
                    if (!DyeColorExtra.BY_COLOR.containsKey(c)) {
                        return true;
                    }
                }
            }

            return false;
        });
    }

}
