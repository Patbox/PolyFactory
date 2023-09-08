package eu.pb4.polyfactory.datagen;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.mixing.ArtificialDyeMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.ColoringMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.FireworkStarMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

class RecipesProvider extends FabricRecipeProvider {
    public RecipesProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        //noinspection unchecked
        var dyes = (List<DyeItem>) (Object) List.of(Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE, Items.WHITE_DYE);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.TOOLS, FactoryItems.STEEL_ALLOY_MIXTURE)
                .input(Items.IRON_INGOT, 2).input(FactoryItems.COAL_DUST).input(Items.REDSTONE)
                .criterion("get_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(exporter);


        CookingRecipeJsonBuilder.createSmelting(
                        Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 80)
                .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                .offerTo(exporter);

        CookingRecipeJsonBuilder.createBlasting(
                        Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 40)
                .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                .offerTo(exporter, id("steel_ingot_blasting"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.METAL_GRID, 1)
                .pattern(" w ")
                .pattern("www")
                .pattern(" w ")
                .input('w', Items.IRON_BARS)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.GRINDER, 1)
                .pattern(" i ")
                .pattern("www")
                .pattern("ppp")
                .input('i', Items.STONE_SLAB)
                .input('w', Items.SMOOTH_STONE_SLAB)
                .input('p', ItemTags.PLANKS)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.GENERIC_MACHINE_PART, 1)
                .pattern("sss")
                .pattern("g g")
                .pattern("s s")
                .input('s', FactoryItems.STEEL_INGOT)
                .input('g', FactoryItems.STEEL_GEAR)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.MIXER, 1)
                .pattern("g")
                .pattern("i")
                .pattern("c")
                .input('i', Items.IRON_SHOVEL)
                .input('c', Items.CAULDRON)
                .input('g', FactoryItems.GENERIC_MACHINE_PART)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.CABLE, 8)
                .pattern("iii")
                .pattern("ccc")
                .pattern("iii")
                .input('i', FactoryItems.TREATED_DRIED_KELP)
                .input('c', Items.COPPER_INGOT)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.TREATED_DRIED_KELP))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.PRESS, 1)
                .pattern(" g ")
                .pattern("sis")
                .pattern("wcw")
                .input('i', Items.ANVIL)
                .input('c', Items.SMOOTH_STONE)
                .input('s', FactoryItems.STEEL_INGOT)
                .input('w', ItemTags.PLANKS)
                .input('g', FactoryItems.GENERIC_MACHINE_PART)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.MINER, 1)
                .pattern(" i ")
                .pattern("wsw")
                .pattern("waw")
                .input('i', Items.IRON_INGOT).input('s', FactoryItems.STEEL_GEAR)
                .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.STEAM_ENGINE, 1)
                .pattern("www")
                .pattern("wbw")
                .pattern("aaa")
                .input('b', Items.BLAST_FURNACE)
                .input('w', FactoryItems.STEEL_PLATE).input('a', Items.DEEPSLATE_BRICKS)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);


        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.FAN, 1)
                .pattern("iii")
                .pattern("wsw")
                .pattern("waw")
                .input('i', Items.IRON_BARS).input('s', Items.IRON_INGOT)
                .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.FUNNEL, 1)
                .pattern("wp ")
                .pattern(" o ")
                .pattern("wp ")
                .input('p', FactoryItems.WOODEN_PLATE).input('o', ItemTags.WOOL)
                .input('w', FactoryItems.STEEL_PLATE)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.SPLITTER, 1)
                .pattern("sss")
                .pattern("f f")
                .pattern("sss")
                .input('f', FactoryItems.FUNNEL).input('s', FactoryItems.STEEL_PLATE)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.FUNNEL))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.CONTAINER, 1)
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

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.ITEM_COUNTER)
                .pattern("pqp")
                .pattern("scs")
                .pattern("scs")
                .input('s', FactoryItems.STEEL_PLATE).input('c', Items.COPPER_INGOT).input('p', FactoryItems.WOODEN_PLATE)
                .input('q', Items.QUARTZ)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_INPUT)
                .pattern("r-r")
                .pattern("srs")
                .pattern("scs")
                .input('s', FactoryItems.STEEL_PLATE).input('c', Items.COPPER_INGOT)
                .input('-', Items.SMOOTH_STONE_SLAB).input('r', Items.REDSTONE)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_OUTPUT)
                .pattern("r-r")
                .pattern("sbs")
                .pattern("scs")
                .input('s', FactoryItems.STEEL_PLATE).input('c', Items.COPPER_INGOT)
                .input('-', Items.SMOOTH_STONE_SLAB).input('r', Items.REDSTONE)
                .input('b', Items.REDSTONE_BLOCK)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.CONVEYOR, 1)
                .pattern("xxx")
                .pattern("scs")
                .pattern("xxx")
                .input('s', Items.SMOOTH_STONE_SLAB).input('c', Items.COPPER_INGOT).input('x', FactoryItems.TREATED_DRIED_KELP)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.STICKY_CONVEYOR)
                .input(FactoryItems.CONVEYOR).input(Items.SLIME_BALL)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.CONVEYOR))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.INVERTED_REDSTONE_LAMP)
                .input(Items.REDSTONE_LAMP).input(Items.REDSTONE_TORCH)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.REDSTONE_LAMP))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.AXLE, 12)
                .pattern("www")
                .pattern("sss")
                .pattern("www")
                .input('w', FactoryItemTags.STRIPPED_LOGS).input('s', FactoryItems.STEEL_INGOT)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.NIXIE_TUBE, 1)
                .pattern(" g ")
                .pattern("gsg")
                .pattern("cpc")
                .input('p', FactoryItems.WOODEN_PLATE)
                .input('c', Items.COPPER_INGOT)
                .input('s', FactoryItems.STEEL_INGOT)
                .input('g', Items.GLASS_PANE)
                .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.GEARBOX, 1)
                .pattern("gw ")
                .pattern("sws")
                .pattern(" wg")
                .input('w', FactoryItems.AXLE).input('s', ItemTags.PLANKS)
                .input('g', FactoryItems.STEEL_GEAR)
                .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.AXLE))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.HAND_CRANK)
                .pattern("ip")
                .pattern("l ")
                .input('l', FactoryItemTags.STRIPPED_LOGS).input('p', ItemTags.PLANKS).input('i', Items.IRON_INGOT)
                .criterion("get_axle", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(exporter);

        of(exporter, ColoringCraftingRecipe.CODEC, ColoringCraftingRecipe.of("cable_color", FactoryItems.CABLE));
        of(exporter, ColoringMixingRecipe.CODEC, ColoringMixingRecipe.of("cable_color", FactoryItems.CABLE, 2, 6, 10));

        of(exporter, GrindingRecipe.CODEC,
                GrindingRecipe.of("coal_dust", Ingredient.ofItems(Items.COAL), 1, 5, 8, FactoryItems.COAL_DUST),
                GrindingRecipe.of("planks_saw_dust", Ingredient.fromTag(ItemTags.PLANKS), 1, 5, 6, OutputStack.of(FactoryItems.SAW_DUST, 0.6f, 3)),
                GrindingRecipe.of("logs_saw_dust", Ingredient.fromTag(ItemTags.LOGS), 1, 5, 6, OutputStack.of(FactoryItems.SAW_DUST, 0.8f, 6)),
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
                PressRecipe.of("steel_plate", Ingredient.ofItems(FactoryItems.STEEL_INGOT), 1, 12f, new ItemStack(FactoryItems.STEEL_PLATE, 1)),
                PressRecipe.of("wooden_plate", Ingredient.ofItems(FactoryItems.SAW_DUST), 2, 5f, new ItemStack(FactoryItems.WOODEN_PLATE, 1)),
                PressRecipe.of("golden_carrot", CountedIngredient.ofItems(1, Items.CARROT), CountedIngredient.ofItems(8, Items.GOLD_NUGGET),
                        5, OutputStack.of(Items.GOLDEN_CARROT)),
                PressRecipe.of("golden_apple", CountedIngredient.ofItems(1, Items.APPLE), CountedIngredient.ofItems(8, Items.GOLD_INGOT),
                        5, OutputStack.of(Items.GOLDEN_APPLE))
        );

        for (var dye : dyes) {
            var x = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.WINDMILL_SAIL, 1)
                    .pattern(" sw")
                    .pattern(" ws")
                    .pattern("wsc")
                    .input('w', FactoryItems.WOODEN_PLATE).input('s', Items.STICK)
                    .input('c', Registries.ITEM.get(new Identifier(dye.getColor().getName() + "_wool")))
                    .group("polyfactory:windmill_sail")
                    .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE));

            if (dye != Items.WHITE_DYE) {
                var nbt = new NbtCompound();
                var display = new NbtCompound();
                display.putInt("color", DyeColorExtra.getColor(dye.getColor()));
                nbt.put("display", display);

                ((NbtRecipe) x).polyfactory$setNbt(nbt);
            }

            x.offerTo(exporter, id("windmill_sail/wool/" + dye.getColor()));


            var nameSolid = dye.getColor().getName() + "_concrete";
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

        of(exporter, ArtificialDyeMixingRecipe.CODEC,
                new ArtificialDyeMixingRecipe(id("mixing/artificial_dye"), 3, 4, 15)
        );

        of(exporter, GenericMixingRecipe.CODEC,
                GenericMixingRecipe.ofCounted("treated_dried_kelp", List.of(CountedIngredient.ofItems(16, Items.DRIED_KELP), CountedIngredient.ofItems(1, Items.BLACK_DYE)), 2, 1, 6f, 0.2f, new ItemStack(FactoryItems.TREATED_DRIED_KELP, 16)),

                GenericMixingRecipe.of("coarse_dirt",
                        List.of(Ingredient.ofItems(Items.DIRT), Ingredient.ofItems(Items.GRAVEL)), 2, 4, 10, new ItemStack(Items.COARSE_DIRT, 2)),
                GenericMixingRecipe.of("packed_mud",
                        List.of(Ingredient.ofItems(Items.WHEAT), Ingredient.ofItems(Items.MUD)), 2, 4, 10, new ItemStack(Items.PACKED_MUD)),
                GenericMixingRecipe.ofCounted("cake",
                        List.of(CountedIngredient.ofItems(3, Items.WHEAT), CountedIngredient.ofItems(2, Items.SUGAR),
                                CountedIngredient.ofItems(1, Items.EGG),
                                CountedIngredient.ofItemsRemainder(3, Items.MILK_BUCKET, Items.BUCKET)),
                        2, 4, 10, new ItemStack(Items.CAKE)),
                GenericMixingRecipe.ofCounted("cookie",
                        List.of(CountedIngredient.ofItems(2, Items.WHEAT), CountedIngredient.ofItems(1, Items.COCOA_BEANS)),
                        2, 4, 10, new ItemStack(Items.COOKIE, 8)),
                GenericMixingRecipe.ofCounted("bread",
                        List.of(CountedIngredient.ofItems(3, Items.WHEAT)),
                        2, 4, 10, new ItemStack(Items.BREAD, 1)),
                GenericMixingRecipe.ofCounted("steel_alloy_mixture",
                        List.of(CountedIngredient.ofItems(3, Items.IRON_INGOT), CountedIngredient.ofItems(2, FactoryItems.COAL_DUST),
                                CountedIngredient.ofItems(1, Items.REDSTONE)),
                        2, 4, 15, new ItemStack(FactoryItems.STEEL_ALLOY_MIXTURE, 2)),
                GenericMixingRecipe.ofCounted("tnt",
                        List.of(CountedIngredient.fromTag(4, ItemTags.SMELTS_TO_GLASS), CountedIngredient.ofItems(5, Items.GUNPOWDER)),
                        2, 4, 16, new ItemStack(Items.TNT)),
                GenericMixingRecipe.ofCounted("redstone_to_glowstone",
                        List.of(CountedIngredient.ofItems(8, Items.REDSTONE), CountedIngredient.ofItems(1, Items.BLAZE_POWDER)),
                        8, 4, 16, new ItemStack(Items.GLOWSTONE_DUST, 12))

        );

        ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, FactoryItems.STEEL_GEAR)
                .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                .pattern(" i ")
                .pattern("i i")
                .pattern(" i ")
                .input('i', FactoryItems.STEEL_INGOT)
                .offerTo(exporter);

    }

    public <T extends Recipe<?>> void of(Consumer<RecipeJsonProvider> exporter, Codec<T> codec, T... recipes) {
        for (var recipe : recipes) {
            exporter.accept(new CodecRecipeJsonProvider<>(codec, recipe));
        }
    }
}