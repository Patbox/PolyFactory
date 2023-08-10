package eu.pb4.polyfactory.datagen;

import com.google.common.hash.HashCode;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.ConveyorModel;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.mixing.FireworkStarMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryEntityTags;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class DataGenInit implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        var blockTags = pack.addProvider(BlockTagsProvider::new);
        pack.addProvider((a, b) -> new ItemTagsProvider(a, b, blockTags));
        pack.addProvider(EntityTagsProvider::new);
        pack.addProvider(LootTables::new);
        pack.addProvider(Recipes::new);
        pack.addProvider(AssetProvider::new);
    }

    static class AssetProvider implements DataProvider {
        private final DataOutput output;

        public AssetProvider(FabricDataOutput output) {
            this.output = output;
        }

        @Override
        public CompletableFuture<?> run(DataWriter writer) {
            BiConsumer<String, byte[]> assetWriter = (path, data) -> {
                try {
                    writer.write(this.output.getPath().resolve(path), data, HashCode.fromBytes(data));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            return CompletableFuture.runAsync(() -> {
                ConveyorModel.generateModels(assetWriter);
                UiResourceCreator.generateAssets(assetWriter);
            }, Util.getMainWorkerExecutor());
        }

        @Override
        public String getName() {
            return "polyfactory:assets";
        }
    }

    static class BlockTagsProvider extends FabricTagProvider.BlockTagProvider {
        public BlockTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup arg) {
            this.getOrCreateTagBuilder(FactoryBlockTags.STRIPPED_LOGS)
                    .add(Blocks.STRIPPED_OAK_LOG)
                    .add(Blocks.STRIPPED_BIRCH_LOG)
                    .add(Blocks.STRIPPED_SPRUCE_LOG)
                    .add(Blocks.STRIPPED_JUNGLE_LOG)
                    .add(Blocks.STRIPPED_ACACIA_LOG)
                    .add(Blocks.STRIPPED_DARK_OAK_LOG)
                    .add(Blocks.STRIPPED_MANGROVE_LOG)
                    .add(Blocks.STRIPPED_CHERRY_LOG)
                    .add(Blocks.STRIPPED_CRIMSON_STEM)
                    .add(Blocks.STRIPPED_WARPED_STEM)
            ;


            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYORS)
                    .add(FactoryBlocks.CONVEYOR)
                    .add(FactoryBlocks.STICKY_CONVEYOR);

            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)
                    .add(FactoryBlocks.FUNNEL).add(FactoryBlocks.SPLITTER);

            this.getOrCreateTagBuilder(FactoryBlockTags.GRINDER_TOP_PLACEABLE)
                    .add(FactoryBlocks.HAND_CRANK).add(FactoryBlocks.AXLE);

            this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                    .add(FactoryBlocks.PRESS)
                    .add(FactoryBlocks.MIXER)
                    .add(Blocks.HOPPER)
                    .add(Blocks.COMPOSTER)
            ;

            this.getOrCreateTagBuilder(FactoryBlockTags.SPLITTER_SIDE_OUTPUT)
                    .addOptionalTag(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                    .addOptionalTag(FactoryBlockTags.CONVEYORS);

            this.getOrCreateTagBuilder(FactoryBlockTags.WIND_PASSTHROUGH)
                    .addOptionalTag(BlockTags.TRAPDOORS)
                    .addOptionalTag(BlockTags.FENCES)
                    .addOptionalTag(BlockTags.FENCE_GATES)
                    .addOptionalTag(BlockTags.BANNERS)
                    .addOptionalTag(BlockTags.ALL_SIGNS)
                    .add(FactoryBlocks.NIXIE_TUBE)
                    .add(FactoryBlocks.HAND_CRANK)
                    .add(FactoryBlocks.METAL_GRID)
            ;

            this.getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                    .addOptionalTag(FactoryBlockTags.CONVEYORS)
                    .add(FactoryBlocks.FAN,
                            FactoryBlocks.NIXIE_TUBE,
                            FactoryBlocks.PRESS,
                            FactoryBlocks.FUNNEL,
                            FactoryBlocks.GRINDER,
                            FactoryBlocks.MINER,
                            FactoryBlocks.SPLITTER)
                    .add(FactoryBlocks.MIXER)
                    .add(FactoryBlocks.STEAM_ENGINE)
                    .add(FactoryBlocks.METAL_GRID)
            ;

            this.getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                    .add(FactoryBlocks.WINDMILL)
                    .add(FactoryBlocks.AXLE)
                    .add(FactoryBlocks.HAND_CRANK)
                    .add(FactoryBlocks.GEARBOX)
                    .add(FactoryBlocks.CONTAINER)
                    .add(FactoryBlocks.FUNNEL)
            ;
        }
    }

    static class ItemTagsProvider extends FabricTagProvider.ItemTagProvider {
        public ItemTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture, @Nullable FabricTagProvider.BlockTagProvider blockTagProvider) {
            super(output, registriesFuture, blockTagProvider);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup arg) {
            this.getOrCreateTagBuilder(FactoryItemTags.ALLOWED_IN_MINER)
                    .addOptionalTag(ItemTags.TOOLS)
                    .add(FactoryItems.STEEL_COG)
            ;

            this.copy(FactoryBlockTags.STRIPPED_LOGS, FactoryItemTags.STRIPPED_LOGS);
        }
    }

    static class EntityTagsProvider extends FabricTagProvider.EntityTypeTagProvider {
        public EntityTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
            super(output, completableFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup arg) {
            this.getOrCreateTagBuilder(FactoryEntityTags.GRID_PASSABLE)
                    .addOptionalTag(EntityTypeTags.ARROWS)
                    .addOptionalTag(EntityTypeTags.IMPACT_PROJECTILES)
                    .add(EntityType.ITEM)
                    .add(EntityType.EXPERIENCE_ORB)
                    .add(EntityType.EXPERIENCE_BOTTLE)
                    .add(EntityType.POTION)
            ;
        }
    }

    static class LootTables extends FabricBlockLootTableProvider {
        protected LootTables(FabricDataOutput dataOutput) {
            super(dataOutput);
        }

        @Override
        public void generate() {
            this.addDrop(FactoryBlocks.SPLITTER);
            this.addDrop(FactoryBlocks.MINER);
            this.addDrop(FactoryBlocks.FAN);
            this.addDrop(FactoryBlocks.GRINDER);
            this.addDrop(FactoryBlocks.PRESS, (block) -> this.dropsWithProperty(block, PressBlock.PART, PressBlock.Part.MAIN));
            this.addDrop(FactoryBlocks.MIXER, (block) -> this.dropsWithProperty(block, MixerBlock.PART, MixerBlock.Part.MAIN));
            this.addDrop(FactoryBlocks.HAND_CRANK);
            this.addDrop(FactoryBlocks.CONVEYOR);
            this.addDrop(FactoryBlocks.STICKY_CONVEYOR);
            this.addDrop(FactoryBlocks.MOTOR);
            this.addDrop(FactoryBlocks.FUNNEL);
            this.addDrop(FactoryBlocks.AXLE);
            this.addDrop(FactoryBlocks.GEARBOX);
            this.addDrop(FactoryBlocks.CONTAINER);
            this.addDrop(FactoryBlocks.NIXIE_TUBE);
            this.addDrop(FactoryBlocks.METAL_GRID);
            this.addDrop(FactoryBlocks.MINER);
            this.addDrop(FactoryBlocks.STEAM_ENGINE);
            this.addDrop(FactoryBlocks.WINDMILL, FactoryItems.AXLE_BLOCK);
        }
    }

    class Recipes extends FabricRecipeProvider {
        public Recipes(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generate(Consumer<RecipeJsonProvider> exporter) {
            var dyes = List.of(Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE, Items.WHITE_DYE);

            ShapelessRecipeJsonBuilder.create(RecipeCategory.TOOLS, FactoryItems.STEEL_ALLOY_MIXTURE)
                    .input(Items.IRON_INGOT, 2).input(Items.COAL).input(Items.REDSTONE)
                    .criterion("get_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                    .offerTo(exporter);


            CookingRecipeJsonBuilder.createSmelting(
                            Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 80)
                    .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.METAL_GRID_BLOCK, 1)
                    .pattern(" w ")
                    .pattern("www")
                    .pattern(" w ")
                    .input('w', Items.IRON_BARS)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.GRINDER_BLOCK, 1)
                    .pattern(" i ")
                    .pattern("www")
                    .pattern("ppp")
                    .input('i', Items.STONE_SLAB)
                    .input('w', Items.SMOOTH_STONE_SLAB)
                    .input('p', ItemTags.PLANKS)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);


            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.MINER_BLOCK, 1)
                    .pattern(" i ")
                    .pattern("wsw")
                    .pattern("waw")
                    .input('i', Items.IRON_INGOT).input('s', FactoryItems.STEEL_COG)
                    .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE_BLOCK)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.FAN_BLOCK, 1)
                    .pattern("iii")
                    .pattern("wsw")
                    .pattern("waw")
                    .input('i', Items.IRON_BARS).input('s', Items.IRON_INGOT)
                    .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE_BLOCK)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.FUNNEL_BLOCK, 1)
                    .pattern("wp ")
                    .pattern(" o ")
                    .pattern("wp ")
                    .input('p', FactoryItems.WOODEN_PLATE).input('o', ItemTags.WOOL)
                    .input('w', FactoryItems.STEEL_PLATE)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.CONTAINER_BLOCK, 1)
                    .pattern("www")
                    .pattern("wsw")
                    .pattern("www")
                    .input('w', FactoryItems.WOODEN_PLATE).input('s', FactoryItems.STEEL_INGOT)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.ITEM_FILTER)
                    .input(FactoryItems.WOODEN_PLATE).input(Items.COBWEB)
                    .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.CONVEYOR_BLOCK, 1)
                    .pattern("xxx")
                    .pattern("scs")
                    .pattern("xxx")
                    .input('s', Items.SMOOTH_STONE_SLAB).input('c', Items.COPPER_INGOT).input('x', FactoryItems.TREATED_DRIED_KELP)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.STICKY_CONVEYOR_BLOCK)
                    .input(FactoryItems.STICKY_CONVEYOR_BLOCK).input(Items.SLIME_BALL)
                    .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.CONVEYOR_BLOCK))
                    .offerTo(exporter);


            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.AXLE_BLOCK, 12)
                    .pattern("www")
                    .pattern("sss")
                    .pattern("www")
                    .input('w', FactoryItemTags.STRIPPED_LOGS).input('s', FactoryItems.STEEL_INGOT)
                    .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.GEARBOX_BLOCK, 1)
                    .pattern("gw ")
                    .pattern("sws")
                    .pattern(" wg")
                    .input('w', FactoryItems.AXLE_BLOCK).input('s', ItemTags.PLANKS)
                    .input('g', FactoryItems.STEEL_COG)
                    .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.AXLE_BLOCK))
                    .offerTo(exporter);

            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.HAND_CRANK_BLOCK)
                    .pattern("ip")
                    .pattern("a ")
                    .input('a', FactoryItems.AXLE_BLOCK).input('p', ItemTags.PLANKS).input('i', FactoryItems.STEEL_INGOT)
                    .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.AXLE_BLOCK))
                    .offerTo(exporter);

            of(exporter, GrindingRecipe.CODEC,
                    GrindingRecipe.of("stone_to_cobblestone", Ingredient.ofItems(Items.STONE), 2, 5, 15, Items.COBBLESTONE),
                    GrindingRecipe.of("cobblestone_to_gravel", Ingredient.ofItems(Items.COBBLESTONE), 4, 6, 15, Items.GRAVEL),
                    GrindingRecipe.of("gravel_to_sand", Ingredient.ofItems(Items.GRAVEL), 4, 3, 15, Items.SAND),
                    GrindingRecipe.of("planks_to_sticks", Ingredient.fromTag(ItemTags.PLANKS), 3, 10, OutputStack.of(Items.STICK, 0.4f, 6)),

                    GrindingRecipe.of("iron_ingot_to_nuggets", Ingredient.ofItems(Items.IRON_INGOT), 2, 5, 10, new ItemStack(Items.IRON_NUGGET, 9)),
                    GrindingRecipe.of("gold_ingot_to_nuggets", Ingredient.ofItems(Items.GOLD_INGOT), 1.8, 5, 10, new ItemStack(Items.GOLD_NUGGET, 9)),

                    GrindingRecipe.of("iron_ore_to_raw", Ingredient.fromTag(ItemTags.IRON_ORES), 6, 10, 30,
                            OutputStack.of(Items.RAW_IRON, 1f, 1), OutputStack.of(Items.RAW_IRON, 0.6f, 5)
                    ),
                    GrindingRecipe.of("gold_ore_to_raw", Ingredient.fromTag(ItemTags.GOLD_ORES), 6, 10, 30,
                            OutputStack.of(Items.RAW_GOLD, 1f, 1), OutputStack.of(Items.RAW_GOLD, 0.6f, 5)
                    ),
                    GrindingRecipe.of("copper_ore_to_raw", Ingredient.fromTag(ItemTags.COPPER_ORES), 6, 10, 30,
                            OutputStack.of(Items.RAW_COPPER, 1f, 2), OutputStack.of(Items.RAW_COPPER, 0.6f, 20)
                    ),
                    GrindingRecipe.of("bone_to_bone_meal", Ingredient.ofItems(Items.BONE), 1, 5, 10, OutputStack.of(Items.BONE_MEAL, 1, 3), OutputStack.of(Items.BONE_MEAL, 0.5f, 2)),
                    GrindingRecipe.of("blaze_rod_to_powder", Ingredient.ofItems(Items.BLAZE_ROD), 2, 5, 10, OutputStack.of(Items.BLAZE_POWDER, 1, 2), OutputStack.of(Items.BLAZE_POWDER, 0.5f, 2)),
                    GrindingRecipe.of("glowstone_to_powder", Ingredient.ofItems(Items.GLOWSTONE), 1, 5, 10, new ItemStack(Items.GLOWSTONE_DUST, 4)),

                    // Flower to Dye
                    GrindingRecipe.of("dandelion_to_dye", "dye", Ingredient.ofItems(Items.DANDELION), 1, 6, new ItemStack(Items.YELLOW_DYE, 3)),
                    GrindingRecipe.of("orchid_to_dye", "dye", Ingredient.ofItems(Items.BLUE_ORCHID), 1, 6, new ItemStack(Items.LIGHT_BLUE_DYE, 3)),
                    GrindingRecipe.of("allium_to_dye", "dye", Ingredient.ofItems(Items.ALLIUM), 1, 6, new ItemStack(Items.MAGENTA_DYE, 3)),
                    GrindingRecipe.of("azure_bluet_to_dye", "dye", Ingredient.ofItems(Items.AZURE_BLUET), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("daisy_to_dye", "dye", Ingredient.ofItems(Items.OXEYE_DAISY), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("red_tulip_to_dye", "dye", Ingredient.ofItems(Items.RED_TULIP), 1, 6, new ItemStack(Items.RED_DYE, 3)),
                    GrindingRecipe.of("orange_tulip_to_dye", "dye", Ingredient.ofItems(Items.ORANGE_TULIP), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
                    GrindingRecipe.of("white_tulip_to_dye", "dye", Ingredient.ofItems(Items.WHITE_TULIP), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                    GrindingRecipe.of("pink_tulip_to_dye", "dye", Ingredient.ofItems(Items.PINK_TULIP), 1, 6, new ItemStack(Items.PINK_DYE, 3)),
                    GrindingRecipe.of("cornflower_to_dye", "dye", Ingredient.ofItems(Items.CORNFLOWER), 1, 6, new ItemStack(Items.BLUE_DYE, 3)),
                    GrindingRecipe.of("lily_to_dye", "dye", Ingredient.ofItems(Items.LILY_OF_THE_VALLEY), 1, 6, new ItemStack(Items.WHITE_DYE, 3)),
                    GrindingRecipe.of("wither_rose_to_dye", "dye", Ingredient.ofItems(Items.WITHER_ROSE), 1, 6, new ItemStack(Items.BLACK_DYE, 3)),
                    GrindingRecipe.of("sunflower_to_dye", "dye", Ingredient.ofItems(Items.SUNFLOWER), 1, 6, new ItemStack(Items.YELLOW_DYE, 6)),
                    GrindingRecipe.of("lilac_to_dye", "dye", Ingredient.ofItems(Items.LILAC), 1, 6, new ItemStack(Items.MAGENTA_DYE, 6)),
                    GrindingRecipe.of("peony_to_dye", "dye", Ingredient.ofItems(Items.PEONY), 1, 6, new ItemStack(Items.PINK_DYE, 6)),
                    GrindingRecipe.of("rose_to_dye", "dye", Ingredient.ofItems(Items.ROSE_BUSH), 1, 6, new ItemStack(Items.RED_DYE, 6)),
                    GrindingRecipe.of("cactus_to_dye", "dye", Ingredient.ofItems(Items.CACTUS), 1, 6, new ItemStack(Items.GREEN_DYE, 3))
            );

            of(exporter, PressRecipe.CODEC,
                    PressRecipe.of("iron_ingot", Ingredient.ofItems(Items.IRON_NUGGET), 9, 10f, Items.IRON_INGOT),
                    PressRecipe.of("gold_ingot", Ingredient.ofItems(Items.GOLD_NUGGET), 9, 8f, Items.GOLD_INGOT),
                    PressRecipe.of("steel_plate", Ingredient.ofItems(FactoryItems.STEEL_INGOT), 1, 12f, new ItemStack(FactoryItems.STEEL_PLATE, 2))
            );

            for (var dye : dyes) {
                var nameSolid = ((DyeItem) dye).getColor().getName() + "_concrete";
                var namePowder = nameSolid + "_powder";

                var powder = Registries.ITEM.get(new Identifier(namePowder));
                var solid = Registries.ITEM.get(new Identifier(nameSolid));
                of(exporter, GrindingRecipe.CODEC, GrindingRecipe.of(nameSolid + "_to_powder", "concrete_to_powder",
                        Ingredient.ofItems(solid), 3, 5, powder
                ));

                of(exporter, GenericMixingRecipe.CODEC, GenericMixingRecipe.ofCounted(namePowder, "concrete_powder",
                        List.of(CountedIngredient.fromTag(4, ItemTags.SMELTS_TO_GLASS), CountedIngredient.ofItems(4, Items.GRAVEL), CountedIngredient.ofItems(1, dye)),
                        4, 1, 13, new ItemStack(powder, 8)));

                of(exporter, GenericMixingRecipe.CODEC, GenericMixingRecipe.ofCounted(nameSolid + "_direct", "concrete_direct",
                        List.of(CountedIngredient.fromTag(4, ItemTags.SMELTS_TO_GLASS),
                                CountedIngredient.ofItems(4, Items.GRAVEL),
                                CountedIngredient.ofItems(0, Items.WATER_BUCKET),
                                CountedIngredient.ofItems(1, dye)),
                        5, 1, 15, new ItemStack(solid, 8)));

                of(exporter, GenericMixingRecipe.CODEC, GenericMixingRecipe.ofCounted(nameSolid + "_from_powder", "concrete_water",
                        List.of(CountedIngredient.ofItems(1, powder), CountedIngredient.ofItems(0, Items.WATER_BUCKET)),
                        1, 1, 4, new ItemStack(solid, 1)));
            }

            of(exporter, FireworkStarMixingRecipe.CODEC,
                    new FireworkStarMixingRecipe(id("mixing/firework_star"), 4, 4, 17)
            );

            of(exporter, GenericMixingRecipe.CODEC,
                    GenericMixingRecipe.ofCounted("cake",
                            List.of(CountedIngredient.ofItems(3, Items.WHEAT), CountedIngredient.ofItems(2, Items.SUGAR),
                                    CountedIngredient.ofItems(1, Items.EGG),
                                    CountedIngredient.ofItemsRemainder(3, Items.MILK_BUCKET, Items.BUCKET)),
                            2, 1, 10, new ItemStack(Items.CAKE)),
                    GenericMixingRecipe.ofCounted("cookie",
                            List.of(CountedIngredient.ofItems(2, Items.WHEAT), CountedIngredient.ofItems(1, Items.COCOA_BEANS)),
                            2, 1, 10, new ItemStack(Items.COOKIE, 8)),
                    GenericMixingRecipe.ofCounted("bread",
                            List.of(CountedIngredient.ofItems(3, Items.WHEAT)),
                            2, 1, 10, new ItemStack(Items.BREAD, 1 )),
                    GenericMixingRecipe.ofCounted("steel_alloy_mixture",
                            List.of(CountedIngredient.ofItems(2, Items.IRON_INGOT), CountedIngredient.ofItems(1, Items.COAL),
                                    CountedIngredient.ofItems(1, Items.REDSTONE)),
                            2, 1, 15, new ItemStack(FactoryItems.STEEL_ALLOY_MIXTURE)),
                    GenericMixingRecipe.ofCounted("tnt",
                            List.of(CountedIngredient.fromTag(4, ItemTags.SMELTS_TO_GLASS), CountedIngredient.ofItems(5, Items.GUNPOWDER)),
                            2, 1, 16, new ItemStack(Items.TNT)),
                    GenericMixingRecipe.ofCounted("redstone_to_glowstone",
                            List.of(CountedIngredient.ofItems(8, Items.REDSTONE), CountedIngredient.ofItems(1, Items.BLAZE_POWDER)),
                            8, 1, 16, new ItemStack(Items.GLOWSTONE_DUST, 12))

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
