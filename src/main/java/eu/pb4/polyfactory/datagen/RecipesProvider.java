package eu.pb4.polyfactory.datagen;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.fluid.FactoryFluidConstants;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.SpoutMolds;
import eu.pb4.polyfactory.mixin.BrewingRecipeRegistryAccessor;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.casting.SimpleCauldronCastingRecipe;
import eu.pb4.polyfactory.recipe.drain.PotionAddDrainRecipe;
import eu.pb4.polyfactory.recipe.drain.PotionRemoveDrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.RemovingFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.fluid.SimpleFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.*;
import eu.pb4.polyfactory.recipe.press.FillSprayCanPressRecipe;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import eu.pb4.polyfactory.recipe.smeltery.SimpleSmelteryRecipe;
import eu.pb4.polyfactory.recipe.spout.PotionSpoutRecipe;
import eu.pb4.polyfactory.recipe.spout.RepairSpoutRecipe;
import eu.pb4.polyfactory.recipe.drain.SimpleDrainRecipe;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.data.recipe.*;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

class RecipesProvider extends FabricRecipeProvider {
    public RecipesProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup registryLookup, RecipeExporter exporter) {
        return new RecipeGenerator(registryLookup, exporter) {
            @Override
            public void generate() {
                var itemWrap = registryLookup.getOrThrow(RegistryKeys.ITEM);

                offerReversibleCompactingRecipes(RecipeCategory.MISC, FactoryItems.STEEL_INGOT, RecipeCategory.MISC, FactoryItems.STEEL_BLOCK, "steel_block", null, "steel_ingot_from_block", null);

                //noinspection unchecked
                var dyes = (List<DyeItem>) (Object) List.of(Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE, Items.WHITE_DYE);

                this.createShapeless(RecipeCategory.TOOLS, FactoryItems.STEEL_ALLOY_MIXTURE)
                        .input(Items.IRON_INGOT, 2).input(FactoryItems.COAL_DUST).input(Items.REDSTONE)
                        .criterion("get_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.STEEL_BUTTON)
                        .input(FactoryItems.STEEL_INGOT, 1)
                        .criterion("get_s", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_BUTTON))
                        .offerTo(exporter);

                CookingRecipeJsonBuilder.createSmelting(
                                Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 80)
                        .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                        .offerTo(exporter);

                CookingRecipeJsonBuilder.createBlasting(
                                Ingredient.ofItems(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 40)
                        .criterion("get_steel_mixture", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_ALLOY_MIXTURE))
                        .offerTo(exporter, recipeKey("steel_ingot_blasting"));

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.METAL_GRID, 1)
                        .pattern(" w ")
                        .pattern("www")
                        .pattern(" w ")
                        .input('w', Items.IRON_BARS)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.GRINDER, 1)
                        .pattern(" i ")
                        .pattern("www")
                        .pattern("ppp")
                        .input('i', Items.STONE_SLAB)
                        .input('w', Items.SMOOTH_STONE_SLAB)
                        .input('p', ItemTags.PLANKS)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.STEEL_MACHINE_GEARBOX, 1)
                        .pattern("sss")
                        .pattern("g g")
                        .pattern("s s")
                        .input('s', FactoryItems.STEEL_INGOT)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.MIXER, 1)
                        .pattern("g")
                        .pattern("i")
                        .pattern("c")
                        .input('i', Items.IRON_SHOVEL)
                        .input('c', Items.CAULDRON)
                        .input('g', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CABLE, 8)
                        .pattern("iii")
                        .pattern("ccc")
                        .pattern("iii")
                        .input('i', FactoryItems.TREATED_DRIED_KELP)
                        .input('c', Items.COPPER_INGOT)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.TREATED_DRIED_KELP))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.GATED_CABLE, 1)
                        .pattern("ris")
                        .pattern("cxc")
                        .pattern("sir")
                        .input('i', FactoryItems.STEEL_PLATE)
                        .input('s', Items.SMOOTH_STONE_SLAB)
                        .input('x', Items.COPPER_INGOT)
                        .input('r', Items.REDSTONE)
                        .input('c', FactoryItems.CABLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.CABLE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PRESS, 1)
                        .pattern(" g ")
                        .pattern("sis")
                        .pattern("wcw")
                        .input('i', Items.ANVIL)
                        .input('c', Items.SMOOTH_STONE)
                        .input('s', FactoryItems.STEEL_INGOT)
                        .input('w', ItemTags.PLANKS)
                        .input('g', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.MINER, 1)
                        .pattern(" i ")
                        .pattern("wsw")
                        .pattern("waw")
                        .input('i', Items.IRON_INGOT).input('s', FactoryItems.STEEL_GEAR)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PLACER, 1)
                        .pattern("wsw")
                        .pattern("gig")
                        .pattern("waw")
                        .input('i', FactoryItems.INTEGRATED_CIRCUIT)
                        .input('s', FactoryItems.STEEL_GEAR)
                        .input('g', Items.GOLD_BLOCK)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.MINER)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.STEAM_ENGINE, 1)
                        .pattern("www")
                        .pattern("wbw")
                        .pattern("aaa")
                        .input('b', Items.BLAST_FURNACE)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', Items.DEEPSLATE_BRICKS)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.EJECTOR, 1)
                        .pattern("iii")
                        .pattern("wgw")
                        .pattern("waw")
                        .input('i', FactoryItems.WOODEN_PLATE)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.FAN, 1)
                        .pattern("iii")
                        .pattern("wsw")
                        .pattern("waw")
                        .input('i', Items.IRON_BARS).input('s', Items.IRON_INGOT)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CRAFTER, 1)
                        .pattern("iai")
                        .pattern("wsw")
                        .pattern("www")
                        .input('i', FactoryItems.STEEL_GEAR).input('s', Items.CRAFTING_TABLE)
                        .input('w', FactoryItems.STEEL_PLATE).input('a', FactoryItems.AXLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.TURNTABLE, 1)
                        .pattern("-")
                        .pattern("i")
                        .input('-', ItemTags.WOODEN_SLABS)
                        .input('i', FactoryItems.AXLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.MISC, FactoryItems.WORKBENCH, 1)
                        .input(Items.CRAFTING_TABLE)
                        .input(FactoryItems.WOODEN_PLATE)
                        .criterion("get_plate", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.MISC, FactoryItems.BLUEPRINT_WORKBENCH, 1)
                        .input(Items.BLUE_DYE)
                        .input(Items.PAPER)
                        .input(FactoryItems.WORKBENCH)
                        .criterion("get_plate", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PIPE, 3)
                        .pattern("ppp")
                        .pattern("i i")
                        .pattern("ppp")
                        .input('p', FactoryItems.COPPER_PLATE)
                        .input('i', Items.COPPER_INGOT)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(Items.COPPER_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.FILTERED_PIPE, 1)
                        .pattern(" w ")
                        .pattern("pgp")
                        .pattern(" w ")
                        .input('p', FactoryItems.PIPE)
                        .input('w', FactoryItems.STEEL_PLATE)
                        .input('g', FactoryItems.TREATED_DRIED_KELP)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.PIPE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_VALVE_PIPE, 1)
                        .pattern("rwr")
                        .pattern("pwp")
                        .pattern("rwr")
                        .input('p', FactoryItems.PIPE)
                        .input('w', FactoryItems.STEEL_PLATE)
                        .input('r', Items.REDSTONE)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.PIPE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.DRAIN, 1)
                        .pattern("pip")
                        .pattern("p p")
                        .pattern("ppp")
                        .input('p', FactoryItems.COPPER_PLATE)
                        .input('i', Items.IRON_INGOT)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(Items.COPPER_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.MECHANICAL_DRAIN, 1)
                        .pattern("s")
                        .pattern("a")
                        .pattern("d")
                        .input('d', FactoryItems.DRAIN)
                        .input('s', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .input('a', FactoryItems.AXLE)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.DRAIN))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.MECHANICAL_SPOUT, 1)
                        .pattern(" s ")
                        .pattern("cac")
                        .pattern("pdp")
                        .input('d', Items.SMOOTH_STONE)
                        .input('p', ItemTags.PLANKS)
                        .input('s', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .input('a', FactoryItems.PIPE)
                        .input('c', FactoryItems.COPPER_PLATE)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(Items.COPPER_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PORTABLE_FLUID_TANK, 1)
                        .pattern("p")
                        .pattern("t")
                        .pattern("p")
                        .input('p', FactoryItems.COPPER_PLATE)
                        .input('t', FactoryItems.FLUID_TANK)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(Items.COPPER_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.FLUID_TANK, 1)
                        .pattern("pgp")
                        .pattern("gcg")
                        .pattern("pgp")
                        .input('p', FactoryItems.COPPER_PLATE)
                        .input('g', Items.TINTED_GLASS)
                        .input('c', Items.COPPER_BLOCK)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(Items.COPPER_INGOT))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PUMP, 1)
                        .pattern(" w ")
                        .pattern("pgp")
                        .pattern(" w ")
                        .input('p', FactoryItems.PIPE)
                        .input('w', FactoryItems.WOODEN_PLATE)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.PIPE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.NOZZLE, 1)
                        .pattern(" c ")
                        .pattern("psc")
                        .pattern(" c ")
                        .input('p', FactoryItems.PIPE)
                        .input('c', FactoryItems.COPPER_PLATE)
                        .input('s', FactoryItems.STEEL_PLATE) // replace with something else?
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.PIPE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PRESSURE_FLUID_GUN, 1)
                        .pattern("ipn")
                        .pattern("sw ")
                        .input('p', FactoryItems.PIPE)
                        .input('n', FactoryItems.NOZZLE)
                        .input('i', Items.NETHERITE_INGOT)
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('w', FactoryItems.WOODEN_PLATE)
                        .criterion("get_copper", InventoryChangedCriterion.Conditions.items(FactoryItems.PIPE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.FUNNEL, 1)
                        .pattern("wp ")
                        .pattern(" o ")
                        .pattern("wp ")
                        .input('p', FactoryItems.WOODEN_PLATE).input('o', ItemTags.WOOL)
                        .input('w', FactoryItems.STEEL_PLATE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.SLOT_AWARE_FUNNEL, 1)
                        .pattern("gcg")
                        .pattern("sfs")
                        .pattern("gsg")
                        .input('c', FactoryItems.REDSTONE_CHIP)
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('f', FactoryItems.FUNNEL)
                        .input('g', Items.GOLD_INGOT)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.SPLITTER, 1)
                        .pattern("sss")
                        .pattern("fxf")
                        .pattern("sss")
                        .input('f', FactoryItems.FUNNEL).input('s', FactoryItems.STEEL_PLATE)
                        .input('x', FactoryItems.REDSTONE_CHIP)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.FUNNEL))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_PACKER, 1)
                        .pattern("wsw")
                        .pattern("shs")
                        .pattern("wsw")
                        .input('w', FactoryItems.WOODEN_PLATE).input('s', FactoryItems.STEEL_PLATE)
                        .input('h', Items.HOPPER)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CONTAINER, 1)
                        .pattern("www")
                        .pattern("wsw")
                        .pattern("www")
                        .input('w', FactoryItems.WOODEN_PLATE).input('s', FactoryItems.STEEL_INGOT)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_FILTER)
                        .pattern(" p ")
                        .pattern("imi")
                        .pattern(" p ")
                        .input('p', FactoryItems.WOODEN_PLATE)
                        .input('m', FactoryItems.STRING_MESH)
                        .input('i', Items.IRON_INGOT)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.MISC, FactoryItems.STRING_MESH)
                        .pattern(" s ")
                        .pattern("sss")
                        .pattern(" s ")
                        .input('s', Items.STRING)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.STRING))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_COUNTER)
                        .pattern("pqp")
                        .pattern("scs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE).input('c', Items.COPPER_INGOT).input('p', FactoryItems.WOODEN_PLATE)
                        .input('q', Items.QUARTZ)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_INPUT)
                        .pattern("r-r")
                        .pattern("srs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE).input('c', Items.COPPER_INGOT)
                        .input('-', Items.SMOOTH_STONE_SLAB).input('r', Items.REDSTONE)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_OUTPUT)
                        .pattern("r-r")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('-', Items.SMOOTH_STONE_SLAB)
                        .input('r', Items.REDSTONE)
                        .input('b', Items.REDSTONE_BLOCK)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.SPEAKER)
                        .pattern("c-c")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('-', Items.NOTE_BLOCK)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.RECORD_PLAYER)
                        .pattern("c-c")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('-', Items.JUKEBOX)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);



                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ARITHMETIC_OPERATOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .input('c', Items.COPPER_INGOT)
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('-', FactoryItems.INTEGRATED_CIRCUIT)
                        .input('w', FactoryItems.CABLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.DATA_COMPARATOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .input('c', Items.QUARTZ)
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('-', FactoryItems.REDSTONE_CHIP)
                        .input('w', FactoryItems.CABLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.DATA_EXTRACTOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .input('c', Items.LAPIS_LAZULI)
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('-', FactoryItems.REDSTONE_CHIP)
                        .input('w', FactoryItems.CABLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PROGRAMMABLE_DATA_EXTRACTOR, 1)
                        .pattern("c c")
                        .pattern("s-w")
                        .pattern("c c")
                        .input('c', Items.COPPER_INGOT)
                        .input('s', FactoryItems.INTEGRATED_CIRCUIT)
                        .input('-', FactoryItems.DATA_EXTRACTOR)
                        .input('w', FactoryItems.CABLE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.DATA_MEMORY, 1)
                        .pattern("cpc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .input('c', Items.GOLD_INGOT).input('s', FactoryItems.STEEL_PLATE)
                        .input('p', FactoryItems.COPPER_PLATE)
                        .input('-', FactoryItems.REDSTONE_CHIP)
                        .input('w', FactoryItems.CABLE)

                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.DATA_MEMORY, 1)
                        .input(FactoryItems.DATA_MEMORY)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.DATA_MEMORY))
                        .offerTo(exporter, "polyfactory:data_memory_clear");

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.WIRELESS_REDSTONE_RECEIVER)
                        .input(FactoryItems.REDSTONE_OUTPUT)
                        .input(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .input(Items.LIGHTNING_ROD)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.WIRELESS_REDSTONE_TRANSMITTER)
                        .input(FactoryItems.REDSTONE_INPUT)
                        .input(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .input(Items.LIGHTNING_ROD)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)
                        .input(FactoryItems.STEEL_PLATE)
                        .input(FactoryItems.INTEGRATED_CIRCUIT)
                        .input(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .input(Items.COPPER_INGOT)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CONVEYOR, 1)
                        .pattern("xxx")
                        .pattern("scs")
                        .pattern("xxx")
                        .input('s', Items.SMOOTH_STONE_SLAB).input('c', Items.COPPER_INGOT).input('x', FactoryItems.TREATED_DRIED_KELP)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.STICKY_CONVEYOR)
                        .input(FactoryItems.CONVEYOR).input(Items.SLIME_BALL)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.CONVEYOR))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.INVERTED_REDSTONE_LAMP)
                        .input(Items.REDSTONE_LAMP).input(Items.REDSTONE_TORCH)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.REDSTONE_LAMP))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.AXLE, 8)
                        .pattern("www")
                        .pattern("sss")
                        .pattern("www")
                        .input('w', ConventionalItemTags.STRIPPED_LOGS).input('s', FactoryItems.STEEL_INGOT)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.NIXIE_TUBE, 1)
                        .pattern(" g ")
                        .pattern("gsg")
                        .pattern("cpc")
                        .input('p', FactoryItems.WOODEN_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('s', FactoryItems.STEEL_INGOT)
                        .input('g', Items.GLASS_PANE)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.NIXIE_TUBE_CONTROLLER)
                        .pattern("csc")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('b', FactoryItems.INTEGRATED_CIRCUIT)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.HOLOGRAM_PROJECTOR, 1)
                        .pattern("pdp")
                        .pattern("ese")
                        .pattern("pcp")
                        .input('d', Items.DIAMOND)
                        .input('e', FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .input('p', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('s', FactoryItems.INTEGRATED_CIRCUIT)
                        .criterion("get_steel", InventoryChangedCriterion.Conditions.items(Items.ENDER_PEARL))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_READER)
                        .pattern("plp")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('l', Items.LECTERN)
                        .input('p', FactoryItems.WOODEN_PLATE)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.BLOCK_OBSERVER)
                        .pattern("sos")
                        .pattern("sbs")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('o', Items.OBSERVER)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.TEXT_INPUT)
                        .pattern("s-s")
                        .pattern("pbt")
                        .pattern("scs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('c', FactoryItems.CABLE)
                        .input('p', Items.IRON_INGOT)
                        .input('-', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .input('t', FactoryItems.TREATED_DRIED_KELP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PLANTER)
                        .pattern("sgs")
                        .pattern("pbp")
                        .pattern("sgs")
                        .input('s', FactoryItems.STEEL_PLATE)
                        .input('b', FactoryItems.REDSTONE_CHIP)
                        .input('p', ItemTags.PLANKS)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                        .offerTo(exporter);


        /*this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ELECTRIC_GENERATOR)
                .pattern("scs")
                .pattern("aia")
                .pattern("scs")
                .input('s', FactoryItems.STEEL_PLATE)
                .input('a', FactoryItems.AXLE)
                .input('i', FactoryItems.STEEL_INGOT)
                .input('c', Items.COPPER_BLOCK)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                .offerTo(exporter);

        this.createShaped(RecipeCategory.REDSTONE, FactoryItems.ELECTRIC_MOTOR)
                .pattern("scs")
                .pattern("cia")
                .pattern("scs")
                .input('s', FactoryItems.STEEL_PLATE)
                .input('a', FactoryItems.AXLE)
                .input('i', FactoryItems.STEEL_INGOT)
                .input('c', Items.COPPER_BLOCK)
                .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_PLATE))
                .offerTo(exporter);
        */
                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.GEARBOX, 1)
                        .pattern("sgs")
                        .pattern("gwg")
                        .pattern("sgs")
                        .input('w', FactoryItems.AXLE)
                        .input('s', ItemTags.PLANKS)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.AXLE))
                        .offerTo(exporter);


                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CLUTCH, 1)
                        .pattern("sps")
                        .pattern("gwg")
                        .pattern("sps")
                        .input('w', FactoryItems.AXLE)
                        .input('s', ItemTags.PLANKS)
                        .input('g', FactoryItems.STEEL_GEAR)
                        .input('p', FactoryItems.STEEL_PLATE)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.AXLE))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.SMELTERY_CORE, 1)
                        .pattern(" d ")
                        .pattern("dpd")
                        .pattern(" d ")
                        .input('d', Items.DEEPSLATE_BRICKS)
                        .input('p', FactoryItems.STEEL_PLATE)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.PRIMITIVE_SMELTERY, 1)
                        .pattern("ibi")
                        .pattern("sfs")
                        .pattern("ibi")
                        .input('i', Items.IRON_INGOT)
                        .input('s', Items.SMOOTH_STONE_SLAB)
                        .input('f', Items.BLAST_FURNACE)
                        .input('b', Items.BRICKS)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.CASTING_TABLE, 1)
                        .pattern("i i")
                        .pattern("iii")
                        .pattern("s s")
                        .input('i', Items.IRON_INGOT)
                        .input('s', Items.SMOOTH_STONE_SLAB)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.SMELTERY_FAUCED, 1)
                        .pattern("csc")
                        .pattern("cic")
                        .input('c', Items.COPPER_INGOT)
                        .input('i', Items.IRON_INGOT)
                        .input('s', Items.STONE)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.HAND_CRANK)
                        .pattern("ip")
                        .pattern("l ")
                        .input('l', ConventionalItemTags.STRIPPED_LOGS).input('p', ItemTags.PLANKS).input('i', Items.IRON_INGOT)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_CHIP)
                        .pattern("grg")
                        .pattern("rcr")
                        .pattern("grg")
                        .input('g', Items.GOLD_NUGGET)
                        .input('c', FactoryItems.COPPER_PLATE)
                        .input('r', Items.REDSTONE)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.INTEGRATED_CIRCUIT)
                        .pattern("qtq")
                        .pattern("cpr")
                        .pattern("ggg")
                        .input('g', Items.GOLD_NUGGET)
                        .input('q', Items.QUARTZ)
                        .input('c', FactoryItems.COPPER_PLATE)
                        .input('r', FactoryItems.REDSTONE_CHIP)
                        .input('t', FactoryItems.TREATED_DRIED_KELP)
                        .input('p', FactoryItems.WOODEN_PLATE)
                        .criterion("get_axle", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.WRENCH)
                        .pattern(" i ")
                        .pattern(" ti")
                        .pattern("i  ")
                        .input('i', FactoryItems.STEEL_INGOT)
                        .input('t', FactoryItems.TREATED_DRIED_KELP)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.MISC, FactoryItems.TINY_POTATO_SPRING)
                        .pattern(" p ")
                        .pattern(" s ")
                        .pattern(" w ")
                        .input('p', Items.POTATO)
                        .input('s', FactoryItems.STEEL_INGOT)
                        .input('w', FactoryItems.WOODEN_PLATE)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.DRAGON_EGG))
                        .offerTo(exporter);

                of(exporter,
                        ColoringCraftingRecipe.of(itemWrap, "cable_color", FactoryItems.CABLE),
                        ColoringCraftingRecipe.of(itemWrap, "lamp_color", FactoryItems.LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_color", FactoryItems.INVERTED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "caged_lamp_color", FactoryItems.CAGED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_caged_color", FactoryItems.INVERTED_CAGED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "fixture_lamp_color", FactoryItems.FIXTURE_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_fixture_color", FactoryItems.INVERTED_FIXTURE_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "portable_redstone_transmitter_color", FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, 1)
                );
                of(exporter,
                        ColoringMixingRecipe.of("cable_color", FactoryItems.CABLE, 2, 6, 10),
                        ColoringMixingRecipe.of("lamp_color", FactoryItems.LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_lamp_color", FactoryItems.INVERTED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("caged_lamp_color", FactoryItems.CAGED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_caged_lamp_color", FactoryItems.INVERTED_CAGED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("fixture_lamp_color", FactoryItems.FIXTURE_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_fixture_lamp_color", FactoryItems.INVERTED_FIXTURE_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("portable_redstone_transmitter_color", FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, 1, 2, 6, 10)
                );

                of(exporter,
                        GrindingRecipe.of("coal_dust", Ingredient.ofItems(Items.COAL), 1, 5, 8, FactoryItems.COAL_DUST),
                        GrindingRecipe.of("coal_dust_charcoal", Ingredient.ofItems(Items.CHARCOAL), 1.5, 5, 14, OutputStack.of(FactoryItems.COAL_DUST, 0.8f)),
                        GrindingRecipe.of("netherrack_dust", Ingredient.ofItems(Items.NETHERRACK), 2, 8, 15,
                                OutputStack.of(FactoryItems.NETHERRACK_DUST, 1, 2), OutputStack.of(FactoryItems.NETHERRACK_DUST, 0.25f, 5)),
                        GrindingRecipe.of("ender_dust", Ingredient.ofItems(Items.ENDER_PEARL), 6, 20, 25,
                                OutputStack.of(FactoryItems.ENDER_DUST, 1, 2), OutputStack.of(FactoryItems.ENDER_DUST, 0.20f, 2)),
                        GrindingRecipe.of("planks_saw_dust", ingredientFromTag(ItemTags.PLANKS), 1, 5, 6,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.6f, 3), OutputStack.of(Items.STICK, 0.4f, 3)),
                        GrindingRecipe.of("logs_saw_dust", ingredientFromTag(ItemTags.LOGS), 1, 5, 6,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.8f, 6), OutputStack.of(Items.STICK, 0.4f, 8)),
                        GrindingRecipe.of("stone_to_cobblestone", Ingredient.ofItems(Items.STONE), 2, 5, 15, Items.COBBLESTONE),
                        GrindingRecipe.of("cobblestone_to_gravel", Ingredient.ofItems(Items.COBBLESTONE), 4, 6, 15, Items.GRAVEL),
                        GrindingRecipe.of("diorite_to_gravel", Ingredient.ofItems(Items.DIORITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.05f, 1)),
                        GrindingRecipe.of("andesite_to_gravel", Ingredient.ofItems(Items.ANDESITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.05f, 1)),
                        GrindingRecipe.of("granite_to_gravel", Ingredient.ofItems(Items.GRANITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.1f, 1)),
                        GrindingRecipe.of("gravel_to_sand", Ingredient.ofItems(Items.GRAVEL), 4, 3, 15, Items.SAND),

                        GrindingRecipe.of("iron_ingot_to_nuggets", Ingredient.ofItems(Items.IRON_INGOT), 2, 5, 10, new ItemStack(Items.IRON_NUGGET, 9)),
                        GrindingRecipe.of("gold_ingot_to_nuggets", Ingredient.ofItems(Items.GOLD_INGOT), 1.8, 5, 10, new ItemStack(Items.GOLD_NUGGET, 9)),
                        GrindingRecipe.of("nether_wart", Ingredient.ofItems(Items.NETHER_WART_BLOCK), 1.8, 5, 10,
                                OutputStack.of(Items.NETHER_WART), OutputStack.of(Items.NETHER_WART, 0.4f, 5)
                        ),

                        GrindingRecipe.of("iron_ore_to_raw", ingredientFromTag(ItemTags.IRON_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_IRON, 1f, 1), OutputStack.of(Items.RAW_IRON, 0.6f, 5)
                        ),
                        GrindingRecipe.of("gold_ore_to_raw", ingredientFromTag(ItemTags.GOLD_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_GOLD, 1f, 1), OutputStack.of(Items.RAW_GOLD, 0.6f, 5)
                        ),
                        GrindingRecipe.of("copper_ore_to_raw", ingredientFromTag(ItemTags.COPPER_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_COPPER, 1f, 2), OutputStack.of(Items.RAW_COPPER, 0.6f, 20)
                        ),
                        GrindingRecipe.of("bone_to_bone_meal", Ingredient.ofItems(Items.BONE), 1, 5, 10, OutputStack.of(Items.BONE_MEAL, 1, 3), OutputStack.of(Items.BONE_MEAL, 0.5f, 2)),
                        GrindingRecipe.of("blaze_rod_to_powder", Ingredient.ofItems(Items.BLAZE_ROD), 2, 5, 10, OutputStack.of(Items.BLAZE_POWDER, 1, 2), OutputStack.of(Items.BLAZE_POWDER, 0.5f, 2)),
                        GrindingRecipe.of("breeze_rod_to_charge", Ingredient.ofItems(Items.BREEZE_ROD), 2, 5, 10, OutputStack.of(Items.WIND_CHARGE, 1, 4), OutputStack.of(Items.WIND_CHARGE, 0.5f, 2)),
                        GrindingRecipe.of("glowstone_to_powder", Ingredient.ofItems(Items.GLOWSTONE), 1, 5, 10, new ItemStack(Items.GLOWSTONE_DUST, 4)),
                        GrindingRecipe.of("sugar", Ingredient.ofItems(Items.SUGAR_CANE), 1, 5, OutputStack.of(Items.SUGAR, 1f, 1), OutputStack.of(Items.SUGAR, 0.4f, 1)),

                        // Crushed raw metals

                        GrindingRecipe.of("crushed_raw_iron", Ingredient.ofItems(Items.RAW_IRON), 8, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_IRON, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_IRON, 0.4f, 1)
                        ),
                        GrindingRecipe.of("crushed_raw_gold", Ingredient.ofItems(Items.RAW_GOLD), 6, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_GOLD, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_GOLD, 0.4f, 1)
                        ),
                        GrindingRecipe.of("crushed_raw_copper", Ingredient.ofItems(Items.RAW_COPPER), 6, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_COPPER, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_COPPER, 0.4f, 1)
                        ),

                        // Flower to Dye
                        GrindingRecipe.of("dandelion_to_dye", "dye", Ingredient.ofItems(Items.DANDELION), 1, 6, new ItemStack(Items.YELLOW_DYE, 3)),
                        GrindingRecipe.of("torch_flower_to_dye", "dye", Ingredient.ofItems(Items.TORCHFLOWER), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
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
                        GrindingRecipe.of("pitcher_to_dye", "dye", Ingredient.ofItems(Items.PITCHER_PLANT), 1, 6, new ItemStack(Items.CYAN_DYE, 6)),
                        GrindingRecipe.of("cactus_to_dye", "dye", Ingredient.ofItems(Items.CACTUS), 1, 6, new ItemStack(Items.GREEN_DYE, 3)),
                        GrindingRecipe.of("closed_eyeblossom_to_dye", "dye", Ingredient.ofItems(Items.CLOSED_EYEBLOSSOM), 1, 6, new ItemStack(Items.GRAY_DYE, 3)),
                        GrindingRecipe.of("open_eyeblossom_to_dye", "dye", Ingredient.ofItems(Items.OPEN_EYEBLOSSOM), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
                        GrindingRecipe.of("cactus_flower_to_dye", "dye", Ingredient.ofItems(Items.CACTUS_FLOWER), 1, 6, new ItemStack(Items.PINK_DYE, 3)),
                        GrindingRecipe.of("wildflowers_to_dye", "dye", Ingredient.ofItems(Items.WILDFLOWERS), 1, 6, new ItemStack(Items.YELLOW_DYE, 3)),
                        GrindingRecipe.of("leaf_litter_to_dye", "dye", Ingredient.ofItems(Items.LEAF_LITTER), 1, 6, OutputStack.of(Items.BROWN_DYE, 0.3f))
                );

                offerSmelting(List.of(FactoryItems.CRUSHED_RAW_IRON), RecipeCategory.MISC, Items.IRON_INGOT, 0.5F, 180, "iron_ingot");
                offerSmelting(List.of(FactoryItems.CRUSHED_RAW_COPPER), RecipeCategory.MISC, Items.COPPER_INGOT, 0.5F, 180, "copper_ingot");
                offerSmelting(List.of(FactoryItems.CRUSHED_RAW_GOLD), RecipeCategory.MISC, Items.GOLD_INGOT, 0.7F, 180, "gold_ingot");
                offerBlasting(List.of(FactoryItems.CRUSHED_RAW_IRON), RecipeCategory.MISC, Items.IRON_INGOT, 0.5F, 100, "iron_ingot");
                offerBlasting(List.of(FactoryItems.CRUSHED_RAW_COPPER), RecipeCategory.MISC, Items.COPPER_INGOT, 0.5F, 100, "copper_ingot");
                offerBlasting(List.of(FactoryItems.CRUSHED_RAW_GOLD), RecipeCategory.MISC, Items.GOLD_INGOT, 0.7F, 100, "gold_ingot");

                offerReversibleCompactingRecipesWithCompactingRecipeGroup(RecipeCategory.MISC, FactoryItems.COPPER_NUGGET, RecipeCategory.MISC, Items.COPPER_INGOT, "copper_ingot_from_nugget", null);
                offerReversibleCompactingRecipesWithCompactingRecipeGroup(RecipeCategory.MISC, FactoryItems.STEEL_NUGGET, RecipeCategory.MISC, FactoryItems.STEEL_INGOT, "steel_ingot_from_nugget", null);

                for (var entry : HoneycombItem.UNWAXED_TO_WAXED_BLOCKS.get().entrySet()) {
                    //noinspection deprecation
                    var name = getShortString(entry.getKey().getRegistryEntry()) + "_to_" + getShortString(entry.getValue().getRegistryEntry());
                    for (int i = 1; i <= 4; i++) {
                        of(exporter,
                                GenericPressRecipe.of(name + "_" + i, "press/copper_waxing",
                                        CountedIngredient.ofItems(i, entry.getKey()), CountedIngredient.ofItems(1, Items.HONEYCOMB), 2f, new OutputStack(new ItemStack(entry.getValue(), i), 1, 1)));
                    }
                }

                of(exporter,
                        GenericPressRecipe.of("purpur_block", Ingredient.ofItems(Items.POPPED_CHORUS_FRUIT), 4, 5f, new ItemStack(Items.PURPUR_BLOCK, 1)),
                        GenericPressRecipe.of("sponge", Ingredient.ofItems(Items.WET_SPONGE), 1, 5f, new ItemStack(Items.SPONGE, 1)),
                        GenericPressRecipe.of("sand", Ingredient.ofItems(Items.SAND), 4, 5f, new ItemStack(Items.SANDSTONE, 1)),
                        GenericPressRecipe.of("honeycomb", Ingredient.ofItems(Items.HONEYCOMB), 4, 4f, new ItemStack(Items.HONEYCOMB, 1)),
                        GenericPressRecipe.of("snow_block", Ingredient.ofItems(Items.SNOWBALL), 4, 3f, new ItemStack(Items.SNOW_BLOCK, 1)),
                        GenericPressRecipe.of("red_sand", Ingredient.ofItems(Items.RED_SAND), 4, 5f, new ItemStack(Items.RED_SANDSTONE, 1)),
                        GenericPressRecipe.of("dripstone", Ingredient.ofItems(Items.POINTED_DRIPSTONE), 4, 5f, new ItemStack(Items.DRIPSTONE_BLOCK, 1)),
                        GenericPressRecipe.of("dried_kelp_block", Ingredient.ofItems(Items.DRIED_KELP), 9, 5f, new ItemStack(Items.DRIED_KELP_BLOCK, 1)),
                        GenericPressRecipe.of("slime_block", Ingredient.ofItems(Items.SLIME_BALL), 9, 5f, new ItemStack(Items.SLIME_BLOCK, 1)),
                        GenericPressRecipe.of("bamboo_block", Ingredient.ofItems(Items.BAMBOO), 9, 5f, new ItemStack(Items.BAMBOO_BLOCK, 1)),
                        GenericPressRecipe.of("hay_block", Ingredient.ofItems(Items.WHEAT), 9, 5f, new ItemStack(Items.HAY_BLOCK, 1)),
                        GenericPressRecipe.of("packed_ice", Ingredient.ofItems(Items.ICE), 8, 8f, new ItemStack(Items.PACKED_ICE, 1)),
                        GenericPressRecipe.of("blue_ice", Ingredient.ofItems(Items.PACKED_ICE), 8, 8f, new ItemStack(Items.BLUE_ICE, 1)),
                        GenericPressRecipe.of("experience_bottle", CountedIngredient.ofItems(1, Items.GLASS_BOTTLE),
                                CountedIngredient.ofItems(8, Items.SCULK), 10f, OutputStack.of(Items.EXPERIENCE_BOTTLE)),
                        GenericPressRecipe.of("book", CountedIngredient.ofItems(1, Items.LEATHER),
                                CountedIngredient.ofItems(3, Items.PAPER), 6f, OutputStack.of(Items.BOOK)),
                        GenericPressRecipe.of("paper", Ingredient.ofItems(Items.SUGAR_CANE), 3, 5f, new ItemStack(Items.PAPER, 4)),
                        GenericPressRecipe.of("iron_ingot", Ingredient.ofItems(Items.IRON_NUGGET), 9, 10f, Items.IRON_INGOT),
                        GenericPressRecipe.of("gold_ingot", Ingredient.ofItems(Items.GOLD_NUGGET), 9, 8f, Items.GOLD_INGOT),
                        GenericPressRecipe.of("steel_plate", Ingredient.ofItems(FactoryItems.STEEL_INGOT), 1, 8.5f, new ItemStack(FactoryItems.STEEL_PLATE, 1)),
                        GenericPressRecipe.of("copper_plate", Ingredient.ofItems(Items.COPPER_INGOT), 2, 8.5f, new ItemStack(FactoryItems.COPPER_PLATE, 1)),
                        GenericPressRecipe.of("wooden_plate", Ingredient.ofItems(FactoryItems.SAW_DUST), 2, 5f, new ItemStack(FactoryItems.WOODEN_PLATE, 1)),
                        GenericPressRecipe.of("golden_carrot", CountedIngredient.ofItems(1, Items.CARROT), CountedIngredient.ofItems(8, Items.GOLD_NUGGET),
                                5, OutputStack.of(Items.GOLDEN_CARROT)),
                        GenericPressRecipe.of("glistening_melon_slice", CountedIngredient.ofItems(1, Items.MELON_SLICE), CountedIngredient.ofItems(8, Items.GOLD_NUGGET),
                                5, OutputStack.of(Items.GLISTERING_MELON_SLICE)),
                        GenericPressRecipe.of("golden_apple", CountedIngredient.ofItems(1, Items.APPLE), CountedIngredient.ofItems(8, Items.GOLD_INGOT),
                                5, OutputStack.of(Items.GOLDEN_APPLE)),
                        GenericPressRecipe.of("spray_can", CountedIngredient.ofItems(1, Items.BUCKET), CountedIngredient.ofItems(1, Items.COPPER_INGOT),
                                5f, OutputStack.of(FactoryItems.SPRAY_CAN)),
                        new RecipeEntry<>(recipeKey("press/spray_can_fill"), new FillSprayCanPressRecipe(12)),
                        GenericPressRecipe.of("bundle", CountedIngredient.ofItems(1, Items.LEATHER), CountedIngredient.ofItems(1, Items.STRING),
                                3, OutputStack.of(Items.BUNDLE)),
                        GenericPressRecipe.of("", CountedIngredient.ofItems(1, Items.HEAVY_CORE),
                                CountedIngredient.ofItems(1, Items.BREEZE_ROD),
                                6, OutputStack.of(Items.MACE))
                );

                for (var i = 0; i < 5; i++) {
                    of(exporter, GenericPressRecipe.of("throwable_glass_bottle_" + i, "throwable_glass_bottle", CountedIngredient.ofItems(5 - i, Items.GLASS_BOTTLE),
                            CountedIngredient.ofItems(1, Items.GUNPOWDER), 7 - i, new OutputStack(new ItemStack(FactoryItems.THROWABLE_GLASS_BOTTLE, 5 - i), 1, 1)));
                    of(exporter, GenericPressRecipe.of("lingering_throwable_glass_bottle_" + i, "lingering_throwable_glass_bottle", CountedIngredient.ofItems(5 - i, FactoryItems.THROWABLE_GLASS_BOTTLE),
                            CountedIngredient.ofItemsRemainder(1, Items.DRAGON_BREATH, Items.GLASS_BOTTLE), 7 - i, new OutputStack(new ItemStack(FactoryItems.LINGERING_THROWABLE_GLASS_BOTTLE, 5 - i), 1, 1)));
                }

                of(exporter,
                        new RecipeEntry<>(recipeKey("crafting/spray_can_fill"), new FillSprayCanCraftingRecipe(CraftingRecipeCategory.MISC)),
                        new RecipeEntry<>(recipeKey("crafting/inverted_colored_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingRecipeCategory.REDSTONE,
                                        FactoryItems.INVERTED_LAMP.getDefaultStack(),
                                        Ingredient.ofItems(FactoryItems.LAMP),
                                        List.of(Ingredient.ofItems(Items.REDSTONE_TORCH)))),
                        new RecipeEntry<>(recipeKey("crafting/inverted_caged_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingRecipeCategory.REDSTONE,
                                        FactoryItems.INVERTED_CAGED_LAMP.getDefaultStack(),
                                        Ingredient.ofItems(FactoryItems.CAGED_LAMP),
                                        List.of(Ingredient.ofItems(Items.REDSTONE_TORCH)))),
                        new RecipeEntry<>(recipeKey("crafting/caged_lamp"),
                                new ShapelessNbtCopyRecipe("", CraftingRecipeCategory.REDSTONE,
                                        FactoryItems.CAGED_LAMP.getDefaultStack(),
                                        Ingredient.ofItems(FactoryItems.LAMP),
                                        List.of(Ingredient.ofItems(FactoryItems.METAL_GRID)))),
                        new RecipeEntry<>(recipeKey("crafting/inverted_fixture_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingRecipeCategory.REDSTONE,
                                        FactoryItems.INVERTED_FIXTURE_LAMP.getDefaultStack(),
                                        Ingredient.ofItems(FactoryItems.FIXTURE_LAMP),
                                        List.of(Ingredient.ofItems(Items.REDSTONE_TORCH)))),
                        new RecipeEntry<>(recipeKey("crafting/fixture_lamp"),
                                new ShapelessNbtCopyRecipe("", CraftingRecipeCategory.REDSTONE,
                                        FactoryItems.FIXTURE_LAMP.getDefaultStack().copyWithCount(3),
                                        Ingredient.ofItems(FactoryItems.LAMP),
                                        List.of(Ingredient.ofItems(FactoryItems.WOODEN_PLATE))))
                );


                {
                    var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                            .setComponent(FactoryDataComponents.COLOR, -2)
                            .pattern("dps")
                            .pattern("rgr")
                            .pattern("sps")
                            .input('r', Items.REDSTONE).input('g', Items.GLOWSTONE)
                            .input('s', FactoryItems.SAW_DUST)
                            .input('p', Items.GLASS).input('d', ConventionalItemTags.DYES)
                            .group("polyfactory:colored_lamp")
                            .criterion("get", InventoryChangedCriterion.Conditions.items(Items.GLOWSTONE));

                    x.offerTo(exporter, recipeKey("colored_lamp/glass/regular_a"));
                }
                {
                    var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                            .setComponent(FactoryDataComponents.COLOR, -2)
                            .pattern("sps")
                            .pattern("rgr")
                            .pattern("dps")
                            .input('r', Items.REDSTONE).input('g', Items.GLOWSTONE)
                            .input('s', FactoryItems.SAW_DUST)
                            .input('p', Items.GLASS).input('d', ConventionalItemTags.DYES)
                            .group("polyfactory:colored_lamp")
                            .criterion("get", InventoryChangedCriterion.Conditions.items(Items.GLOWSTONE));
                    x.offerTo(exporter, recipeKey("colored_lamp/glass/regular_b"));
                }

                for (var dye : dyes) {
                    {
                        var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                                .setComponent(FactoryDataComponents.COLOR, DyeColorExtra.getColor(dye.getColor()))

                                .pattern("sps")
                                .pattern("rgr")
                                .pattern("sps")
                                .input('r', Items.REDSTONE).input('g', Items.GLOWSTONE)
                                .input('s', FactoryItems.SAW_DUST)
                                .input('p', Registries.ITEM.get(Identifier.of(dye.getColor().asString() + "_stained_glass")))
                                .group("polyfactory:colored_lamp")
                                .criterion("get", InventoryChangedCriterion.Conditions.items(Items.GLOWSTONE));

                        x.offerTo(exporter, recipeKey("colored_lamp/glass/" + dye.getColor()));
                    }

                    {
                        var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.WINDMILL_SAIL, 1);

                        if (dye != Items.WHITE_DYE) {
                            x.setComponent(DataComponentTypes.DYED_COLOR, new DyedColorComponent(DyeColorExtra.getColor(dye.getColor())));
                        }

                        x.pattern(" sw")
                                .pattern(" ws")
                                .pattern("wsc")
                                .input('w', FactoryItems.WOODEN_PLATE).input('s', Items.STICK)
                                .input('c', Registries.ITEM.get(Identifier.of(dye.getColor().asString() + "_wool")))
                                .group("polyfactory:windmill_sail")
                                .criterion("get_axle", InventoryChangedCriterion.Conditions.items(FactoryItems.WOODEN_PLATE))
                                .offerTo(exporter, recipeKey("windmill_sail/wool/" + dye.getColor()));
                    }

                    {
                        var bundle = Registries.ITEM.get(Identifier.ofVanilla(dye.getColor().asString() + "_bundle"));
                        var shulker = Registries.ITEM.get(Identifier.ofVanilla(dye.getColor().asString() + "_shulker_box"));
                        of(exporter, TransformMixingRecipe.of(dye.getColor().asString() + "_bundle", "bundle_coloring",
                                        this.ingredientFromTag(ItemTags.BUNDLES), List.of(Ingredient.ofItems(dye)), 2, 2, 5, bundle.getDefaultStack()),
                                TransformMixingRecipe.of(dye.getColor().asString() + "_shulker", "shulker_coloring",
                                        this.ingredientFromTag(ItemTags.SHULKER_BOXES), List.of(Ingredient.ofItems(dye)), 2, 2, 5, shulker.getDefaultStack()
                                ));
                    }

                    {
                        var nameSolid = dye.getColor().asString() + "_concrete";
                        var namePowder = nameSolid + "_powder";

                        var powder = Registries.ITEM.get(Identifier.of(namePowder));
                        var solid = Registries.ITEM.get(Identifier.of(nameSolid));
                        of(exporter, GrindingRecipe.of(nameSolid + "_to_powder", "concrete_to_powder",
                                Ingredient.ofItems(solid), 3, 5, powder
                        ));

                        of(exporter, GenericMixingRecipe.ofCounted(namePowder, "concrete_powder",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)), CountedIngredient.ofItems(4, Items.GRAVEL), CountedIngredient.ofItems(1, dye)),
                                4, 1, 13, new ItemStack(powder, 8)));

                        of(exporter, GenericMixingRecipe.ofCounted(nameSolid + "_direct", "concrete_direct",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)),
                                        CountedIngredient.ofItems(4, Items.GRAVEL),
                                        CountedIngredient.ofItems(0, Items.WATER_BUCKET),
                                        CountedIngredient.ofItems(1, dye)),
                                5, 1, 15, new ItemStack(solid, 8)));

                        of(exporter, GenericMixingRecipe.ofCounted(nameSolid + "_from_powder", "concrete_water",
                                List.of(CountedIngredient.ofItems(1, powder), CountedIngredient.ofItems(0, Items.WATER_BUCKET)),
                                1, 1, 4, new ItemStack(solid, 1)));


                        of(exporter, GenericMixingRecipe.ofCounted(nameSolid + "_direct_new", "concrete_direct",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)),
                                        CountedIngredient.ofItems(4, Items.GRAVEL),
                                        CountedIngredient.ofItems(1, dye)),
                                List.of(new FluidInputStack(FactoryFluids.WATER.defaultInstance(), FluidConstants.BOTTLE, FluidConstants.BOTTLE / 10)),
                                5, 1, 15, 0f, new ItemStack(solid, 8)));

                        of(exporter, GenericMixingRecipe.ofCounted(nameSolid + "_from_powder_new", "concrete_water",
                                List.of(CountedIngredient.ofItems(1, powder)),
                                List.of(new FluidInputStack(FactoryFluids.WATER.defaultInstance(), FluidConstants.BOTTLE, FluidConstants.BOTTLE / 10)),
                                1, 1, 4, 0f, new ItemStack(solid, 1)));
                    }
                }

                of(exporter,
                        new RecipeEntry<>(recipeKey("mixing/firework_star"), new FireworkStarMixingRecipe(4, 4, 17))
                );

                of(exporter,
                        new RecipeEntry<>(recipeKey("mixing/artificial_dye"), new ArtificialDyeMixingRecipe(3, 4, 15))
                );
                this.createShapeless(RecipeCategory.COMBAT, FactoryItems.DYNAMITE)
                        .input(Items.PAPER, 1).input(FactoryItems.SAW_DUST, 1).input(Items.GUNPOWDER, 2).input(ItemTags.SAND)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.GUNPOWDER))
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.COMBAT, FactoryItems.STICKY_DYNAMITE)
                        .input(FactoryItems.DYNAMITE, 1).input(Items.SLIME_BALL, 1)
                        .criterion("get_item", InventoryChangedCriterion.Conditions.items(Items.GUNPOWDER))
                        .offerTo(exporter);

                of(exporter,
                        GenericMixingRecipe.ofCounted("treated_dried_kelp", List.of(CountedIngredient.ofItems(16, Items.DRIED_KELP), CountedIngredient.ofItems(1, Items.BLACK_DYE)), 2, 1, 6f, 0.2f, new ItemStack(FactoryItems.TREATED_DRIED_KELP, 16)),
                        GenericMixingRecipe.ofCounted("fermented_spider_eye", List.of(CountedIngredient.ofItems(1, Items.SPIDER_EYE),
                                        CountedIngredient.ofItems(1, Items.SUGAR),
                                        CountedIngredient.ofItems(1, Items.BROWN_MUSHROOM)), 3, 4, 8f,
                                new ItemStack(Items.FERMENTED_SPIDER_EYE, 1)),

                        GenericMixingRecipe.ofCounted("fire_charge", List.of(CountedIngredient.ofItems(1, Items.GUNPOWDER),
                                        CountedIngredient.ofTag(1, itemWrap.getOrThrow(ItemTags.COALS)),
                                        CountedIngredient.ofItems(1, Items.BLAZE_POWDER)), 3, 4, 8f,
                                new ItemStack(Items.FIRE_CHARGE, 1)),
                        GenericMixingRecipe.ofCounted("magma_cream", List.of(CountedIngredient.ofItems(1, Items.SLIME_BALL),
                                        CountedIngredient.ofItems(1, Items.BLAZE_POWDER)), 3, 4, 8f,
                                new ItemStack(Items.MAGMA_CREAM, 1)),
                        GenericMixingRecipe.of("coarse_dirt",
                                List.of(Ingredient.ofItems(Items.DIRT), Ingredient.ofItems(Items.GRAVEL)), 2, 4, 10, new ItemStack(Items.COARSE_DIRT, 2)),
                        GenericMixingRecipe.of("packed_mud",
                                List.of(Ingredient.ofItems(Items.WHEAT), Ingredient.ofItems(Items.MUD)), 2, 4, 10, new ItemStack(Items.PACKED_MUD)),
                        GenericMixingRecipe.ofCounted("cake",
                                List.of(CountedIngredient.ofItems(2, Items.WHEAT), CountedIngredient.ofItems(2, Items.SUGAR),
                                        CountedIngredient.ofItems(1, Items.EGG),
                                        CountedIngredient.ofItemsRemainder(2, Items.MILK_BUCKET, Items.BUCKET)),
                                3, 4, 10, new ItemStack(Items.CAKE)),
                        GenericMixingRecipe.ofCounted("pumpkin_pie",
                                List.of(CountedIngredient.ofItems(1, Items.PUMPKIN), CountedIngredient.ofItems(1, Items.SUGAR),
                                        CountedIngredient.ofItems(1, Items.EGG)),
                                3, 4, 10, new ItemStack(Items.PUMPKIN_PIE, 2)),
                        GenericMixingRecipe.ofCounted("mushroom_stew",
                                List.of(CountedIngredient.ofItems(1, Items.RED_MUSHROOM), CountedIngredient.ofItems(1, Items.BROWN_MUSHROOM),
                                        CountedIngredient.ofItems(2, Items.BOWL)),
                                2, 4, 10, new ItemStack(Items.MUSHROOM_STEW, 2)),
                        GenericMixingRecipe.ofCounted("beetroot_soup",
                                List.of(CountedIngredient.ofItems(3, Items.BEETROOT),
                                        CountedIngredient.ofItems(1, Items.BOWL)),
                                2, 4, 10, new ItemStack(Items.BEETROOT_SOUP, 1)),
                        GenericMixingRecipe.ofCounted("rabbit_stew",
                                List.of(CountedIngredient.ofItems(1, Items.COOKED_RABBIT),
                                        CountedIngredient.ofItems(2, Items.BAKED_POTATO),
                                        CountedIngredient.ofItems(2, Items.CARROT),
                                        CountedIngredient.ofItems(2, Items.BROWN_MUSHROOM),
                                        CountedIngredient.ofItems(3, Items.BOWL)),
                                2, 4, 10, new ItemStack(Items.RABBIT_STEW, 3)),
                        GenericMixingRecipe.ofCounted("cookie",
                                List.of(CountedIngredient.ofItems(2, Items.WHEAT), CountedIngredient.ofItems(1, Items.COCOA_BEANS)),
                                2, 4, 10, new ItemStack(Items.COOKIE, 10)),
                        GenericMixingRecipe.ofCounted("bread",
                                List.of(CountedIngredient.ofItems(2, Items.WHEAT)),
                                2, 4, 10, new ItemStack(Items.BREAD, 1)),
                        GenericMixingRecipe.ofCounted("steel_alloy_mixture",
                                List.of(CountedIngredient.ofItems(3, Items.IRON_INGOT), CountedIngredient.ofItems(2, FactoryItems.COAL_DUST),
                                        CountedIngredient.ofItems(1, Items.REDSTONE)),
                                2, 4, 15, new ItemStack(FactoryItems.STEEL_ALLOY_MIXTURE, 2)),
                        GenericMixingRecipe.ofCounted("tnt",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)), CountedIngredient.ofItems(5, Items.GUNPOWDER)),
                                2, 4, 16, new ItemStack(Items.TNT)),
                        GenericMixingRecipe.ofCounted("redstone_to_glowstone",
                                List.of(CountedIngredient.ofItems(8, Items.REDSTONE), CountedIngredient.ofItems(1, Items.BLAZE_POWDER)),
                                8, 4, 16, new ItemStack(Items.GLOWSTONE_DUST, 12)),
                        GenericMixingRecipe.ofCounted("redstone_to_glowstone_2",
                                List.of(CountedIngredient.ofItems(8, Items.REDSTONE), CountedIngredient.ofItems(3, FactoryItems.NETHERRACK_DUST)),
                                8, 5, 16, new ItemStack(Items.GLOWSTONE_DUST, 8)),
                        GenericMixingRecipe.ofCounted("dynamite",
                                List.of(CountedIngredient.ofItems(1, Items.PAPER), CountedIngredient.ofItems(1, FactoryItems.SAW_DUST), CountedIngredient.ofTag(1, itemWrap.getOrThrow(ItemTags.SAND)), CountedIngredient.ofItems(2, Items.GUNPOWDER)),
                                2, 1, 6f, new ItemStack(FactoryItems.DYNAMITE, 1)),
                        GenericMixingRecipe.ofCounted("gunpowder",
                                List.of(CountedIngredient.ofItems(2, FactoryItems.NETHERRACK_DUST),
                                        CountedIngredient.ofItems(1, FactoryItems.COAL_DUST),
                                        CountedIngredient.ofItems(1, FactoryItems.SAW_DUST)
                                ),
                                2, 1, 15f, 0.3f, new ItemStack(Items.GUNPOWDER)),
                        GenericMixingRecipe.ofCounted("ender_infused_amethyst_shard",
                                List.of(CountedIngredient.ofItems(2, FactoryItems.ENDER_DUST),
                                        CountedIngredient.ofItems(1, Items.AMETHYST_SHARD)
                                ),
                                5, 5, 10f, 0.6f, new ItemStack(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)),
                        GenericMixingRecipe.ofCounted("honey_block_to_liquid", "",
                                List.of(CountedIngredient.ofItems(1, Items.HONEY_BLOCK)),
                                null,
                                6, 3, 10f, 0.3f, ItemStack.EMPTY, List.of(FactoryFluids.HONEY.ofBucket())),
                        GenericMixingRecipe.ofCounted("slime_block_to_liquid", "slime_to_liquid",
                                List.of(CountedIngredient.ofItems(1, Items.SLIME_BLOCK)),
                                null,
                                8, 3, 10f, 0.3f, ItemStack.EMPTY, List.of(FactoryFluids.SLIME.ofBucket())),
                        GenericMixingRecipe.ofCounted("slime_ball_to_liquid", "slime_to_liquid",
                                List.of(CountedIngredient.ofItems(1, Items.SLIME_BALL)),
                                null,
                                1, 3, 10f, 0.3f, ItemStack.EMPTY, List.of(FactoryFluids.SLIME.of(FluidConstants.BLOCK / 9))),
                        GenericMixingRecipe.ofCounted("mud", "",
                                List.of(CountedIngredient.ofItems(1, Items.DIRT)),
                                List.of(FluidInputStack.from(FactoryFluids.WATER.ofBottle())),
                                1, 6, 18f, 0f, Items.MUD.getDefaultStack(), List.of()),
                        GenericMixingRecipe.ofCounted("snow_fluid", "",
                                List.of(),
                                List.of(FluidInputStack.from(FactoryFluids.WATER.of(800))),
                                1, 1, 3f, Float.NEGATIVE_INFINITY, -0.05f, ItemStack.EMPTY, List.of(FactoryFluids.SNOW.of(1000)))
                );

                for (var recipe : ((BrewingRecipeRegistryAccessor) BrewingRecipeRegistry.create(FeatureSet.of(FeatureFlags.VANILLA))).getPotionRecipes()) {
                    var from = FactoryFluids.getPotion(recipe.from());
                    var to = FactoryFluids.getPotion(recipe.to());
                    var b = new StringBuilder("mixing/brewing/");
                    b.append(getShortString(recipe.from()));
                    b.append("_with_");
                    for (var stack : recipe.ingredient().getMatchingItems().toList()) {
                        //noinspection deprecation
                        b.append(getShortString(stack));
                        b.append("_");
                    }
                    b.append("to_");
                    b.append(getShortString(recipe.to()));
                    exporter.accept(
                            recipeKey(b.toString()),
                            new BrewingMixingRecipe(getShortString(recipe.to()).replace("long_", "").replace("strong_", ""), recipe.ingredient(), from, to, FluidConstants.BOTTLE, FluidConstants.BOTTLE * 6,
                                    20, 15, 30, 0.7f, 2f), null
                    );
                }

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.STEEL_GEAR, 3)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .pattern(" i ")
                        .pattern("i i")
                        .pattern(" i ")
                        .input('i', FactoryItems.STEEL_INGOT)
                        .offerTo(exporter);

                this.createShapeless(RecipeCategory.REDSTONE, FactoryItems.LARGE_STEEL_GEAR)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .input(FactoryItems.STEEL_INGOT, 1)
                        .input(FactoryItems.STEEL_GEAR)
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.TACHOMETER, 1)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .pattern("#c#")
                        .pattern("-=-")
                        .pattern("#c#")
                        .input('#', FactoryItems.STEEL_PLATE)
                        .input('c', Items.COPPER_INGOT)
                        .input('-', FactoryItems.AXLE)
                        .input('=', ItemTags.PLANKS)
                        .offerTo(exporter);

                this.createShaped(RecipeCategory.REDSTONE, FactoryItems.STRESSOMETER, 1)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(FactoryItems.STEEL_INGOT))
                        .pattern("#c#")
                        .pattern("-=-")
                        .pattern("#c#")
                        .input('#', FactoryItems.STEEL_PLATE)
                        .input('c', Items.GOLD_INGOT)
                        .input('-', FactoryItems.AXLE)
                        .input('=', ItemTags.PLANKS)
                        .offerTo(exporter);

                exporter.accept(recipeKey("crafting/prt_key_setter"), new PRTKeySetterCraftingRecipe(CraftingRecipeCategory.MISC), null);


                fluidBase(exporter, Items.LAVA_BUCKET, Items.BUCKET, FactoryFluids.LAVA.ofBucket(), SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundEvents.ITEM_BUCKET_EMPTY_LAVA);
                fluidBase(exporter, Items.WATER_BUCKET, Items.BUCKET, FactoryFluids.WATER.ofBucket(), SoundEvents.ITEM_BUCKET_FILL, SoundEvents.ITEM_BUCKET_EMPTY);
                fluidBase(exporter, Items.MILK_BUCKET, Items.BUCKET, FactoryFluids.MILK.ofBucket(), SoundEvents.ITEM_BUCKET_FILL, SoundEvents.ITEM_BUCKET_EMPTY);
                fluidBase(exporter, Items.POWDER_SNOW_BUCKET, Items.BUCKET, FactoryFluids.SNOW.ofBucket(), SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW, SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW);
                fluidBase(exporter, FactoryItems.HONEY_BUCKET, Items.BUCKET, FactoryFluids.HONEY.ofBucket(), FactorySoundEvents.ITEM_BUCKET_FILL_HONEY, FactorySoundEvents.ITEM_BUCKET_EMPTY_HONEY);
                fluidBase(exporter, Items.HONEY_BOTTLE, Items.GLASS_BOTTLE, FactoryFluids.HONEY.of(FluidConstants.BLOCK / 4), SoundEvents.ITEM_BOTTLE_FILL, SoundEvents.ITEM_BOTTLE_EMPTY);
                fluidBase(exporter, FactoryItems.SLIME_BUCKET, Items.BUCKET, FactoryFluids.SLIME.ofBucket(), FactorySoundEvents.ITEM_BUCKET_FILL_SLIME, FactorySoundEvents.ITEM_BUCKET_EMPTY_SLIME);
                fluidBase(exporter, FactoryItems.EXPERIENCE_BUCKET, Items.BUCKET, FactoryFluids.EXPERIENCE.ofBucket(), SoundEvents.ITEM_BUCKET_FILL, SoundEvents.ITEM_BUCKET_EMPTY);

                fluidBasePotion(exporter, Items.POTION, Items.GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.ITEM_BOTTLE_FILL, SoundEvents.ITEM_BOTTLE_EMPTY);
                fluidBasePotion(exporter, Items.SPLASH_POTION, FactoryItems.THROWABLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.ITEM_BOTTLE_FILL, SoundEvents.ITEM_BOTTLE_EMPTY);
                fluidBasePotion(exporter, Items.LINGERING_POTION, FactoryItems.LINGERING_THROWABLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.ITEM_BOTTLE_FILL, SoundEvents.ITEM_BOTTLE_EMPTY);
                fluidBasePotion(exporter, FactoryItems.BRITTLE_POTION, FactoryItems.BRITTLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.ITEM_BOTTLE_FILL, SoundEvents.ITEM_BOTTLE_EMPTY);

                exporter.accept(recipeKey("spout/experience_repair"), new RepairSpoutRecipe(), null);
                exporter.accept(recipeKey("spout/sticky_piston"), SimpleSpoutRecipe.toItem(Items.PISTON, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), Items.STICKY_PISTON, SoundEvents.BLOCK_SLIME_BLOCK_PLACE), null);
                exporter.accept(recipeKey("spout/sticky_conveyor"), SimpleSpoutRecipe.toItem(FactoryItems.CONVEYOR, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), FactoryItems.STICKY_CONVEYOR, SoundEvents.BLOCK_SLIME_BLOCK_PLACE), null);
                exporter.accept(recipeKey("spout/sticky_dynamite"), SimpleSpoutRecipe.toItem(FactoryItems.DYNAMITE, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), FactoryItems.STICKY_DYNAMITE, SoundEvents.BLOCK_SLIME_BLOCK_PLACE), null);
                exporter.accept(recipeKey("spout/brittle_glass_bottle"), SimpleSpoutRecipe.toItem(Items.GLASS_BOTTLE, FactoryFluids.LAVA.of(FluidConstants.NUGGET), FactoryItems.BRITTLE_GLASS_BOTTLE, SoundEvents.BLOCK_GLASS_HIT), null);
                exporter.accept(recipeKey("spout/brittle_potion"), SimpleSpoutRecipe.toItemCopy(Items.POTION, FactoryFluids.LAVA.of(FluidConstants.NUGGET), FactoryItems.BRITTLE_POTION, SoundEvents.BLOCK_GLASS_HIT), null);
                exporter.accept(recipeKey("spout/slimeball"), SimpleSpoutRecipe.template(Items.BOWL,
                        FactoryFluids.SLIME.of(FluidConstants.INGOT), Items.SLIME_BALL, SoundEvents.BLOCK_SLIME_BLOCK_PLACE), null);
                exporter.accept(recipeKey("spout/snowball"), SimpleSpoutRecipe.template(Items.BOWL,
                        FactoryFluids.SNOW.of(FluidConstants.BLOCK / 4), Items.SNOWBALL, SoundEvents.BLOCK_SNOW_PLACE), null);


                spoutAndCasting("honeyed_apple", Items.APPLE, FactoryFluids.HONEY.of(FluidConstants.BLOCK / 4), FactoryItems.HONEYED_APPLE, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 0, 0);
                spoutAndCasting("golden_apple", Items.APPLE,  FactoryFluids.GOLD.of(FluidConstants.INGOT * 7), Items.GOLDEN_APPLE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);
                spoutAndCasting("golden_carrot", Items.CARROT, FactoryFluids.GOLD.of(FluidConstants.NUGGET * 7), Items.GOLDEN_CARROT, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);
                spoutAndCasting("golden_tiny_potato", FactoryItems.TINY_POTATO_SPRING, FactoryFluids.GOLD.of(FluidConstants.INGOT * 3), FactoryItems.GOLDEN_TINY_POTATO_SPRING, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);


                exporter.accept(recipeKey("casting/cauldron/honey_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.HONEY.of(FluidConstants.BLOCK), Items.HONEY_BLOCK, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 0), null);
                exporter.accept(recipeKey("casting/cauldron/slime_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.SLIME.of(FluidConstants.BLOCK), Items.SLIME_BLOCK, SoundEvents.BLOCK_SLIME_BLOCK_PLACE, 0), null);
                exporter.accept(recipeKey("casting/cauldron/snow_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.SNOW.of(FluidConstants.BLOCK), Items.SNOW_BLOCK, SoundEvents.BLOCK_SNOW_PLACE, 0), null);

                destructiveItemCreatingFluidInteraction(exporter, "honey_lava", 1, List.of(FactoryFluids.HONEY.of(8000), FactoryFluids.LAVA.of(4000)),
                        OutputStack.of(FactoryItems.CRISPY_HONEY, 0.15f, 1), ParticleTypes.LARGE_SMOKE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value());

                for (var i : List.of(FactoryFluids.WATER, FactoryFluids.MILK)) {
                    for (var s : List.of(
                            new Pair<>(FactoryFluids.LAVA.of(3000), OutputStack.of(Items.FLINT, 0.15f, 1)),
                            new Pair<>(FactoryFluids.IRON.of(FluidConstants.NUGGET), OutputStack.of(Items.IRON_NUGGET, 0.9f, 1)),
                            new Pair<>(FactoryFluids.GOLD.of(FluidConstants.NUGGET), OutputStack.of(Items.GOLD_NUGGET, 0.9f, 1)),
                            new Pair<>(FactoryFluids.COPPER.of(FluidConstants.NUGGET), OutputStack.of(FactoryItems.COPPER_NUGGET, 0.9f, 1)),
                            new Pair<>(FactoryFluids.STEEL.of(FluidConstants.NUGGET), OutputStack.of(FactoryItems.STEEL_NUGGET, 0.9f, 1))
                    )) {
                        destructiveItemCreatingFluidInteraction(exporter, FactoryRegistries.FLUID_TYPES.getId(i).getPath() + "_" + FactoryRegistries.FLUID_TYPES.getId(s.getLeft().type()).getPath(),
                                (int) (3000 / FluidConstants.NUGGET), List.of(i.of(s.getLeft().amount() * 2), s.getLeft()),
                                s.getRight(), ParticleTypes.LARGE_SMOKE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value());
                    }
                }


                exporter.accept(recipeKey("fluid_interaction/snow_melting"), new SimpleFluidInteractionRecipe(
                        List.of(FluidInputStack.from(FactoryFluids.SNOW.of(1000))),
                        List.of(FactoryFluids.WATER.of(800)),
                        List.of(),
                        Optional.empty(), Optional.empty(), 0, BlockHeat.TORCH, Float.POSITIVE_INFINITY, 1
                ), null);

                smelteryOreSet(FactoryFluids.IRON, FactoryFluidConstants.IRON_INGOT_MELTING, ItemTags.IRON_ORES, Items.RAW_IRON, Items.RAW_IRON_BLOCK,
                        FactoryItems.CRUSHED_RAW_IRON, Items.IRON_INGOT, Items.IRON_NUGGET, Items.IRON_BLOCK);
                of(exporter,
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) * 3 / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 3 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.CHIPPED_ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) * 2 / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 3 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.DAMAGED_ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 2 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.BUCKET, FactoryFluids.IRON.of(FluidConstants.INGOT * 2) , FactoryFluidConstants.IRON_INGOT_MELTING * 2),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.CAULDRON, FactoryFluids.IRON.of(FluidConstants.INGOT * 6) , FactoryFluidConstants.IRON_INGOT_MELTING * 6),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.CHAIN, FactoryFluids.IRON.of(FluidConstants.INGOT / 2) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, FactoryFluids.IRON.of(FluidConstants.INGOT / 2) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_BARS, FactoryFluids.IRON.of(FluidConstants.INGOT * 6 / 16) , FactoryFluidConstants.IRON_INGOT_MELTING * 6 / 16),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_DOOR, FactoryFluids.IRON.of(FluidConstants.INGOT * 1) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_TRAPDOOR, FactoryFluids.IRON.of(FluidConstants.INGOT * 2) , FactoryFluidConstants.IRON_INGOT_MELTING * 2),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.MINECART, FactoryFluids.IRON.of(FluidConstants.INGOT * 3) , FactoryFluidConstants.IRON_INGOT_MELTING * 3)
                );

                smelteryOreSet(FactoryFluids.GOLD, FactoryFluidConstants.GOLD_INGOT_MELTING, ItemTags.GOLD_ORES, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK,
                        FactoryItems.CRUSHED_RAW_GOLD, Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.GOLD_BLOCK);
                smelteryOreSet(FactoryFluids.COPPER, FactoryFluidConstants.COPPER_INGOT_MELTING, ItemTags.COPPER_ORES, Items.RAW_COPPER, Items.RAW_COPPER_BLOCK,
                        FactoryItems.CRUSHED_RAW_COPPER, Items.COPPER_INGOT, FactoryItems.COPPER_NUGGET, Items.COPPER_BLOCK);


                of(exporter,
                        //SimpleSmelteryRecipe.of("minecraft_copper", ConventionalItemTags.STORAGE_BLOCKS_COPPER, FactoryFluids.COPPER.of(FluidConstants.BLOCK), FactoryFluidConstants.COPPER_INGOT_COOLING * 9),
                        SimpleSmelteryRecipe.of("minecraft_copper", FactoryItems.PIPE, FactoryFluids.COPPER.of(FluidConstants.INGOT * 2), FactoryFluidConstants.COPPER_INGOT_MELTING * 2)
                );

                moldRecipes(FactoryItems.PIPE_MOLD, FactoryFluids.COPPER.of(FluidConstants.INGOT * 2), FactoryItems.PIPE);

                smelteryOreSet(FactoryFluids.STEEL, FactoryFluidConstants.STEEL_INGOT_MELTING, null, FactoryItems.STEEL_ALLOY_MIXTURE, null,
                        null, FactoryItems.STEEL_INGOT, FactoryItems.STEEL_NUGGET, FactoryItems.STEEL_BLOCK);

                // Smeltery Steel recycling
                of(exporter,
                        SimpleSmelteryRecipe.of("polyfactory_steel", FactoryItems.STEEL_BUTTON, FactoryFluids.STEEL.of(FluidConstants.INGOT),FactoryFluidConstants.STEEL_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("polyfactory_steel", FactoryItems.STEEL_MACHINE_GEARBOX, FactoryFluids.STEEL.of(FluidConstants.INGOT * 10),FactoryFluidConstants.STEEL_INGOT_MELTING * 10),
                        SimpleSmelteryRecipe.of("polyfactory_steel", FactoryItems.STEEL_GEAR, FactoryFluids.STEEL.of(FluidConstants.INGOT),FactoryFluidConstants.STEEL_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("polyfactory_steel", FactoryItems.LARGE_STEEL_GEAR, FactoryFluids.STEEL.of(FluidConstants.INGOT * 2),FactoryFluidConstants.STEEL_INGOT_MELTING)
                );

                this.spoutMolds(ConventionalItemTags.INGOTS, FactoryItems.INGOT_MOLD);
                this.spoutMolds(ConventionalItemTags.NUGGETS, FactoryItems.NUGGET_MOLD);
                this.spoutMolds(FactoryItemTags.PIPES, FactoryItems.PIPE_MOLD);


                this.spoutMolds(FactoryItemTags.MOLD_SHAPE_BOTTLE, FactoryItems.BOTTLE_MOLD);
                this.spoutMolds(FactoryItemTags.MOLD_SHAPE_THROWABLE_BOTTLE, FactoryItems.THROWABLE_BOTTLE_MOLD);
                this.spoutMolds(FactoryItemTags.MOLD_SHAPE_BRITTLE_BOTTLE, FactoryItems.BRITTLE_BOTTLE_MOLD);

                moldRecipes(FactoryItems.BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), Items.GLASS_BOTTLE);
                moldRecipes(FactoryItems.THROWABLE_BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryItems.THROWABLE_GLASS_BOTTLE);
                moldRecipes(FactoryItems.BRITTLE_BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryItems.BRITTLE_GLASS_BOTTLE);

                exporter.accept(recipeKey("casting/glass_pane"), SimpleCastingRecipe.fluid(FactoryFluids.GLASS.of(FluidConstants.BLOCK * 6 / 16), Items.GLASS_PANE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null);
                exporter.accept(recipeKey("casting/iron_bars"), SimpleCastingRecipe.fluid(FactoryFluids.IRON.of(FluidConstants.BLOCK * 6 / 16), Items.IRON_BARS, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null);

                of(exporter,
                        SimpleSmelteryRecipe.of("minecraft_glass", ItemTags.SMELTS_TO_GLASS, FactoryFluids.GLASS.of(FluidConstants.BLOCK * 10 / 9), FactoryFluidConstants.GLASS_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_glass", ConventionalItemTags.GLASS_BLOCKS, FactoryFluids.GLASS.of(FluidConstants.BLOCK), FactoryFluidConstants.GLASS_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_glass", ConventionalItemTags.GLASS_PANES, FactoryFluids.GLASS.of(FluidConstants.BLOCK * 6 / 16), FactoryFluidConstants.GLASS_MELTING * 6 / 16),
                        SimpleSmelteryRecipe.of("minecraft_glass", FactoryItemTags.GLASS_BOTTLE_MELTABLE, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryFluidConstants.GLASS_MELTING / 2),
                        new RecipeEntry<>(recipeKey("casting/cauldron/glass"),
                                SimpleCauldronCastingRecipe.toItem(FactoryFluids.GLASS.of(FluidConstants.BLOCK), Items.GLASS, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60))
                );

            }

            private void moldRecipes(SpoutMolds molds, FluidStack<?> fluidStack, Item item) {
                exporter.accept(recipeKey("spout/" + Registries.ITEM.getId(item).getPath() + "_with_molds"),
                        SimpleSpoutRecipe.templateDamaged(molds.tag(), fluidStack,
                                item, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60), null
                );

                exporter.accept(recipeKey("casting/" + Registries.ITEM.getId(item).getPath() + "_with_molds"),
                        SimpleCastingRecipe.templateDamaged(molds.tag(), fluidStack,
                                item, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60), null
                );
            }

            private void spoutAndCasting(String name, Item base, FluidStack<Unit> fluid, Item result, SoundEvent sound, int coolingSpout, int coolingCast) {
                exporter.accept(recipeKey("spout/" + name),
                        SimpleSpoutRecipe.toItem(base, fluid, result, sound, coolingSpout), null
                );

                exporter.accept(recipeKey("casting/" + name),
                        SimpleCastingRecipe.toItem(base, fluid, result, sound, coolingCast), null
                );
            }

            private void spoutMolds(TagKey<Item> tag, SpoutMolds mold) {
                CookingRecipeJsonBuilder.create(Ingredient.ofItems(mold.clay()), RecipeCategory.TOOLS, mold.hardened(),
                        0, 40, RecipeSerializer.SMELTING, SmeltingRecipe::new)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(Items.CLAY))
                        .group("polyfactory:hardened_molds")
                        .offerTo(exporter);

                exporter.accept(recipeKey("casting/mold_" + mold.name().getPath()),
                        SimpleCastingRecipe.toItem(tag, FactoryFluids.STEEL.of(FluidConstants.INGOT * 4),
                                mold.mold(), FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null
                );

                this.createShaped(RecipeCategory.TOOLS, mold.clay()).pattern(" x ").pattern("xox").pattern(" x ")
                        .input('x', Items.CLAY_BALL)
                        .input('o', tag)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(Items.CLAY))
                        .group("polyfactory:clay_molds")
                        .offerTo(CraftingWithLeftoverRecipe.asExporter(exporter, ingredientFromTag(ConventionalItemTags.INGOTS)));
            }

            private void spoutMolds(Item tag, SpoutMolds mold) {
                CookingRecipeJsonBuilder.create(Ingredient.ofItems(mold.clay()), RecipeCategory.TOOLS, mold.hardened(),
                                0, 40, RecipeSerializer.SMELTING, SmeltingRecipe::new)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(Items.CLAY))
                        .group("polyfactory:hardened_molds")
                        .offerTo(exporter);

                exporter.accept(recipeKey("casting/mold_" + mold.name().getPath()),
                        SimpleCastingRecipe.toItem(tag, FactoryFluids.STEEL.of(FluidConstants.INGOT * 4),
                                mold.mold(), FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null
                );

                this.createShaped(RecipeCategory.TOOLS, mold.clay()).pattern(" x ").pattern("xox").pattern(" x ")
                        .input('x', Items.CLAY_BALL)
                        .input('o', tag)
                        .criterion("steel_ingot", InventoryChangedCriterion.Conditions.items(Items.CLAY))
                        .group("polyfactory:clay_molds")
                        .offerTo(CraftingWithLeftoverRecipe.asExporter(exporter, ingredientFromTag(ConventionalItemTags.INGOTS)));
            }


            private void smelteryOreSet(FluidType<?> fluidType, int ingotTime, TagKey<Item> oreBlock, Item raw, Item rawBlock, Item crushed, Item ingot, Item nugget, Item block) {
                var group = FactoryRegistries.FLUID_TYPES.getId(fluidType).toUnderscoreSeparatedString();
                of(exporter,
                        raw != null ? SimpleSmelteryRecipe.of(group, raw, fluidType.of(FluidConstants.INGOT + FluidConstants.NUGGET * 2), ingotTime * 5 / 4) : null,
                        oreBlock != null ? SimpleSmelteryRecipe.of(group, oreBlock, fluidType.of(FluidConstants.INGOT * 2), ingotTime * 3 / 2) : null,
                        rawBlock != null ? SimpleSmelteryRecipe.of(group, rawBlock, fluidType.of((FluidConstants.INGOT + FluidConstants.NUGGET) * 9), (ingotTime * 3 / 2) * 9) : null,
                        crushed != null ? SimpleSmelteryRecipe.of(group, crushed, fluidType.of(FluidConstants.INGOT + FluidConstants.NUGGET * 2), (ingotTime * 5 / 4)) : null
                );

                if (ingot != null) {
                    of(exporter,
                            SimpleSmelteryRecipe.of(group, ingot, fluidType.of(FluidConstants.INGOT), ingotTime)
                    );

                    moldRecipes(FactoryItems.INGOT_MOLD, fluidType.of(FluidConstants.INGOT), ingot);
                }

                if (nugget != null) {
                    of(exporter,
                            SimpleSmelteryRecipe.of(group, nugget, fluidType.of(FluidConstants.NUGGET), ingotTime / 9)
                    );

                    moldRecipes(FactoryItems.NUGGET_MOLD, fluidType.of(FluidConstants.NUGGET), nugget);
                }

                if (block != null) {
                    of(exporter,
                            SimpleSmelteryRecipe.of(group, block, fluidType.of(FluidConstants.BLOCK), ingotTime * 9),
                            new RecipeEntry<>(recipeKey("casting/cauldron/" + getRecipeName(block)),
                                    SimpleCauldronCastingRecipe.toItem(fluidType.of(FluidConstants.BLOCK), block, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60))
                    );
                }

            }

            private void destructiveItemCreatingFluidInteraction(RecipeExporter exporter, String name, int repeats, List<FluidStack<?>> fluids, OutputStack item, ParticleEffect particleEffect, SoundEvent soundEvent) {
                var base = recipeKey("fluid_interaction/" + name);
                var remove = recipeKey("fluid_interaction/" + name + "_leftover");

                exporter.accept(base, new SimpleFluidInteractionRecipe(
                        fluids.stream().map(FluidInputStack::from).toList(), List.of(), List.of(item), Optional.of(particleEffect), Optional.of(Registries.SOUND_EVENT.getEntry(soundEvent)), 0.25f,
                        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, repeats
                ), null);

                exporter.accept(remove, new RemovingFluidInteractionRecipe(
                        fluids.stream().map(FluidInputStack::from).toList(), Optional.of(particleEffect), Optional.of(Registries.SOUND_EVENT.getEntry(soundEvent)), 0.25f,
                        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
                ), null);
            }


            private void fluidBase(RecipeExporter exporter, Item withFluid, Item emptyContainer, FluidStack<?> fluid, SoundEvent fillSound, SoundEvent emptySound) {
                var base = Registries.ITEM.getId(withFluid).getPath();

                exporter.accept(recipeKey("drain/from_" + base), SimpleDrainRecipe.fromItem(withFluid, fluid, emptyContainer, emptySound), null);
                exporter.accept(recipeKey("drain/to_" + base), SimpleDrainRecipe.toItem(emptyContainer, fluid, withFluid, fillSound), null);
                exporter.accept(recipeKey("spout/to_" + base), SimpleSpoutRecipe.toItem(emptyContainer, fluid, withFluid, fillSound), null);
            }

            @SuppressWarnings("SameParameterValue")
            private void fluidBasePotion(RecipeExporter exporter, Item withFluid, Item emptyContainer, long fluid, SoundEvent fillSound, SoundEvent emptySound) {
                var base = Registries.ITEM.getId(withFluid).getPath();

                exporter.accept(recipeKey("drain/from_" + base), PotionAddDrainRecipe.of(withFluid, fluid, emptyContainer, emptySound), null);
                exporter.accept(recipeKey("drain/to_" + base), PotionRemoveDrainRecipe.of(emptyContainer, fluid, withFluid, fillSound), null);
                exporter.accept(recipeKey("spout/to_" + base), PotionSpoutRecipe.of(emptyContainer, fluid, withFluid, fillSound), null);
            }

            private String getShortString(RegistryEntry<?> entry) {
                //noinspection OptionalGetWithoutIsPresent
                var key = entry.getKey().get().getValue();

                return key.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? key.getPath().replace("/", "_") : key.toUnderscoreSeparatedString();
            }


            public void of(RecipeExporter exporter, RecipeEntry<?>... recipes) {
                for (var recipe : recipes) {
                    if (recipe != null){
                        exporter.accept(recipe.id(), recipe.value(), null);
                    }
                }
            }
        };
    }

    @Override
    public String getName() {
        return "recipes";
    }
}
