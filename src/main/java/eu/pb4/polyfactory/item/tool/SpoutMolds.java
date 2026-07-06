package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryItemIds;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static eu.pb4.polyfactory.ModInit.id;

public record SpoutMolds<T>(Identifier name, T clay, T hardened, T mold, TagKey<Item> tag) {
    public static SpoutMolds<ResourceKey<Item>> createIds(String name) {
        return createIds(id(name));
    }

    public static SpoutMolds<ResourceKey<Item>> createIds(Identifier name) {
        return new SpoutMolds<>(name,
                FactoryItemIds.of(name.withPrefix("mold/").withSuffix("_clay")),
                FactoryItemIds.of(name.withPrefix("mold/").withSuffix("_hardened")),
                FactoryItemIds.of(name.withPrefix("mold/")),
                TagKey.create(Registries.ITEM, name.withPrefix("mold/"))
        );
    }

    public static SpoutMolds<Item> registerItems(SpoutMolds<ResourceKey<Item>> keys) {
        return new SpoutMolds<>(keys.name,
                FactoryItems.register(keys.clay, (s) -> new SimplePolymerItem(s.stacksTo(1))),
                FactoryItems.register(keys.hardened, (s) -> new SimplePolymerItem(s.durability(64))),
                FactoryItems.register(keys.mold, (s) -> new SimplePolymerItem(s.stacksTo(1))),
                keys.tag
        );
    }
}
