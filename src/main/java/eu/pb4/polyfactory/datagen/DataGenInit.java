package eu.pb4.polyfactory.datagen;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.machines.GrinderBlock;
import eu.pb4.polyfactory.block.machines.PressBlock;
import eu.pb4.polyfactory.block.mechanical.WindmillBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.PressTemplateItem;
import eu.pb4.polyfactory.loottable.IntStateLootNumberProvider;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.PressRecipe;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DataGenInit implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        pack.addProvider(BlockTagsProvider::new);
        pack.addProvider(LootTables::new);
        pack.addProvider(Recipes::new);
    }

    class BlockTagsProvider extends FabricTagProvider.BlockTagProvider {
        public BlockTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup arg) {
            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYORS)
                    .add(FactoryBlocks.CONVEYOR)
                    .add(FactoryBlocks.STICKY_CONVEYOR);

            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)
                    .add(FactoryBlocks.FUNNEL).add(FactoryBlocks.SPLITTER);

            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                    .addOptionalTag(FactoryBlockTags.CONVEYORS)
                    .add(FactoryBlocks.PRESS)
                    .add(Blocks.HOPPER);

            this.getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                    .addOptionalTag(FactoryBlockTags.CONVEYORS)
                    .add(FactoryBlocks.FAN, FactoryBlocks.PRESS, FactoryBlocks.FUNNEL, FactoryBlocks.GRINDER, FactoryBlocks.MINER, FactoryBlocks.SPLITTER)
            ;

            this.getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                    .add(FactoryBlocks.WINDMILL)
                    .add(FactoryBlocks.AXLE)
                    .add(FactoryBlocks.GEARBOX)
                    .add(FactoryBlocks.CONTAINER)
                    .add(FactoryBlocks.FUNNEL)
            ;
        }
    }

    class LootTables extends FabricBlockLootTableProvider {
        protected LootTables(FabricDataOutput dataOutput) {
            super(dataOutput);
        }

        @Override
        public void generate() {
            this.addDrop(FactoryBlocks.SPLITTER);
            this.addDrop(FactoryBlocks.MINER);
            this.addDrop(FactoryBlocks.FAN);
            this.addDrop(FactoryBlocks.GRINDER, (block) -> this.dropsWithProperty(block, GrinderBlock.PART, GrinderBlock.Part.MAIN));
            this.addDrop(FactoryBlocks.PRESS, (block) -> this.dropsWithProperty(block, PressBlock.PART, PressBlock.Part.MAIN));
            this.addDrop(FactoryBlocks.HAND_CRANK);
            this.addDrop(FactoryBlocks.CONVEYOR);
            this.addDrop(FactoryBlocks.STICKY_CONVEYOR);
            this.addDrop(FactoryBlocks.MOTOR);
            this.addDrop(FactoryBlocks.CABLE_PLATE);
            this.addDrop(FactoryBlocks.FUNNEL);
            this.addDrop(FactoryBlocks.AXLE);
            this.addDrop(FactoryBlocks.GEARBOX);
            this.addDrop(FactoryBlocks.CONTAINER);
            this.addDrop(FactoryBlocks.WINDMILL, FactoryItems.AXLE_BLOCK);
        }
    }

    class Recipes extends FabricRecipeProvider {
        public Recipes(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generate(Consumer<RecipeJsonProvider> exporter) {
            of(exporter, GrindingRecipe.CODEC,
                    GrindingRecipe.of("stone_to_cobblestone", Ingredient.ofItems(Items.STONE), 2, 0.2, Items.COBBLESTONE),
                    GrindingRecipe.of("cobblestone_to_gravel", Ingredient.ofItems(Items.COBBLESTONE), 6.5, 0.2, Items.GRAVEL),
                    GrindingRecipe.of("gravel_to_sand", Ingredient.ofItems(Items.GRAVEL), 6.5, 0.2, Items.SAND),
                    GrindingRecipe.of("planks_to_sticks", Ingredient.fromTag(ItemTags.PLANKS), 4, 0.05, GrindingRecipe.Output.of(Items.STICK, 0.4f, 6)),

                    GrindingRecipe.of("iron_ingot_to_nuggets", Ingredient.ofItems(Items.IRON_INGOT), 3, 0.4, new ItemStack(Items.IRON_NUGGET, 9)),
                    GrindingRecipe.of("gold_ingot_to_nuggets", Ingredient.ofItems(Items.GOLD_INGOT), 3, 0.4, new ItemStack(Items.GOLD_NUGGET, 9)),

                    GrindingRecipe.of("iron_ore_to_raw", Ingredient.fromTag(ItemTags.IRON_ORES), 16, 0.5,
                            GrindingRecipe.Output.of(Items.RAW_IRON, 1f, 1), GrindingRecipe.Output.of(Items.RAW_IRON, 0.6f, 5)
                    ),
                    GrindingRecipe.of("gold_ore_to_raw", Ingredient.fromTag(ItemTags.GOLD_ORES), 16, 0.5,
                            GrindingRecipe.Output.of(Items.RAW_GOLD, 1f, 1), GrindingRecipe.Output.of(Items.RAW_GOLD, 0.6f, 5)
                    ),
                    GrindingRecipe.of("copper_ore_to_raw", Ingredient.fromTag(ItemTags.COPPER_ORES), 16, 0.5,
                            GrindingRecipe.Output.of(Items.RAW_COPPER, 1f, 2), GrindingRecipe.Output.of(Items.RAW_COPPER, 0.6f, 20)
                    ),
                    GrindingRecipe.of("bone_to_bone_meal", Ingredient.ofItems(Items.BONE), 2, GrindingRecipe.Output.of(Items.BONE_MEAL, 1, 3), GrindingRecipe.Output.of(Items.BONE_MEAL, 0.5f, 2)),

                    // Flower to Dye
                    GrindingRecipe.of("dandelion_to_dye", Ingredient.ofItems(Items.DANDELION), 2, new ItemStack(Items.YELLOW_DYE, 3)),
                    GrindingRecipe.of("orchid_to_dye", Ingredient.ofItems(Items.BLUE_ORCHID), 2, new ItemStack(Items.LIGHT_BLUE_DYE, 3)),
                    GrindingRecipe.of("allium_to_dye", Ingredient.ofItems(Items.ALLIUM), 2, new ItemStack(Items.MAGENTA_DYE, 3)),
                    GrindingRecipe.of("azure_bluet_to_dye", Ingredient.ofItems(Items.AZURE_BLUET), 2, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("daisy_to_dye", Ingredient.ofItems(Items.OXEYE_DAISY), 2, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("red_tulip_to_dye", Ingredient.ofItems(Items.RED_TULIP), 2, new ItemStack(Items.RED_DYE, 3)),
                    GrindingRecipe.of("orange_tulip_to_dye", Ingredient.ofItems(Items.ORANGE_TULIP), 2, new ItemStack(Items.ORANGE_DYE, 3)),
                    GrindingRecipe.of("white_tulip_to_dye", Ingredient.ofItems(Items.WHITE_TULIP), 2, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("pink_tulip_to_dye", Ingredient.ofItems(Items.PINK_TULIP), 2, new ItemStack(Items.PINK_DYE, 3)),
                    GrindingRecipe.of("cornflower_to_dye", Ingredient.ofItems(Items.CORNFLOWER), 2, new ItemStack(Items.BLUE_DYE, 3)),
                    GrindingRecipe.of("lily_to_dye", Ingredient.ofItems(Items.LILY_OF_THE_VALLEY), 2, new ItemStack(Items.WHITE_DYE, 3)),
                    GrindingRecipe.of("wither_rose_to_dye", Ingredient.ofItems(Items.WITHER_ROSE), 2, new ItemStack(Items.BLACK_DYE, 3)),
                    GrindingRecipe.of("sunflower_to_dye", Ingredient.ofItems(Items.SUNFLOWER), 2, new ItemStack(Items.YELLOW_DYE, 6)),
                    GrindingRecipe.of("lilac_to_dye", Ingredient.ofItems(Items.LILAC), 2, new ItemStack(Items.MAGENTA_DYE, 6)),
                    GrindingRecipe.of("peony_to_dye", Ingredient.ofItems(Items.PEONY), 2, new ItemStack(Items.PINK_DYE, 6)),
                    GrindingRecipe.of("rose_to_dye", Ingredient.ofItems(Items.ROSE_BUSH), 2, new ItemStack(Items.RED_DYE, 6)),
                    GrindingRecipe.of("cactus_to_dye", Ingredient.ofItems(Items.CACTUS), 2, new ItemStack(Items.GREEN_DYE, 3))
            );

            ShapelessRecipeJsonBuilder.create(RecipeCategory.TOOLS, FactoryItems.STEEL_ALLOY_MIXTURE)
                    .input(Items.IRON_INGOT, 2).input(Items.COAL).input(Items.REDSTONE)
                    .criterion("get_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                    .offerTo(exporter);

            CookingRecipeJsonBuilder.createSmelting(
                    Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 80)
                    .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                    .offerTo(exporter);


            of(exporter, PressRecipe.CODEC,
                    PressRecipe.of("iron_ingot", Ingredient.ofItems(Items.IRON_NUGGET), 9, PressTemplateItem.DEFAULT_TYPE, 0.4f, Items.IRON_INGOT),
                    PressRecipe.of("gold_ingot", Ingredient.ofItems(Items.GOLD_NUGGET), 9, PressTemplateItem.DEFAULT_TYPE, 0.4f, Items.GOLD_INGOT),
                    PressRecipe.of("steel_plate", Ingredient.ofItems(FactoryItems.STEEL_INGOT), 1, PressTemplateItem.DEFAULT_TYPE, 0.5f, new ItemStack(FactoryItems.STEEL_PLATE, 2))
            );

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.STEEL_COG)
                    .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .pattern(" p ")
                    .pattern("pip")
                    .pattern(" p ")
                    .input('p', FactoryItems.STEEL_PLATE)
                    .input('i', FactoryItems.STEEL_INGOT)
                    .offerTo(exporter);

        }

        public <T extends Recipe<?>> void of(Consumer<RecipeJsonProvider> exporter, Codec<T> codec, T... recipes) {
            for (var recipe : recipes) {
                exporter.accept(new CodecRecipeJsonProvider<>(codec, recipe));
            }
        }
    }
}
