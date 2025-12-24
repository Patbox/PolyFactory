package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.datagen.ItemTagsProvider;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static eu.pb4.polyfactory.ModInit.id;

public record SpoutMolds(Identifier name, Item clay, Item hardened, Item mold, TagKey<Item> tag) {
    public static SpoutMolds create(String name) {
        return create(id(name));
    }

    public static SpoutMolds create(Identifier name) {
        return new SpoutMolds(name,
                FactoryItems.register(name.withPrefix("mold/").withSuffix("_clay"),
                        (s) -> new SimplePolymerItem(s.stacksTo(1))),
                FactoryItems.register(name.withPrefix("mold/").withSuffix("_hardened"),
                        (s) -> new SimplePolymerItem(s.durability(64))),
                FactoryItems.register(name.withPrefix("mold/"),
                        (s) -> new SimplePolymerItem(s.stacksTo(1))),
                TagKey.create(Registries.ITEM, name.withPrefix("mold/"))
        );
    }

    public void createTag(ItemTagsProvider itemTagsProvider) {
        itemTagsProvider.getOrCreateTagBuilder(tag).add(hardened, mold);
        itemTagsProvider.getOrCreateTagBuilder(FactoryItemTags.MOLDS).addOptionalTag(tag);
        itemTagsProvider.getOrCreateTagBuilder(FactoryItemTags.SHAPEABLE_CLAY_MOLDS).add(clay);
    }
}
