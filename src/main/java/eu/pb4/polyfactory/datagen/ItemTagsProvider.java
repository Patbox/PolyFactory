package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

class ItemTagsProvider extends FabricTagProvider.ItemTagProvider {
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
        ;

        this.getOrCreateTagBuilder(ConventionalItemTags.DYES)
                .add(FactoryItems.ARTIFICIAL_DYE)
        ;

        this.getOrCreateTagBuilder(ItemTags.DYEABLE)
                .add(FactoryItems.WINDMILL_SAIL)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.ROOT_ADVANCEMENT)
                .add(FactoryItems.GRINDER)
                .add(FactoryItems.METAL_GRID)
                .add(FactoryItems.STEEL_GEAR)
                .add(FactoryItems.STEEL_INGOT)
                .add(FactoryItems.STEEL_ALLOY_MIXTURE)
                .add(FactoryItems.CABLE)
                .add(FactoryItems.WINDMILL_SAIL)
                .add(FactoryItems.AXLE)
                ;

        this.getOrCreateTagBuilder(FactoryItemTags.PLACER_USABLE)
                .addOptionalTag(ConventionalItemTags.TOOLS)
                .addOptionalTag(ConventionalItemTags.EMPTY_BUCKETS)
                .addOptionalTag(ConventionalItemTags.WATER_BUCKETS)
                .addOptionalTag(ConventionalItemTags.LAVA_BUCKETS)
                .addOptionalTag(ConventionalItemTags.DYES)
                .add(Items.SHEARS)
                .add(Items.BOWL)
                .add(FactoryItems.WINDMILL_SAIL, FactoryItems.STEEL_GEAR, FactoryItems.LARGE_STEEL_GEAR)
                .add(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, FactoryItems.SPRAY_CAN, FactoryItems.DYNAMITE)
                .add(Items.END_CRYSTAL, Items.ENDER_EYE, Items.EXPERIENCE_BOTTLE, Items.GLASS_BOTTLE, Items.POTION, Items.WHEAT)
        ;

        this.getOrCreateTagBuilder(FactoryItemTags.DRAIN_CATALYST).add(Items.ECHO_SHARD);
        this.getOrCreateTagBuilder(FactoryItemTags.XP_CONVERSION_CATALYST).add(Items.ECHO_SHARD);

        this.copy(FactoryBlockTags.STRIPPED_LOGS, FactoryItemTags.STRIPPED_LOGS);
    }
}
