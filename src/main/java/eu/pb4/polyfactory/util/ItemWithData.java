package eu.pb4.polyfactory.util;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registries;

public record ItemWithData(Item item, ComponentPredicate predicate) {
    public static ItemWithData of(Item item) {
        return new ItemWithData(item, ComponentPredicate.EMPTY);
    }

    public static ItemWithData of(Item item, ComponentMap map) {
        return new ItemWithData(item, ComponentPredicate.of(map));
    }

    public ItemStack createStack() {
        return new ItemStack(Registries.ITEM.getEntry(item), 1, predicate.toChanges());
    }
}
