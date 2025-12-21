package eu.pb4.polyfactory.advancement;

import eu.pb4.factorytools.api.util.ExtraItemPredicates;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactoryItemPredicates {
    public static Identifier CUSTOM_FIREWORK_COLOR = id("custom_firework_color");
    public static Identifier HAS_FLUIDS = id("has_fluids");


    public static void register() {
        ExtraItemPredicates.PREDICATES.put(CUSTOM_FIREWORK_COLOR, stack -> {
            var nbt = stack.get(DataComponents.FIREWORK_EXPLOSION);
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

        ExtraItemPredicates.PREDICATES.put(HAS_FLUIDS, stack -> {
            var nbt = stack.get(FactoryDataComponents.FLUID);
            if (nbt == null) {
                return false;
            }

            return !nbt.isEmpty();
        });
    }

}
