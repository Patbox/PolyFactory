package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.datagen.ItemTagsProvider;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import static eu.pb4.polyfactory.ModInit.id;

public record SpoutMolds(Identifier name, Item clay, Item hardened, Item mold, TagKey<Item> tag) {
    public static SpoutMolds create(String name) {
        return create(id(name));
    }

    public static SpoutMolds create(Identifier name) {
        return new SpoutMolds(name,
                FactoryItems.register(name.withPrefixedPath("mold/").withSuffixedPath("_clay"),
                        (s) -> new SimplePolymerItem(s.maxCount(1))),
                FactoryItems.register(name.withPrefixedPath("mold/").withSuffixedPath("_hardened"),
                        (s) -> new SimplePolymerItem(s.maxDamage(16))),
                FactoryItems.register(name.withPrefixedPath("mold/"),
                        (s) -> new SimplePolymerItem(s.maxCount(1))),
                TagKey.of(RegistryKeys.ITEM, name.withPrefixedPath("mold/"))
        );
    }

    public void createTag(ItemTagsProvider itemTagsProvider) {
        itemTagsProvider.getOrCreateTagBuilder(tag).add(hardened, mold);
        itemTagsProvider.getOrCreateTagBuilder(FactoryItemTags.MOLDS).addOptionalTag(tag);
    }
}
