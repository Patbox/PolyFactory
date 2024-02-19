package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class FactoryItemTags {
    public static final TagKey<Item> ALLOWED_IN_MINER = of("allowed_in_miner");
    public static final TagKey<Item> ALLOWED_IN_PLANTER = of("allowed_in_planter");
    public static final TagKey<Item> STRIPPED_LOGS = of("stripped_logs");
    public static final TagKey<Item> ROOT_ADVANCEMENT = of("root_advancement");
    public static final TagKey<Item> PLACER_USABLE = of("placer_usable");

    private static TagKey<Item> of(String path) {
        return TagKey.of(RegistryKeys.ITEM, ModInit.id(path));
    }
}
