package eu.pb4.factorytools.api.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.function.Predicate;

public class ExtraItemPredicates {
    public static HashMap<Identifier, Predicate<ItemStack>> PREDICATES = new HashMap<>();

    public static void register() {

    }

}
