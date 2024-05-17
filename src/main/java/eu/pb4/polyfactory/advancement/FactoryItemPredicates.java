package eu.pb4.polyfactory.advancement;

import eu.pb4.factorytools.api.util.ExtraItemPredicates;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.util.Identifier;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactoryItemPredicates {
    public static Identifier CUSTOM_FIREWORK_COLOR = id("custom_firework_color");


    public static void register() {
        ExtraItemPredicates.PREDICATES.put(CUSTOM_FIREWORK_COLOR, stack -> {
            var nbt = stack.get(DataComponentTypes.FIREWORK_EXPLOSION);
            if (nbt == null) {
                return false;
            }

            for (var c : nbt.colors()) {
                if (!DyeColorExtra.BY_COLOR.containsKey(c.intValue())) {
                    return true;
                }
            }

            for (var c : nbt.fadeColors()) {
                if (!DyeColorExtra.BY_COLOR.containsKey(c.intValue())) {
                    return true;
                }
            }

            return false;
        });
    }

}
