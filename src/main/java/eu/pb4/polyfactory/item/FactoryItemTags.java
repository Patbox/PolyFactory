package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class FactoryItemTags {
    public static final TagKey<Item> ALLOWED_IN_MINER = of("allowed_in_miner");

    private static TagKey<Item> of(String path) {
        return TagKey.of(RegistryKeys.ITEM, ModInit.id(path));
    }
}
