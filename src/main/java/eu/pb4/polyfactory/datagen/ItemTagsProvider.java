package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.item.FactoryItemIds;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.references.ItemIds;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagsProvider extends FabricTagsProvider.ItemTagsProvider {
    public ItemTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture, @Nullable FabricTagsProvider.BlockTagsProvider blockTagsProvider) {
        super(output, registriesFuture, blockTagsProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.tag(FactoryItemTags.ALLOWED_IN_MINER)
                .addOptionalTag(ConventionalItemTags.TOOLS)
                .add(FactoryItemIds.STEEL_GEAR)
                .addOptionalTag(FactoryItemTags.DRILL_HEADS)
        ;

        this.tag(FactoryItemTags.DRILL_HEADS)
                .add(FactoryItemIds.IRON_DRILL_HEAD)
                .add(FactoryItemIds.COPPER_DRILL_HEAD)
                .add(FactoryItemIds.GOLDEN_DRILL_HEAD)
                .add(FactoryItemIds.DIAMOND_DRILL_HEAD)
                .add(FactoryItemIds.NETHERITE_DRILL_HEAD)
        ;

        this.tag(ItemTags.DURABILITY_ENCHANTABLE)
                .addOptionalTag(FactoryItemTags.DRILL_HEADS);
        this.tag(ItemTags.MINING_ENCHANTABLE)
                .addOptionalTag(FactoryItemTags.DRILL_HEADS);
        this.tag(ItemTags.MINING_LOOT_ENCHANTABLE)
                .addOptionalTag(FactoryItemTags.DRILL_HEADS);

        this.tag(FactoryItemTags.ALLOWED_IN_PLANTER)
                .addOptionalTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                .addOptionalTag(ItemTags.SAPLINGS)
                .addOptionalTag(ConventionalItemTags.SEEDS)
        ;

        this.tag(ConventionalItemTags.DYES)
                .add(FactoryItemIds.ARTIFICIAL_DYE)
        ;

        this.tag(ConventionalItemTags.RANGED_WEAPON_TOOLS)
                .add(FactoryItemIds.PRESSURE_FLUID_GUN)
        ;

        this.tag(ConventionalItemTags.NUGGETS)
                .add(FactoryItemIds.STEEL_NUGGET)
        ;
        this.tag(ConventionalItemTags.INGOTS)
                .add(FactoryItemIds.STEEL_INGOT)
        ;

        this.tag(ItemTags.DURABILITY_ENCHANTABLE)
                .add(FactoryItemIds.PRESSURE_FLUID_GUN)
        ;

        this.tag(ItemTags.VANISHING_ENCHANTABLE)
                .add(FactoryItemIds.PRESSURE_FLUID_GUN)
        ;

        this.tag(ItemTags.CAULDRON_CAN_REMOVE_DYE)
                .add(FactoryItemIds.WINDMILL_SAIL);

        var x = this.tag(FactoryItemTags.ROOT_ADVANCEMENT);
        for (var item : BuiltInRegistries.ITEM.registryKeySet()) {
            if (item.identifier().getNamespace().equals(ModInit.ID)) {
                x.add(item);
            }
        }

        this.tag(FactoryItemTags.PLACER_USABLE)
                .addOptionalTag(ConventionalItemTags.TOOLS)
                .addOptionalTag(ConventionalItemTags.EMPTY_BUCKETS)
                .addOptionalTag(ConventionalItemTags.WATER_BUCKETS)
                .addOptionalTag(ConventionalItemTags.LAVA_BUCKETS)
                .addOptionalTag(ConventionalItemTags.BUCKETS)
                .addOptionalTag(ConventionalItemTags.DYES)
                .add(ItemIds.SHEARS)
                .add(ItemIds.BOWL)
                .add(FactoryItemIds.WINDMILL_SAIL, FactoryItemIds.STEEL_GEAR, FactoryItemIds.LARGE_STEEL_GEAR)
                .add(FactoryItemIds.PORTABLE_REDSTONE_TRANSMITTER, FactoryItemIds.SPRAY_CAN, FactoryItemIds.DYNAMITE)
                .add(ItemIds.END_CRYSTAL, ItemIds.ENDER_EYE, ItemIds.EXPERIENCE_BOTTLE, ItemIds.GLASS_BOTTLE, ItemIds.POTION, ItemIds.WHEAT)
        ;

        this.tag(FactoryItemTags.XP_CONVERSION_CATALYST)
                .add(ItemIds.ECHO_SHARD);
        this.tag(FactoryItemTags.DRAIN_CATALYST)
                .addOptionalTag(FactoryItemTags.XP_CONVERSION_CATALYST);

        this.tag(FactoryItemTags.PIPES)
                .add(FactoryItemIds.PIPE);

        for (var self : FactoryItemIds.MOLDS) {
            this.tag(self.tag()).add(self.hardened()).add(self.mold());
            this.tag(FactoryItemTags.MOLDS).addOptionalTag(self.tag());
            this.tag(FactoryItemTags.SHAPEABLE_CLAY_MOLDS).add(self.clay());
        }

        this.tag(FactoryItemTags.SPOUT_ITEM_HORIZONTAL).addOptionalTag(FactoryItemTags.MOLDS);
        this.tag(FactoryItemTags.CASTING_SMALL_FLUID).addOptionalTag(FactoryItemTags.MOLDS);

        this.tag(FactoryItemTags.MOLD_SHAPE_BOTTLE)
                .add(ItemIds.GLASS_BOTTLE)
                .add(ItemIds.POTION)
                .add(ItemIds.EXPERIENCE_BOTTLE)
                .add(ItemIds.HONEY_BOTTLE)
        ;

        this.tag(FactoryItemTags.MOLD_SHAPE_THROWABLE_BOTTLE)
                .add(ItemIds.SPLASH_POTION)
                .add(FactoryItemIds.THROWABLE_GLASS_BOTTLE)
        ;

        this.tag(FactoryItemTags.MOLD_SHAPE_BRITTLE_BOTTLE)
                .add(ItemIds.OMINOUS_BOTTLE)
                .add(FactoryItemIds.BRITTLE_GLASS_BOTTLE)
                .add(FactoryItemIds.BRITTLE_POTION)
        ;

        this.tag(FactoryItemTags.GLASS_BOTTLE_MELTABLE)
                .add(ItemIds.GLASS_BOTTLE)
                .add(FactoryItemIds.THROWABLE_GLASS_BOTTLE)
                .add(FactoryItemIds.BRITTLE_GLASS_BOTTLE)
                .add(FactoryItemIds.LINGERING_THROWABLE_GLASS_BOTTLE)
        ;
    }


    public TagAppender<Item> tag(TagKey<Item> tag) {
        return super.tag(tag);
    }
}
