package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.tag.ProvidedTagBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagsProvider extends FabricTagProvider.ItemTagProvider {
    public ItemTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture, @Nullable FabricTagProvider.BlockTagProvider blockTagProvider) {
        super(output, registriesFuture, blockTagProvider);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        this.getOrCreateTagBuilder(FactoryItemTags.ALLOWED_IN_MINER)
                .addOptionalTag(ConventionalItemTags.TOOLS)
                .add(FactoryItems.STEEL_GEAR)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.ALLOWED_IN_PLANTER)
                .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                .addOptionalTag(ItemTags.SAPLINGS)
                .addOptionalTag(ConventionalItemTags.SEEDS)
        ;

        this.getOrCreateTagBuilder(ConventionalItemTags.DYES)
                .add(FactoryItems.ARTIFICIAL_DYE)
        ;

        this.getOrCreateTagBuilder(ConventionalItemTags.RANGED_WEAPON_TOOLS)
                .add(FactoryItems.PRESSURE_FLUID_GUN)
        ;

        this.getOrCreateTagBuilder(ConventionalItemTags.NUGGETS)
                .add(FactoryItems.COPPER_NUGGET)
                .add(FactoryItems.STEEL_NUGGET)
        ;
        this.getOrCreateTagBuilder(ConventionalItemTags.INGOTS)
                .add(FactoryItems.STEEL_INGOT)
        ;

        this.getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .add(FactoryItems.PRESSURE_FLUID_GUN)
        ;

        this.getOrCreateTagBuilder(ItemTags.VANISHING_ENCHANTABLE)
                .add(FactoryItems.PRESSURE_FLUID_GUN)
        ;

        this.getOrCreateTagBuilder(ItemTags.DYEABLE)
                .add(FactoryItems.WINDMILL_SAIL)
        ;

        var x = this.getOrCreateTagBuilder(FactoryItemTags.ROOT_ADVANCEMENT);
        for (var item : Registries.ITEM.getIds()) {
            if (item.getNamespace().equals(ModInit.ID)) {
                x.add(Registries.ITEM.get(item));
            }
        }

        this.getOrCreateTagBuilder(FactoryItemTags.PLACER_USABLE)
                .addOptionalTag(ConventionalItemTags.TOOLS)
                .addOptionalTag(ConventionalItemTags.EMPTY_BUCKETS)
                .addOptionalTag(ConventionalItemTags.WATER_BUCKETS)
                .addOptionalTag(ConventionalItemTags.LAVA_BUCKETS)
                .addOptionalTag(ConventionalItemTags.BUCKETS)
                .addOptionalTag(ConventionalItemTags.DYES)
                .add(Items.SHEARS)
                .add(Items.BOWL)
                .add(FactoryItems.WINDMILL_SAIL, FactoryItems.STEEL_GEAR, FactoryItems.LARGE_STEEL_GEAR)
                .add(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, FactoryItems.SPRAY_CAN, FactoryItems.DYNAMITE)
                .add(Items.END_CRYSTAL, Items.ENDER_EYE, Items.EXPERIENCE_BOTTLE, Items.GLASS_BOTTLE, Items.POTION, Items.WHEAT)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.XP_CONVERSION_CATALYST)
                .add(Items.ECHO_SHARD);
        this.getOrCreateTagBuilder(FactoryItemTags.DRAIN_CATALYST)
                .addOptionalTag(FactoryItemTags.XP_CONVERSION_CATALYST);

        this.getOrCreateTagBuilder(FactoryItemTags.PIPES)
                .add(FactoryItems.PIPE);

        for (var mold : FactoryItems.MOLDS) {
            mold.createTag(this);
        }

        this.getOrCreateTagBuilder(FactoryItemTags.SPOUT_ITEM_HORIZONTAL).addOptionalTag(FactoryItemTags.MOLDS);
        this.getOrCreateTagBuilder(FactoryItemTags.CASTING_SMALL_FLUID).addOptionalTag(FactoryItemTags.MOLDS);

        this.getOrCreateTagBuilder(FactoryItemTags.MOLD_SHAPE_BOTTLE)
                .add(Items.GLASS_BOTTLE)
                .add(Items.POTION)
                .add(Items.EXPERIENCE_BOTTLE)
                .add(Items.HONEY_BOTTLE)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.MOLD_SHAPE_THROWABLE_BOTTLE)
                .add(Items.SPLASH_POTION)
                .add(FactoryItems.THROWABLE_GLASS_BOTTLE)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.MOLD_SHAPE_BRITTLE_BOTTLE)
                .add(Items.OMINOUS_BOTTLE)
                .add(FactoryItems.BRITTLE_GLASS_BOTTLE)
                .add(FactoryItems.BRITTLE_POTION)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.GLASS_BOTTLE_MELTABLE)
                .add(Items.GLASS_BOTTLE)
                .add(FactoryItems.THROWABLE_GLASS_BOTTLE)
                .add(FactoryItems.BRITTLE_GLASS_BOTTLE)
                .add(FactoryItems.LINGERING_THROWABLE_GLASS_BOTTLE)
        ;
    }


    public ProvidedTagBuilder<Item, Item> getOrCreateTagBuilder(TagKey<Item> tag) {
        return super.valueLookupBuilder(tag);
    }
}
