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
import eu.pb4.polyfactory.mixin.PotionBrewingAccessor;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.casting.SimpleCauldronCastingRecipe;
import eu.pb4.polyfactory.recipe.drain.PotionAddDrainRecipe;
import eu.pb4.polyfactory.recipe.drain.PotionRemoveDrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.RemovingFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.fluid.SimpleFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.grinding.SimpleGrindingRecipe;
import eu.pb4.polyfactory.recipe.grinding.StrippingGrindingRecipe;
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
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

class RecipesProvider extends FabricRecipeProvider {
    public RecipesProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput exporter) {
        return new RecipeProvider(registryLookup, exporter) {
            @Override
            public void buildRecipes() {
                var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);

                nineBlockStorageRecipes(RecipeCategory.MISC, FactoryItems.STEEL_INGOT, RecipeCategory.MISC, FactoryItems.STEEL_BLOCK, "steel_block", null, "steel_ingot_from_block", null);

                //noinspection unchecked
                var dyes = (List<DyeItem>) (Object) List.of(Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE, Items.WHITE_DYE);

                this.shapeless(RecipeCategory.TOOLS, FactoryItems.STEEL_ALLOY_MIXTURE)
                        .requires(Items.IRON_INGOT, 2).requires(FactoryItems.COAL_DUST).requires(Items.REDSTONE)
                        .unlockedBy("get_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.STEEL_BUTTON)
                        .pattern("##")
                        .pattern("##")
                        .define('#', FactoryItems.STEEL_NUGGET)
                        .unlockedBy("get_s", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                SimpleCookingRecipeBuilder.smelting(
                                Ingredient.of(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 80)
                        .unlockedBy("get_steel_mixture", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_ALLOY_MIXTURE))
                        .save(output);

                SimpleCookingRecipeBuilder.blasting(
                                Ingredient.of(FactoryItems.STEEL_ALLOY_MIXTURE), RecipeCategory.MISC, FactoryItems.STEEL_INGOT, 0.4f, 40)
                        .unlockedBy("get_steel_mixture", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_ALLOY_MIXTURE))
                        .save(output, recipeKey("steel_ingot_blasting"));

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.METAL_GRID, 1)
                        .pattern(" w ")
                        .pattern("www")
                        .pattern(" w ")
                        .define('w', Items.IRON_BARS)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.GRINDER, 1)
                        .pattern(" i ")
                        .pattern("www")
                        .pattern("ppp")
                        .define('i', Items.STONE_SLAB)
                        .define('w', Items.SMOOTH_STONE_SLAB)
                        .define('p', ItemTags.PLANKS)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.STEEL_MACHINE_GEARBOX, 1)
                        .pattern("sss")
                        .pattern("g g")
                        .pattern("s s")
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.MIXER, 1)
                        .pattern("g")
                        .pattern("i")
                        .pattern("c")
                        .define('i', Items.IRON_SHOVEL)
                        .define('c', Items.CAULDRON)
                        .define('g', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CABLE, 8)
                        .pattern("iii")
                        .pattern("ccc")
                        .pattern("iii")
                        .define('i', FactoryItems.TREATED_DRIED_KELP)
                        .define('c', Items.COPPER_INGOT)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.TREATED_DRIED_KELP))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.GATED_CABLE, 1)
                        .pattern("rsr")
                        .pattern("cxc")
                        .pattern("rsr")
                        .define('s', Items.SMOOTH_STONE_SLAB)
                        .define('x', Items.COPPER_INGOT)
                        .define('r', Items.REDSTONE)
                        .define('c', FactoryItems.CABLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.CABLE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PRESS, 1)
                        .pattern(" g ")
                        .pattern("sis")
                        .pattern("wcw")
                        .define('i', Items.ANVIL)
                        .define('c', Items.SMOOTH_STONE)
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('w', ItemTags.PLANKS)
                        .define('g', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.MINER, 1)
                        .pattern(" i ")
                        .pattern("wsw")
                        .pattern("waw")
                        .define('i', Items.IRON_INGOT).define('s', FactoryItems.STEEL_GEAR)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', FactoryItems.AXLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PLACER, 1)
                        .pattern("wsw")
                        .pattern("gig")
                        .pattern("waw")
                        .define('i', FactoryItems.INTEGRATED_CIRCUIT)
                        .define('s', FactoryItems.STEEL_GEAR)
                        .define('g', Items.GOLD_BLOCK)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', FactoryItems.MINER)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.STEAM_ENGINE, 1)
                        .pattern("www")
                        .pattern("wbw")
                        .pattern("aaa")
                        .define('b', Items.BLAST_FURNACE)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', Items.DEEPSLATE_BRICKS)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.EJECTOR, 1)
                        .pattern("iii")
                        .pattern("wgw")
                        .pattern("waw")
                        .define('i', FactoryItems.WOODEN_PLATE)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', FactoryItems.AXLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.FAN, 1)
                        .pattern("iii")
                        .pattern("wsw")
                        .pattern("waw")
                        .define('i', Items.IRON_BARS).define('s', Items.IRON_INGOT)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', FactoryItems.AXLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CRAFTER, 1)
                        .pattern("iai")
                        .pattern("wsw")
                        .pattern("www")
                        .define('i', FactoryItems.STEEL_GEAR).define('s', Items.CRAFTING_TABLE)
                        .define('w', FactoryItems.STEEL_PLATE).define('a', FactoryItems.AXLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.TURNTABLE, 1)
                        .pattern("-")
                        .pattern("i")
                        .define('-', ItemTags.WOODEN_SLABS)
                        .define('i', FactoryItems.AXLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shapeless(RecipeCategory.MISC, FactoryItems.WORKBENCH, 1)
                        .requires(Items.CRAFTING_TABLE)
                        .requires(FactoryItems.WOODEN_PLATE)
                        .unlockedBy("get_plate", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                        .save(output);

                this.shapeless(RecipeCategory.MISC, FactoryItems.BLUEPRINT_WORKBENCH, 1)
                        .requires(Items.BLUE_DYE)
                        .requires(Items.PAPER)
                        .requires(FactoryItems.WORKBENCH)
                        .unlockedBy("get_plate", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.MISC, FactoryItems.MOLDMAKING_TABLE, 1)
                        .pattern(" i ")
                        .pattern("pPp")
                        .define('p', FactoryItems.WOODEN_PLATE)
                        .define('P', ItemTags.PLANKS)
                        .define('i', Items.COPPER_INGOT)
                        .unlockedBy("get_plate", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                        .save(output);

                this.spoutAndCasting("moldmaking_table", Items.CRAFTING_TABLE, FactoryFluids.COPPER.ofIngot(),  FactoryItems.MOLDMAKING_TABLE, SoundEvents.FIRE_EXTINGUISH, 40, 40);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PIPE, 3)
                        .pattern("ppp")
                        .pattern("i i")
                        .pattern("ppp")
                        .define('p', FactoryItems.COPPER_PLATE)
                        .define('i', Items.COPPER_INGOT)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.FILTERED_PIPE, 1)
                        .pattern(" w ")
                        .pattern("pgp")
                        .pattern(" w ")
                        .define('p', FactoryItems.PIPE)
                        .define('w', FactoryItems.STEEL_PLATE)
                        .define('g', FactoryItems.TREATED_DRIED_KELP)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.PIPE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_VALVE_PIPE, 1)
                        .pattern("rwr")
                        .pattern("pwp")
                        .pattern("rwr")
                        .define('p', FactoryItems.PIPE)
                        .define('w', FactoryItems.STEEL_PLATE)
                        .define('r', Items.REDSTONE)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.PIPE))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.DRAIN, 1)
                        .pattern("pip")
                        .pattern("p p")
                        .pattern("ppp")
                        .define('p', FactoryItems.COPPER_PLATE)
                        .define('i', Items.IRON_INGOT)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.MECHANICAL_DRAIN, 1)
                        .pattern("s")
                        .pattern("a")
                        .pattern("d")
                        .define('d', FactoryItems.DRAIN)
                        .define('s', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .define('a', FactoryItems.AXLE)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.DRAIN))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.MECHANICAL_SPOUT, 1)
                        .pattern(" s ")
                        .pattern("cac")
                        .pattern("pdp")
                        .define('d', Items.SMOOTH_STONE)
                        .define('p', ItemTags.PLANKS)
                        .define('s', FactoryItems.STEEL_MACHINE_GEARBOX)
                        .define('a', FactoryItems.PIPE)
                        .define('c', FactoryItems.COPPER_PLATE)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PORTABLE_FLUID_TANK, 1)
                        .pattern("p")
                        .pattern("t")
                        .pattern("p")
                        .define('p', FactoryItems.COPPER_PLATE)
                        .define('t', FactoryItems.FLUID_TANK)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.FLUID_TANK, 1)
                        .pattern("pgp")
                        .pattern("gcg")
                        .pattern("pgp")
                        .define('p', FactoryItems.COPPER_PLATE)
                        .define('g', Items.TINTED_GLASS)
                        .define('c', Items.COPPER_BLOCK)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PUMP, 1)
                        .pattern(" w ")
                        .pattern("pgp")
                        .pattern(" w ")
                        .define('p', FactoryItems.PIPE)
                        .define('w', FactoryItems.WOODEN_PLATE)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.PIPE))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.NOZZLE, 1)
                        .pattern(" c ")
                        .pattern("psc")
                        .pattern(" c ")
                        .define('p', FactoryItems.PIPE)
                        .define('c', FactoryItems.COPPER_PLATE)
                        .define('s', FactoryItems.STEEL_PLATE) // replace with something else?
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.PIPE))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PRESSURE_FLUID_GUN, 1)
                        .pattern("ipn")
                        .pattern("sw ")
                        .define('p', FactoryItems.PIPE)
                        .define('n', FactoryItems.NOZZLE)
                        .define('i', Items.NETHERITE_INGOT)
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('w', FactoryItems.WOODEN_PLATE)
                        .unlockedBy("get_copper", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.PIPE))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.FUNNEL, 1)
                        .pattern("wp ")
                        .pattern(" o ")
                        .pattern("wp ")
                        .define('p', FactoryItems.WOODEN_PLATE).define('o', ItemTags.WOOL)
                        .define('w', FactoryItems.STEEL_PLATE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.SLOT_AWARE_FUNNEL, 1)
                        .pattern("gcg")
                        .pattern("sfs")
                        .pattern("gsg")
                        .define('c', FactoryItems.REDSTONE_CHIP)
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('f', FactoryItems.FUNNEL)
                        .define('g', Items.GOLD_INGOT)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.SPLITTER, 1)
                        .pattern("sss")
                        .pattern("fxf")
                        .pattern("sss")
                        .define('f', FactoryItems.FUNNEL).define('s', FactoryItems.STEEL_PLATE)
                        .define('x', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.FUNNEL))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_PACKER, 1)
                        .pattern("wsw")
                        .pattern("shs")
                        .pattern("wsw")
                        .define('w', FactoryItems.WOODEN_PLATE).define('s', FactoryItems.STEEL_PLATE)
                        .define('h', Items.HOPPER)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.MISC, FactoryItems.CONTAINER, 1)
                        .pattern("www")
                        .pattern("wsw")
                        .pattern("www")
                        .define('w', FactoryItems.WOODEN_PLATE).define('s', FactoryItems.STEEL_INGOT)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.MISC, FactoryItems.DEEP_STORAGE_CONTAINER, 1)
                        .pattern("www")
                        .pattern("wsw")
                        .pattern("www")
                        .define('w', FactoryItems.STEEL_PLATE)
                        .define('s', FactoryItems.STEEL_BLOCK)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_FILTER)
                        .pattern(" p ")
                        .pattern("imi")
                        .pattern(" p ")
                        .define('p', FactoryItems.WOODEN_PLATE)
                        .define('m', FactoryItems.STRING_MESH)
                        .define('i', Items.IRON_INGOT)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                        .save(output);


                this.shaped(RecipeCategory.MISC, FactoryItems.STRING_MESH)
                        .pattern(" s ")
                        .pattern("sss")
                        .pattern(" s ")
                        .define('s', Items.STRING)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STRING))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_COUNTER)
                        .pattern("pqp")
                        .pattern("scs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE).define('c', Items.COPPER_INGOT).define('p', FactoryItems.WOODEN_PLATE)
                        .define('q', Items.QUARTZ)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_INPUT)
                        .pattern("r-r")
                        .pattern("srs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE).define('c', Items.COPPER_INGOT)
                        .define('-', Items.SMOOTH_STONE_SLAB).define('r', Items.REDSTONE)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_OUTPUT)
                        .pattern("r-r")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('-', Items.SMOOTH_STONE_SLAB)
                        .define('r', Items.REDSTONE)
                        .define('b', Items.REDSTONE_BLOCK)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.SPEAKER)
                        .pattern("c-c")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('-', Items.NOTE_BLOCK)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.RECORD_PLAYER)
                        .pattern("c-c")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('-', Items.JUKEBOX)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);



                this.shaped(RecipeCategory.REDSTONE, FactoryItems.ARITHMETIC_OPERATOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .define('c', Items.COPPER_INGOT)
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('-', FactoryItems.INTEGRATED_CIRCUIT)
                        .define('w', FactoryItems.CABLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.DATA_COMPARATOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .define('c', Items.QUARTZ)
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('-', FactoryItems.REDSTONE_CHIP)
                        .define('w', FactoryItems.CABLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.DATA_EXTRACTOR, 1)
                        .pattern("csc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .define('c', Items.LAPIS_LAZULI)
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('-', FactoryItems.REDSTONE_CHIP)
                        .define('w', FactoryItems.CABLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PROGRAMMABLE_DATA_EXTRACTOR, 1)
                        .pattern("c c")
                        .pattern("s-w")
                        .pattern("c c")
                        .define('c', Items.COPPER_INGOT)
                        .define('s', FactoryItems.INTEGRATED_CIRCUIT)
                        .define('-', FactoryItems.DATA_EXTRACTOR)
                        .define('w', FactoryItems.CABLE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.DATA_MEMORY, 1)
                        .pattern("cpc")
                        .pattern("s-s")
                        .pattern("cwc")
                        .define('c', Items.GOLD_INGOT).define('s', FactoryItems.STEEL_PLATE)
                        .define('p', FactoryItems.COPPER_PLATE)
                        .define('-', FactoryItems.REDSTONE_CHIP)
                        .define('w', FactoryItems.CABLE)

                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.DATA_MEMORY, 1)
                        .requires(FactoryItems.DATA_MEMORY)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.DATA_MEMORY))
                        .save(output, "polyfactory:data_memory_clear");

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.WIRELESS_REDSTONE_RECEIVER)
                        .requires(FactoryItems.REDSTONE_OUTPUT)
                        .requires(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .requires(Items.LIGHTNING_ROD)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.WIRELESS_REDSTONE_TRANSMITTER)
                        .requires(FactoryItems.REDSTONE_INPUT)
                        .requires(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .requires(Items.LIGHTNING_ROD)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)
                        .requires(FactoryItems.STEEL_PLATE)
                        .requires(FactoryItems.INTEGRATED_CIRCUIT)
                        .requires(FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .requires(Items.COPPER_INGOT)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CONVEYOR, 1)
                        .pattern("xxx")
                        .pattern("scs")
                        .pattern("xxx")
                        .define('s', Items.SMOOTH_STONE_SLAB).define('c', Items.COPPER_INGOT).define('x', FactoryItems.TREATED_DRIED_KELP)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.STICKY_CONVEYOR)
                        .requires(FactoryItems.CONVEYOR).requires(Items.SLIME_BALL)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.CONVEYOR))
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.INVERTED_REDSTONE_LAMP)
                        .requires(Items.REDSTONE_LAMP).requires(Items.REDSTONE_TORCH)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.REDSTONE_LAMP))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.AXLE, 8)
                        .pattern("www")
                        .pattern("sss")
                        .pattern("www")
                        .define('w', ConventionalItemTags.STRIPPED_LOGS).define('s', FactoryItems.STEEL_INGOT)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CHAIN_DRIVE, 1)
                        .pattern("waw")
                        .pattern("csc")
                        .pattern("waw")
                        .define('w', FactoryItems.WOODEN_PLATE)
                        .define('a', FactoryItems.AXLE)
                        .define('c', Items.IRON_CHAIN)
                        .define('s', FactoryItems.STEEL_GEAR)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CHAIN_LIFT, 1)
                        .pattern("csc")
                        .pattern("c c")
                        .pattern("ppp")
                        .define('c', Items.IRON_CHAIN)
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('p', FactoryItems.STEEL_PLATE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.NIXIE_TUBE, 1)
                        .pattern(" g ")
                        .pattern("gsg")
                        .pattern("cpc")
                        .define('p', FactoryItems.WOODEN_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('g', Items.GLASS_PANE)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.NIXIE_TUBE_CONTROLLER)
                        .pattern("csc")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('b', FactoryItems.INTEGRATED_CIRCUIT)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.GAUGE)
                        .pattern(" s ")
                        .pattern("sps")
                        .pattern(" s ")
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('p', Items.PAPER)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.HOLOGRAM_PROJECTOR, 1)
                        .pattern("pdp")
                        .pattern("ese")
                        .pattern("pcp")
                        .define('d', Items.DIAMOND)
                        .define('e', FactoryItems.ENDER_INFUSED_AMETHYST_SHARD)
                        .define('p', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('s', FactoryItems.INTEGRATED_CIRCUIT)
                        .unlockedBy("get_steel", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ENDER_PEARL))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.ITEM_READER)
                        .pattern("plp")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('l', Items.LECTERN)
                        .define('p', FactoryItems.WOODEN_PLATE)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.BLOCK_OBSERVER)
                        .pattern("sos")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('o', Items.OBSERVER)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.TEXT_INPUT)
                        .pattern("s-s")
                        .pattern("pbt")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', FactoryItems.CABLE)
                        .define('p', Items.IRON_INGOT)
                        .define('-', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .define('t', FactoryItems.TREATED_DRIED_KELP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.DIGITAL_CLOCK)
                        .pattern("c-c")
                        .pattern("sbs")
                        .pattern("scs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('c', FactoryItems.CABLE)
                        .define('-', Items.CLOCK)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PLANTER)
                        .pattern("sgs")
                        .pattern("pbp")
                        .pattern("sgs")
                        .define('s', FactoryItems.STEEL_PLATE)
                        .define('b', FactoryItems.REDSTONE_CHIP)
                        .define('p', ItemTags.PLANKS)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_PLATE))
                        .save(output);


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
                this.shaped(RecipeCategory.REDSTONE, FactoryItems.GEARBOX, 1)
                        .pattern("sgs")
                        .pattern("gwg")
                        .pattern("sgs")
                        .define('w', FactoryItems.AXLE)
                        .define('s', ItemTags.PLANKS)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.AXLE))
                        .save(output);


                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CLUTCH, 1)
                        .pattern("sps")
                        .pattern("gwg")
                        .pattern("sps")
                        .define('w', FactoryItems.AXLE)
                        .define('s', ItemTags.PLANKS)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .define('p', FactoryItems.STEEL_PLATE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.AXLE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.GEARSHIFT, 1)
                        .pattern("sps")
                        .pattern("wgw")
                        .pattern("sps")
                        .define('w', FactoryItems.AXLE)
                        .define('s', ItemTags.PLANKS)
                        .define('g', FactoryItems.STEEL_GEAR)
                        .define('p', FactoryItems.COPPER_PLATE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.AXLE))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.SMELTERY_CORE, 1)
                        .pattern(" d ")
                        .pattern("dpd")
                        .pattern(" d ")
                        .define('d', Items.DEEPSLATE_BRICKS)
                        .define('p', FactoryItems.STEEL_PLATE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.PRIMITIVE_SMELTERY, 1)
                        .pattern("ibi")
                        .pattern("sfs")
                        .pattern("ibi")
                        .define('i', Items.IRON_INGOT)
                        .define('s', Items.SMOOTH_STONE_SLAB)
                        .define('f', Items.BLAST_FURNACE)
                        .define('b', Items.BRICKS)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.CASTING_TABLE, 1)
                        .pattern("i i")
                        .pattern("iii")
                        .pattern("s s")
                        .define('i', Items.IRON_INGOT)
                        .define('s', Items.SMOOTH_STONE_SLAB)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.SMELTERY_FAUCET, 1)
                        .pattern("csc")
                        .pattern("cic")
                        .define('c', Items.COPPER_INGOT)
                        .define('i', Items.IRON_INGOT)
                        .define('s', Items.STONE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.HAND_CRANK)
                        .pattern("ip")
                        .pattern("l ")
                        .define('l', ConventionalItemTags.STRIPPED_LOGS).define('p', ItemTags.PLANKS).define('i', Items.IRON_INGOT)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.REDSTONE_CHIP)
                        .pattern("grg")
                        .pattern("rcr")
                        .pattern("grg")
                        .define('g', Items.GOLD_NUGGET)
                        .define('c', FactoryItems.COPPER_PLATE)
                        .define('r', Items.REDSTONE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.INTEGRATED_CIRCUIT)
                        .pattern("qtq")
                        .pattern("cpr")
                        .pattern("ggg")
                        .define('g', Items.GOLD_NUGGET)
                        .define('q', Items.QUARTZ)
                        .define('c', FactoryItems.COPPER_PLATE)
                        .define('r', FactoryItems.REDSTONE_CHIP)
                        .define('t', FactoryItems.TREATED_DRIED_KELP)
                        .define('p', FactoryItems.WOODEN_PLATE)
                        .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.WRENCH)
                        .pattern(" i ")
                        .pattern(" ti")
                        .pattern("i  ")
                        .define('i', FactoryItems.STEEL_INGOT)
                        .define('t', FactoryItems.TREATED_DRIED_KELP)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .save(output);

                this.shaped(RecipeCategory.MISC, FactoryItems.TINY_POTATO_SPRING)
                        .pattern(" p ")
                        .pattern(" s ")
                        .pattern(" w ")
                        .define('p', Items.POTATO)
                        .define('s', FactoryItems.STEEL_INGOT)
                        .define('w', FactoryItems.WOODEN_PLATE)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DRAGON_EGG))
                        .save(output);

                of(output,
                        ColoringCraftingRecipe.of(itemWrap, "cable_color", FactoryItems.CABLE),
                        ColoringCraftingRecipe.of(itemWrap, "lamp_color", FactoryItems.LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_color", FactoryItems.INVERTED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "caged_lamp_color", FactoryItems.CAGED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_caged_color", FactoryItems.INVERTED_CAGED_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "fixture_lamp_color", FactoryItems.FIXTURE_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "inverted_fixture_color", FactoryItems.INVERTED_FIXTURE_LAMP),
                        ColoringCraftingRecipe.of(itemWrap, "portable_redstone_transmitter_color", FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, 1)
                );
                of(output,
                        ColoringMixingRecipe.of("cable_color", FactoryItems.CABLE, 2, 6, 10),
                        ColoringMixingRecipe.of("lamp_color", FactoryItems.LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_lamp_color", FactoryItems.INVERTED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("caged_lamp_color", FactoryItems.CAGED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_caged_lamp_color", FactoryItems.INVERTED_CAGED_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("fixture_lamp_color", FactoryItems.FIXTURE_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("inverted_fixture_lamp_color", FactoryItems.INVERTED_FIXTURE_LAMP, 2, 6, 10),
                        ColoringMixingRecipe.of("portable_redstone_transmitter_color", FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, 1, 2, 6, 10)
                );

                of(output,
                        SimpleGrindingRecipe.of("coal_dust", Ingredient.of(Items.COAL), 1, 5, 8, FactoryItems.COAL_DUST),
                        SimpleGrindingRecipe.of("coal_dust_charcoal", Ingredient.of(Items.CHARCOAL), 1.5, 5, 14, OutputStack.of(FactoryItems.COAL_DUST, 0.8f)),
                        SimpleGrindingRecipe.of("netherrack_dust", Ingredient.of(Items.NETHERRACK), 2, 8, 15,
                                OutputStack.of(FactoryItems.NETHERRACK_DUST, 1, 2), OutputStack.of(FactoryItems.NETHERRACK_DUST, 0.25f, 5)),
                        SimpleGrindingRecipe.of("ender_dust", Ingredient.of(Items.ENDER_PEARL), 6, 20, 25,
                                OutputStack.of(FactoryItems.ENDER_DUST, 1, 2), OutputStack.of(FactoryItems.ENDER_DUST, 0.20f, 2)),
                        SimpleGrindingRecipe.of("planks_saw_dust", tag(ItemTags.PLANKS), 1, 5, 6,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.6f, 3), OutputStack.of(Items.STICK, 0.4f, 3)),
                        SimpleGrindingRecipe.of("stripped_logs_saw_dust", "logs_saw_dust", tag(ConventionalItemTags.STRIPPED_LOGS), 1, 5, 6,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.8f, 6), OutputStack.of(Items.STICK, 0.4f, 8)),
                        SimpleGrindingRecipe.of("stripped_wood_saw_dust", "logs_saw_dust", tag(ConventionalItemTags.STRIPPED_WOODS), 1, 5, 6,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.8f, 6), OutputStack.of(Items.STICK, 0.4f, 8)),
                        StrippingGrindingRecipe.of("wood_stripping", tag(ItemTags.LOGS), 0.5, 4, 10,
                                OutputStack.of(FactoryItems.SAW_DUST, 0.3f, 2)),
                        SimpleGrindingRecipe.of("stone_to_cobblestone", Ingredient.of(Items.STONE), 2, 5, 15, Items.COBBLESTONE),
                        SimpleGrindingRecipe.of("cobblestone_to_gravel", Ingredient.of(Items.COBBLESTONE), 4, 6, 15, Items.GRAVEL),
                        SimpleGrindingRecipe.of("diorite_to_gravel", Ingredient.of(Items.DIORITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.05f, 1)),
                        SimpleGrindingRecipe.of("andesite_to_gravel", Ingredient.of(Items.ANDESITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.05f, 1)),
                        SimpleGrindingRecipe.of("granite_to_gravel", Ingredient.of(Items.GRANITE), 4, 6, 15, OutputStack.of(Items.GRAVEL), OutputStack.of(Items.QUARTZ, 0.1f, 1)),
                        SimpleGrindingRecipe.of("gravel_to_sand", Ingredient.of(Items.GRAVEL), 4, 3, 15, Items.SAND),

                        SimpleGrindingRecipe.of("iron_ingot_to_nuggets", Ingredient.of(Items.IRON_INGOT), 2, 5, 10, new ItemStack(Items.IRON_NUGGET, 9)),
                        SimpleGrindingRecipe.of("gold_ingot_to_nuggets", Ingredient.of(Items.GOLD_INGOT), 1.8, 5, 10, new ItemStack(Items.GOLD_NUGGET, 9)),
                        SimpleGrindingRecipe.of("nether_wart", Ingredient.of(Items.NETHER_WART_BLOCK), 1.8, 5, 10,
                                OutputStack.of(Items.NETHER_WART), OutputStack.of(Items.NETHER_WART, 0.4f, 5)
                        ),

                        SimpleGrindingRecipe.of("iron_ore_to_raw", tag(ItemTags.IRON_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_IRON, 1f, 1), OutputStack.of(Items.RAW_IRON, 0.6f, 5)
                        ),
                        SimpleGrindingRecipe.of("gold_ore_to_raw", tag(ItemTags.GOLD_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_GOLD, 1f, 1), OutputStack.of(Items.RAW_GOLD, 0.6f, 5)
                        ),
                        SimpleGrindingRecipe.of("copper_ore_to_raw", tag(ItemTags.COPPER_ORES), 6, 12, 38,
                                OutputStack.of(Items.RAW_COPPER, 1f, 2), OutputStack.of(Items.RAW_COPPER, 0.6f, 20)
                        ),
                        SimpleGrindingRecipe.of("bone_to_bone_meal", Ingredient.of(Items.BONE), 1, 5, 10, OutputStack.of(Items.BONE_MEAL, 1, 3), OutputStack.of(Items.BONE_MEAL, 0.5f, 2)),
                        SimpleGrindingRecipe.of("blaze_rod_to_powder", Ingredient.of(Items.BLAZE_ROD), 2, 5, 10, OutputStack.of(Items.BLAZE_POWDER, 1, 2), OutputStack.of(Items.BLAZE_POWDER, 0.5f, 2)),
                        SimpleGrindingRecipe.of("breeze_rod_to_charge", Ingredient.of(Items.BREEZE_ROD), 2, 5, 10, OutputStack.of(Items.WIND_CHARGE, 1, 4), OutputStack.of(Items.WIND_CHARGE, 0.5f, 2)),
                        SimpleGrindingRecipe.of("glowstone_to_powder", Ingredient.of(Items.GLOWSTONE), 1, 5, 10, new ItemStack(Items.GLOWSTONE_DUST, 4)),
                        SimpleGrindingRecipe.of("sugar", Ingredient.of(Items.SUGAR_CANE), 1, 5, OutputStack.of(Items.SUGAR, 1f, 1), OutputStack.of(Items.SUGAR, 0.4f, 1)),

                        // Crushed raw metals

                        SimpleGrindingRecipe.of("crushed_raw_iron", Ingredient.of(Items.RAW_IRON), 8, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_IRON, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_IRON, 0.4f, 1)
                        ),
                        SimpleGrindingRecipe.of("crushed_raw_gold", Ingredient.of(Items.RAW_GOLD), 6, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_GOLD, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_GOLD, 0.4f, 1)
                        ),
                        SimpleGrindingRecipe.of("crushed_raw_copper", Ingredient.of(Items.RAW_COPPER), 6, 12, 38,
                                OutputStack.of(FactoryItems.CRUSHED_RAW_COPPER, 1f, 1), OutputStack.of(FactoryItems.CRUSHED_RAW_COPPER, 0.4f, 1)
                        ),

                        // Flower to Dye
                        SimpleGrindingRecipe.of("dandelion_to_dye", "dye", Ingredient.of(Items.DANDELION), 1, 6, new ItemStack(Items.YELLOW_DYE, 3)),
                        SimpleGrindingRecipe.of("torch_flower_to_dye", "dye", Ingredient.of(Items.TORCHFLOWER), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
                        SimpleGrindingRecipe.of("orchid_to_dye", "dye", Ingredient.of(Items.BLUE_ORCHID), 1, 6, new ItemStack(Items.LIGHT_BLUE_DYE, 3)),
                        SimpleGrindingRecipe.of("allium_to_dye", "dye", Ingredient.of(Items.ALLIUM), 1, 6, new ItemStack(Items.MAGENTA_DYE, 3)),
                        SimpleGrindingRecipe.of("azure_bluet_to_dye", "dye", Ingredient.of(Items.AZURE_BLUET), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                        SimpleGrindingRecipe.of("daisy_to_dye", "dye", Ingredient.of(Items.OXEYE_DAISY), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                        SimpleGrindingRecipe.of("red_tulip_to_dye", "dye", Ingredient.of(Items.RED_TULIP), 1, 6, new ItemStack(Items.RED_DYE, 3)),
                        SimpleGrindingRecipe.of("orange_tulip_to_dye", "dye", Ingredient.of(Items.ORANGE_TULIP), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
                        SimpleGrindingRecipe.of("white_tulip_to_dye", "dye", Ingredient.of(Items.WHITE_TULIP), 1, 6, new ItemStack(Items.LIGHT_GRAY_DYE, 3)),
                        SimpleGrindingRecipe.of("pink_tulip_to_dye", "dye", Ingredient.of(Items.PINK_TULIP), 1, 6, new ItemStack(Items.PINK_DYE, 3)),
                        SimpleGrindingRecipe.of("cornflower_to_dye", "dye", Ingredient.of(Items.CORNFLOWER), 1, 6, new ItemStack(Items.BLUE_DYE, 3)),
                        SimpleGrindingRecipe.of("lily_to_dye", "dye", Ingredient.of(Items.LILY_OF_THE_VALLEY), 1, 6, new ItemStack(Items.WHITE_DYE, 3)),
                        SimpleGrindingRecipe.of("wither_rose_to_dye", "dye", Ingredient.of(Items.WITHER_ROSE), 1, 6, new ItemStack(Items.BLACK_DYE, 3)),
                        SimpleGrindingRecipe.of("sunflower_to_dye", "dye", Ingredient.of(Items.SUNFLOWER), 1, 6, new ItemStack(Items.YELLOW_DYE, 6)),
                        SimpleGrindingRecipe.of("lilac_to_dye", "dye", Ingredient.of(Items.LILAC), 1, 6, new ItemStack(Items.MAGENTA_DYE, 6)),
                        SimpleGrindingRecipe.of("peony_to_dye", "dye", Ingredient.of(Items.PEONY), 1, 6, new ItemStack(Items.PINK_DYE, 6)),
                        SimpleGrindingRecipe.of("rose_to_dye", "dye", Ingredient.of(Items.ROSE_BUSH), 1, 6, new ItemStack(Items.RED_DYE, 6)),
                        SimpleGrindingRecipe.of("pitcher_to_dye", "dye", Ingredient.of(Items.PITCHER_PLANT), 1, 6, new ItemStack(Items.CYAN_DYE, 6)),
                        SimpleGrindingRecipe.of("cactus_to_dye", "dye", Ingredient.of(Items.CACTUS), 1, 6, new ItemStack(Items.GREEN_DYE, 3)),
                        SimpleGrindingRecipe.of("closed_eyeblossom_to_dye", "dye", Ingredient.of(Items.CLOSED_EYEBLOSSOM), 1, 6, new ItemStack(Items.GRAY_DYE, 3)),
                        SimpleGrindingRecipe.of("open_eyeblossom_to_dye", "dye", Ingredient.of(Items.OPEN_EYEBLOSSOM), 1, 6, new ItemStack(Items.ORANGE_DYE, 3)),
                        SimpleGrindingRecipe.of("cactus_flower_to_dye", "dye", Ingredient.of(Items.CACTUS_FLOWER), 1, 6, new ItemStack(Items.PINK_DYE, 3)),
                        SimpleGrindingRecipe.of("wildflowers_to_dye", "dye", Ingredient.of(Items.WILDFLOWERS), 1, 6, new ItemStack(Items.YELLOW_DYE, 3)),
                        SimpleGrindingRecipe.of("leaf_litter_to_dye", "dye", Ingredient.of(Items.LEAF_LITTER), 1, 6, OutputStack.of(Items.BROWN_DYE, 0.3f))
                );

                oreSmelting(List.of(FactoryItems.CRUSHED_RAW_IRON), RecipeCategory.MISC, Items.IRON_INGOT, 0.5F, 180, "iron_ingot");
                oreSmelting(List.of(FactoryItems.CRUSHED_RAW_COPPER), RecipeCategory.MISC, Items.COPPER_INGOT, 0.5F, 180, "copper_ingot");
                oreSmelting(List.of(FactoryItems.CRUSHED_RAW_GOLD), RecipeCategory.MISC, Items.GOLD_INGOT, 0.7F, 180, "gold_ingot");
                oreBlasting(List.of(FactoryItems.CRUSHED_RAW_IRON), RecipeCategory.MISC, Items.IRON_INGOT, 0.5F, 100, "iron_ingot");
                oreBlasting(List.of(FactoryItems.CRUSHED_RAW_COPPER), RecipeCategory.MISC, Items.COPPER_INGOT, 0.5F, 100, "copper_ingot");
                oreBlasting(List.of(FactoryItems.CRUSHED_RAW_GOLD), RecipeCategory.MISC, Items.GOLD_INGOT, 0.7F, 100, "gold_ingot");

                nineBlockStorageRecipesWithCustomPacking(RecipeCategory.MISC, Items.COPPER_NUGGET, RecipeCategory.MISC, Items.COPPER_INGOT, "copper_ingot_from_nugget", null);
                nineBlockStorageRecipesWithCustomPacking(RecipeCategory.MISC, FactoryItems.STEEL_NUGGET, RecipeCategory.MISC, FactoryItems.STEEL_INGOT, "steel_ingot_from_nugget", null);

                for (var entry : HoneycombItem.WAXABLES.get().entrySet()) {
                    //noinspection deprecation
                    var name = getShortString(entry.getKey().builtInRegistryHolder()) + "_to_" + getShortString(entry.getValue().builtInRegistryHolder());
                    for (int i = 1; i <= 4; i++) {
                        of(output,
                                GenericPressRecipe.of(name + "_" + i, "press/copper_waxing",
                                        CountedIngredient.ofItems(i, entry.getKey()), CountedIngredient.ofItems(1, Items.HONEYCOMB), 2f, new OutputStack(new ItemStack(entry.getValue(), i), 1, 1)));
                    }
                }

                of(output,
                        GenericPressRecipe.of("purpur_block", Ingredient.of(Items.POPPED_CHORUS_FRUIT), 4, 5f, new ItemStack(Items.PURPUR_BLOCK, 1)),
                        GenericPressRecipe.of("sponge", Ingredient.of(Items.WET_SPONGE), 1, 5f, new ItemStack(Items.SPONGE, 1)),
                        GenericPressRecipe.of("sand", Ingredient.of(Items.SAND), 4, 5f, new ItemStack(Items.SANDSTONE, 1)),
                        GenericPressRecipe.of("honeycomb", Ingredient.of(Items.HONEYCOMB), 4, 4f, new ItemStack(Items.HONEYCOMB_BLOCK, 1)),
                        GenericPressRecipe.of("snow_block", Ingredient.of(Items.SNOWBALL), 4, 3f, new ItemStack(Items.SNOW_BLOCK, 1)),
                        GenericPressRecipe.of("red_sand", Ingredient.of(Items.RED_SAND), 4, 5f, new ItemStack(Items.RED_SANDSTONE, 1)),
                        GenericPressRecipe.of("dripstone", Ingredient.of(Items.POINTED_DRIPSTONE), 4, 5f, new ItemStack(Items.DRIPSTONE_BLOCK, 1)),
                        GenericPressRecipe.of("dried_kelp_block", Ingredient.of(Items.DRIED_KELP), 9, 5f, new ItemStack(Items.DRIED_KELP_BLOCK, 1)),
                        GenericPressRecipe.of("slime_block", Ingredient.of(Items.SLIME_BALL), 9, 5f, new ItemStack(Items.SLIME_BLOCK, 1)),
                        GenericPressRecipe.of("bamboo_block", Ingredient.of(Items.BAMBOO), 9, 5f, new ItemStack(Items.BAMBOO_BLOCK, 1)),
                        GenericPressRecipe.of("hay_block", Ingredient.of(Items.WHEAT), 9, 5f, new ItemStack(Items.HAY_BLOCK, 1)),
                        GenericPressRecipe.of("packed_ice", Ingredient.of(Items.ICE), 8, 8f, new ItemStack(Items.PACKED_ICE, 1)),
                        GenericPressRecipe.of("blue_ice", Ingredient.of(Items.PACKED_ICE), 8, 8f, new ItemStack(Items.BLUE_ICE, 1)),
                        GenericPressRecipe.of("experience_bottle", CountedIngredient.ofItems(1, Items.GLASS_BOTTLE),
                                CountedIngredient.ofItems(8, Items.SCULK), 10f, OutputStack.of(Items.EXPERIENCE_BOTTLE)),
                        GenericPressRecipe.of("book", CountedIngredient.ofItems(1, Items.LEATHER),
                                CountedIngredient.ofItems(3, Items.PAPER), 6f, OutputStack.of(Items.BOOK)),
                        GenericPressRecipe.of("paper", Ingredient.of(Items.SUGAR_CANE), 3, 5f, new ItemStack(Items.PAPER, 4)),
                        GenericPressRecipe.of("iron_ingot", Ingredient.of(Items.IRON_NUGGET), 9, 10f, Items.IRON_INGOT),
                        GenericPressRecipe.of("gold_ingot", Ingredient.of(Items.GOLD_NUGGET), 9, 8f, Items.GOLD_INGOT),
                        GenericPressRecipe.of("steel_plate", Ingredient.of(FactoryItems.STEEL_INGOT), 1, 8.5f, new ItemStack(FactoryItems.STEEL_PLATE, 1)),
                        GenericPressRecipe.of("copper_plate", Ingredient.of(Items.COPPER_INGOT), 2, 8.5f, new ItemStack(FactoryItems.COPPER_PLATE, 1)),
                        GenericPressRecipe.of("wooden_plate", Ingredient.of(FactoryItems.SAW_DUST), 2, 5f, new ItemStack(FactoryItems.WOODEN_PLATE, 1)),
                        GenericPressRecipe.of("golden_carrot", CountedIngredient.ofItems(1, Items.CARROT), CountedIngredient.ofItems(8, Items.GOLD_NUGGET),
                                5, OutputStack.of(Items.GOLDEN_CARROT)),
                        GenericPressRecipe.of("glistening_melon_slice", CountedIngredient.ofItems(1, Items.MELON_SLICE), CountedIngredient.ofItems(8, Items.GOLD_NUGGET),
                                5, OutputStack.of(Items.GLISTERING_MELON_SLICE)),
                        GenericPressRecipe.of("golden_apple", CountedIngredient.ofItems(1, Items.APPLE), CountedIngredient.ofItems(8, Items.GOLD_INGOT),
                                5, OutputStack.of(Items.GOLDEN_APPLE)),
                        GenericPressRecipe.of("spray_can", CountedIngredient.ofItems(1, Items.BUCKET), CountedIngredient.ofItems(1, Items.COPPER_INGOT),
                                5f, OutputStack.of(FactoryItems.SPRAY_CAN)),
                        new RecipeHolder<>(recipeKey("press/spray_can_fill"), new FillSprayCanPressRecipe(12)),
                        GenericPressRecipe.of("bundle", CountedIngredient.ofItems(1, Items.LEATHER), CountedIngredient.ofItems(1, Items.STRING),
                                3, OutputStack.of(Items.BUNDLE)),
                        GenericPressRecipe.of("mace", CountedIngredient.ofItems(1, Items.HEAVY_CORE),
                                CountedIngredient.ofItems(1, Items.BREEZE_ROD),
                                6, OutputStack.of(Items.MACE))
                        /*GenericPressRecipe.of("test_fluid", "", CountedIngredient.ofTag(1, fakeTagList(ItemTags.LEAVES)),
                                CountedIngredient.EMPTY,
                                3, List.of(OutputStack.of(Items.STICK)), List.of(FactoryFluids.WATER.of(FluidConstants.NUGGET)))*/
                );

                for (var i = 0; i < 5; i++) {
                    of(output, GenericPressRecipe.of("throwable_glass_bottle_" + i, "throwable_glass_bottle", CountedIngredient.ofItems(5 - i, Items.GLASS_BOTTLE),
                            CountedIngredient.ofItems(1, Items.GUNPOWDER), 7 - i, new OutputStack(new ItemStack(FactoryItems.THROWABLE_GLASS_BOTTLE, 5 - i), 1, 1)));
                    of(output, GenericPressRecipe.of("lingering_throwable_glass_bottle_" + i, "lingering_throwable_glass_bottle", CountedIngredient.ofItems(5 - i, FactoryItems.THROWABLE_GLASS_BOTTLE),
                            CountedIngredient.ofItemsRemainder(1, Items.DRAGON_BREATH, Items.GLASS_BOTTLE), 7 - i, new OutputStack(new ItemStack(FactoryItems.LINGERING_THROWABLE_GLASS_BOTTLE, 5 - i), 1, 1)));
                }

                of(output,
                        new RecipeHolder<>(recipeKey("crafting/spray_can_fill"), new FillSprayCanCraftingRecipe(CraftingBookCategory.MISC)),
                        new RecipeHolder<>(recipeKey("crafting/inverted_colored_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingBookCategory.REDSTONE,
                                        FactoryItems.INVERTED_LAMP.getDefaultInstance(),
                                        Ingredient.of(FactoryItems.LAMP),
                                        List.of(Ingredient.of(Items.REDSTONE_TORCH)))),
                        new RecipeHolder<>(recipeKey("crafting/inverted_caged_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingBookCategory.REDSTONE,
                                        FactoryItems.INVERTED_CAGED_LAMP.getDefaultInstance(),
                                        Ingredient.of(FactoryItems.CAGED_LAMP),
                                        List.of(Ingredient.of(Items.REDSTONE_TORCH)))),
                        new RecipeHolder<>(recipeKey("crafting/caged_lamp"),
                                new ShapelessNbtCopyRecipe("", CraftingBookCategory.REDSTONE,
                                        FactoryItems.CAGED_LAMP.getDefaultInstance(),
                                        Ingredient.of(FactoryItems.LAMP),
                                        List.of(Ingredient.of(FactoryItems.METAL_GRID)))),
                        new RecipeHolder<>(recipeKey("crafting/inverted_fixture_lamp"),
                                new ShapelessNbtCopyRecipe("lamp_invertion", CraftingBookCategory.REDSTONE,
                                        FactoryItems.INVERTED_FIXTURE_LAMP.getDefaultInstance(),
                                        Ingredient.of(FactoryItems.FIXTURE_LAMP),
                                        List.of(Ingredient.of(Items.REDSTONE_TORCH)))),
                        new RecipeHolder<>(recipeKey("crafting/fixture_lamp"),
                                new ShapelessNbtCopyRecipe("", CraftingBookCategory.REDSTONE,
                                        FactoryItems.FIXTURE_LAMP.getDefaultInstance().copyWithCount(3),
                                        Ingredient.of(FactoryItems.LAMP),
                                        List.of(Ingredient.of(FactoryItems.WOODEN_PLATE))))
                );


                {
                    var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                            .setComponent(FactoryDataComponents.COLOR, -2)
                            .pattern("dps")
                            .pattern("rgr")
                            .pattern("sps")
                            .define('r', Items.REDSTONE).define('g', Items.GLOWSTONE)
                            .define('s', FactoryItems.SAW_DUST)
                            .define('p', Items.GLASS).define('d', ConventionalItemTags.DYES)
                            .group("polyfactory:colored_lamp")
                            .unlockedBy("get", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GLOWSTONE));

                    x.save(output, recipeKey("colored_lamp/glass/regular_a"));
                }
                {
                    var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                            .setComponent(FactoryDataComponents.COLOR, -2)
                            .pattern("sps")
                            .pattern("rgr")
                            .pattern("dps")
                            .define('r', Items.REDSTONE).define('g', Items.GLOWSTONE)
                            .define('s', FactoryItems.SAW_DUST)
                            .define('p', Items.GLASS).define('d', ConventionalItemTags.DYES)
                            .group("polyfactory:colored_lamp")
                            .unlockedBy("get", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GLOWSTONE));
                    x.save(output, recipeKey("colored_lamp/glass/regular_b"));
                }

                for (var dye : dyes) {
                    {
                        var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.LAMP, 1)
                                .setComponent(FactoryDataComponents.COLOR, DyeColorExtra.getColor(dye.getDyeColor()))

                                .pattern("sps")
                                .pattern("rgr")
                                .pattern("sps")
                                .define('r', Items.REDSTONE).define('g', Items.GLOWSTONE)
                                .define('s', FactoryItems.SAW_DUST)
                                .define('p', BuiltInRegistries.ITEM.getValue(Identifier.parse(dye.getDyeColor().getSerializedName() + "_stained_glass")))
                                .group("polyfactory:colored_lamp")
                                .unlockedBy("get", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GLOWSTONE));

                        x.save(output, recipeKey("colored_lamp/glass/" + dye.getDyeColor()));
                    }

                    {
                        var x = new CompShapedRecipeJsonBuilder(itemWrap, RecipeCategory.REDSTONE, FactoryItems.WINDMILL_SAIL, 1);

                        if (dye != Items.WHITE_DYE) {
                            x.setComponent(DataComponents.DYED_COLOR, new DyedItemColor(DyeColorExtra.getColor(dye.getDyeColor())));
                        }

                        x.pattern(" sw")
                                .pattern(" ws")
                                .pattern("wsc")
                                .define('w', FactoryItems.WOODEN_PLATE).define('s', Items.STICK)
                                .define('c', BuiltInRegistries.ITEM.getValue(Identifier.parse(dye.getDyeColor().getSerializedName() + "_wool")))
                                .group("polyfactory:windmill_sail")
                                .unlockedBy("get_axle", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.WOODEN_PLATE))
                                .save(output, recipeKey("windmill_sail/wool/" + dye.getDyeColor()));
                    }

                    {
                        var bundle = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dye.getDyeColor().getSerializedName() + "_bundle"));
                        var shulker = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dye.getDyeColor().getSerializedName() + "_shulker_box"));
                        of(output, TransformMixingRecipe.of(dye.getDyeColor().getSerializedName() + "_bundle", "bundle_coloring",
                                        this.tag(ItemTags.BUNDLES), List.of(Ingredient.of(dye)), 2, 2, 5, bundle.getDefaultInstance()),
                                TransformMixingRecipe.of(dye.getDyeColor().getSerializedName() + "_shulker", "shulker_coloring",
                                        this.tag(ItemTags.SHULKER_BOXES), List.of(Ingredient.of(dye)), 2, 2, 5, shulker.getDefaultInstance()
                                ));
                    }

                    {
                        var nameSolid = dye.getDyeColor().getSerializedName() + "_concrete";
                        var namePowder = nameSolid + "_powder";

                        var powder = BuiltInRegistries.ITEM.getValue(Identifier.parse(namePowder));
                        var solid = BuiltInRegistries.ITEM.getValue(Identifier.parse(nameSolid));
                        of(output, SimpleGrindingRecipe.of(nameSolid + "_to_powder", "concrete_to_powder",
                                Ingredient.of(solid), 3, 5, powder
                        ));

                        of(output, GenericMixingRecipe.ofCounted(namePowder, "concrete_powder",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)), CountedIngredient.ofItems(4, Items.GRAVEL), CountedIngredient.ofItems(1, dye)),
                                4, 1, 13, new ItemStack(powder, 8)));

                        of(output, GenericMixingRecipe.ofCounted(nameSolid + "_direct", "concrete_direct",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)),
                                        CountedIngredient.ofItems(4, Items.GRAVEL),
                                        CountedIngredient.ofItems(0, Items.WATER_BUCKET),
                                        CountedIngredient.ofItems(1, dye)),
                                5, 1, 15, new ItemStack(solid, 8)));

                        of(output, GenericMixingRecipe.ofCounted(nameSolid + "_from_powder", "concrete_water",
                                List.of(CountedIngredient.ofItems(1, powder), CountedIngredient.ofItems(0, Items.WATER_BUCKET)),
                                1, 1, 4, new ItemStack(solid, 1)));


                        of(output, GenericMixingRecipe.ofCounted(nameSolid + "_direct_new", "concrete_direct",
                                List.of(CountedIngredient.ofTag(4, itemWrap.getOrThrow(ItemTags.SMELTS_TO_GLASS)),
                                        CountedIngredient.ofItems(4, Items.GRAVEL),
                                        CountedIngredient.ofItems(1, dye)),
                                List.of(new FluidInputStack(FactoryFluids.WATER.defaultInstance(), FluidConstants.BOTTLE, FluidConstants.BOTTLE / 10)),
                                5, 1, 15, 0f, new ItemStack(solid, 8)));

                        of(output, GenericMixingRecipe.ofCounted(nameSolid + "_from_powder_new", "concrete_water",
                                List.of(CountedIngredient.ofItems(1, powder)),
                                List.of(new FluidInputStack(FactoryFluids.WATER.defaultInstance(), FluidConstants.BOTTLE, FluidConstants.BOTTLE / 10)),
                                1, 1, 4, 0f, new ItemStack(solid, 1)));
                    }
                }

                of(output,
                        new RecipeHolder<>(recipeKey("mixing/firework_star"), new FireworkStarMixingRecipe(4, 4, 17))
                );

                of(output,
                        new RecipeHolder<>(recipeKey("mixing/artificial_dye"), new ArtificialDyeMixingRecipe(3, 4, 15))
                );
                this.shapeless(RecipeCategory.COMBAT, FactoryItems.DYNAMITE)
                        .requires(Items.PAPER, 1).requires(FactoryItems.SAW_DUST, 1).requires(Items.GUNPOWDER, 2).requires(ItemTags.SAND)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GUNPOWDER))
                        .save(output);

                this.shapeless(RecipeCategory.COMBAT, FactoryItems.STICKY_DYNAMITE)
                        .requires(FactoryItems.DYNAMITE, 1).requires(Items.SLIME_BALL, 1)
                        .unlockedBy("get_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GUNPOWDER))
                        .save(output);

                of(output,
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
                                List.of(Ingredient.of(Items.DIRT), Ingredient.of(Items.GRAVEL)), 2, 4, 10, new ItemStack(Items.COARSE_DIRT, 2)),
                        GenericMixingRecipe.of("packed_mud",
                                List.of(Ingredient.of(Items.WHEAT), Ingredient.of(Items.MUD)), 2, 4, 10, new ItemStack(Items.PACKED_MUD)),
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
                                1, 6, 18f, 0f, Items.MUD.getDefaultInstance(), List.of()),
                        GenericMixingRecipe.ofCounted("snow_fluid", "",
                                List.of(),
                                List.of(FluidInputStack.from(FactoryFluids.WATER.of(800))),
                                1, 1, 3f, Float.NEGATIVE_INFINITY, -0.05f, ItemStack.EMPTY, List.of(FactoryFluids.SNOW.of(1000)))
                );

                for (var recipe : ((PotionBrewingAccessor) PotionBrewing.bootstrap(FeatureFlagSet.of(FeatureFlags.VANILLA))).getPotionMixes()) {
                    var from = FactoryFluids.getPotion(recipe.from());
                    var to = FactoryFluids.getPotion(recipe.to());
                    var b = new StringBuilder("mixing/brewing/");
                    b.append(getShortString(recipe.from()));
                    b.append("_with_");
                    for (var stack : recipe.ingredient().items().toList()) {
                        //noinspection deprecation
                        b.append(getShortString(stack));
                        b.append("_");
                    }
                    b.append("to_");
                    b.append(getShortString(recipe.to()));
                    output.accept(
                            recipeKey(b.toString()),
                            new BrewingMixingRecipe(getShortString(recipe.to()).replace("long_", "").replace("strong_", ""), recipe.ingredient(), from, to, FluidConstants.BOTTLE, FluidConstants.BOTTLE * 6,
                                    20, 15, 30, 0.7f, 2f), null
                    );
                }

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.STEEL_GEAR, 3)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .pattern(" i ")
                        .pattern("i i")
                        .pattern(" i ")
                        .define('i', FactoryItems.STEEL_INGOT)
                        .save(output);

                this.shapeless(RecipeCategory.REDSTONE, FactoryItems.LARGE_STEEL_GEAR)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .requires(FactoryItems.STEEL_INGOT, 1)
                        .requires(FactoryItems.STEEL_GEAR)
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.TACHOMETER, 1)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .pattern("#c#")
                        .pattern("-=-")
                        .pattern("#c#")
                        .define('#', FactoryItems.STEEL_PLATE)
                        .define('c', Items.COPPER_INGOT)
                        .define('-', FactoryItems.AXLE)
                        .define('=', ItemTags.PLANKS)
                        .save(output);

                this.shaped(RecipeCategory.REDSTONE, FactoryItems.STRESSOMETER, 1)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.STEEL_INGOT))
                        .pattern("#c#")
                        .pattern("-=-")
                        .pattern("#c#")
                        .define('#', FactoryItems.STEEL_PLATE)
                        .define('c', Items.GOLD_INGOT)
                        .define('-', FactoryItems.AXLE)
                        .define('=', ItemTags.PLANKS)
                        .save(output);

                output.accept(recipeKey("crafting/prt_key_setter"), new PRTKeySetterCraftingRecipe(CraftingBookCategory.MISC), null);


                fluidBase(output, Items.LAVA_BUCKET, Items.BUCKET, FactoryFluids.LAVA.ofBucket(), SoundEvents.BUCKET_FILL_LAVA, SoundEvents.BUCKET_EMPTY_LAVA);
                fluidBase(output, Items.WATER_BUCKET, Items.BUCKET, FactoryFluids.WATER.ofBucket(), SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
                fluidBase(output, Items.MILK_BUCKET, Items.BUCKET, FactoryFluids.MILK.ofBucket(), SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
                fluidBase(output, Items.POWDER_SNOW_BUCKET, Items.BUCKET, FactoryFluids.SNOW.ofBucket(), SoundEvents.BUCKET_FILL_POWDER_SNOW, SoundEvents.BUCKET_EMPTY_POWDER_SNOW);
                fluidBase(output, FactoryItems.HONEY_BUCKET, Items.BUCKET, FactoryFluids.HONEY.ofBucket(), FactorySoundEvents.ITEM_BUCKET_FILL_HONEY, FactorySoundEvents.ITEM_BUCKET_EMPTY_HONEY);
                fluidBase(output, Items.HONEY_BOTTLE, Items.GLASS_BOTTLE, FactoryFluids.HONEY.of(FluidConstants.BLOCK / 4), SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY);
                fluidBase(output, FactoryItems.SLIME_BUCKET, Items.BUCKET, FactoryFluids.SLIME.ofBucket(), FactorySoundEvents.ITEM_BUCKET_FILL_SLIME, FactorySoundEvents.ITEM_BUCKET_EMPTY_SLIME);
                fluidBase(output, FactoryItems.EXPERIENCE_BUCKET, Items.BUCKET, FactoryFluids.EXPERIENCE.ofBucket(), SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);

                fluidBasePotion(output, Items.POTION, Items.GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY);
                fluidBasePotion(output, Items.SPLASH_POTION, FactoryItems.THROWABLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY);
                fluidBasePotion(output, Items.LINGERING_POTION, FactoryItems.LINGERING_THROWABLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY);
                fluidBasePotion(output, FactoryItems.BRITTLE_POTION, FactoryItems.BRITTLE_GLASS_BOTTLE, FluidConstants.BOTTLE, SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY);

                output.accept(recipeKey("spout/experience_repair"), new RepairSpoutRecipe(), null);
                output.accept(recipeKey("spout/sticky_piston"), SimpleSpoutRecipe.toItem(Items.PISTON, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), Items.STICKY_PISTON, SoundEvents.SLIME_BLOCK_PLACE), null);
                output.accept(recipeKey("spout/sticky_conveyor"), SimpleSpoutRecipe.toItem(FactoryItems.CONVEYOR, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), FactoryItems.STICKY_CONVEYOR, SoundEvents.SLIME_BLOCK_PLACE), null);
                output.accept(recipeKey("spout/sticky_dynamite"), SimpleSpoutRecipe.toItem(FactoryItems.DYNAMITE, FactoryFluids.SLIME.of(FluidConstants.BLOCK / 10), FactoryItems.STICKY_DYNAMITE, SoundEvents.SLIME_BLOCK_PLACE), null);
                output.accept(recipeKey("spout/brittle_glass_bottle"), SimpleSpoutRecipe.toItem(Items.GLASS_BOTTLE, FactoryFluids.LAVA.of(FluidConstants.NUGGET), FactoryItems.BRITTLE_GLASS_BOTTLE, SoundEvents.GLASS_HIT), null);
                output.accept(recipeKey("spout/brittle_potion"), SimpleSpoutRecipe.toItemCopy(Items.POTION, FactoryFluids.LAVA.of(FluidConstants.NUGGET), FactoryItems.BRITTLE_POTION, SoundEvents.GLASS_HIT), null);
                output.accept(recipeKey("spout/slimeball"), SimpleSpoutRecipe.template(Items.BOWL,
                        FactoryFluids.SLIME.of(FluidConstants.INGOT), Items.SLIME_BALL, SoundEvents.SLIME_BLOCK_PLACE), null);
                output.accept(recipeKey("spout/snowball"), SimpleSpoutRecipe.template(Items.BOWL,
                        FactoryFluids.SNOW.of(FluidConstants.BLOCK / 4), Items.SNOWBALL, SoundEvents.SNOW_PLACE), null);


                spoutAndCasting("honeyed_apple", Items.APPLE, FactoryFluids.HONEY.of(FluidConstants.BLOCK / 4), FactoryItems.HONEYED_APPLE, SoundEvents.HONEY_BLOCK_PLACE, 0, 0);
                spoutAndCasting("golden_apple", Items.APPLE,  FactoryFluids.GOLD.of(FluidConstants.INGOT * 7), Items.GOLDEN_APPLE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);
                spoutAndCasting("golden_carrot", Items.CARROT, FactoryFluids.GOLD.of(FluidConstants.NUGGET * 7), Items.GOLDEN_CARROT, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);
                spoutAndCasting("golden_tiny_potato", FactoryItems.TINY_POTATO_SPRING, FactoryFluids.GOLD.of(FluidConstants.INGOT * 3), FactoryItems.GOLDEN_TINY_POTATO_SPRING, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 20, 50);


                output.accept(recipeKey("casting/cauldron/honey_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.HONEY.of(FluidConstants.BLOCK), Items.HONEY_BLOCK, SoundEvents.HONEY_BLOCK_PLACE, 0), null);
                output.accept(recipeKey("casting/cauldron/slime_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.SLIME.of(FluidConstants.BLOCK), Items.SLIME_BLOCK, SoundEvents.SLIME_BLOCK_PLACE, 0), null);
                output.accept(recipeKey("casting/cauldron/snow_block"),
                        SimpleCauldronCastingRecipe.toItem(FactoryFluids.SNOW.of(FluidConstants.BLOCK), Items.SNOW_BLOCK, SoundEvents.SNOW_PLACE, 0), null);

                destructiveItemCreatingFluidInteraction(output, "honey_lava", 1, List.of(FactoryFluids.HONEY.of(8000), FactoryFluids.LAVA.of(4000)),
                        OutputStack.of(FactoryItems.CRISPY_HONEY, 0.15f, 1), ParticleTypes.LARGE_SMOKE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value());

                for (var i : List.of(FactoryFluids.WATER, FactoryFluids.MILK)) {
                    for (var s : List.of(
                            new Tuple<>(FactoryFluids.LAVA.of(3000), OutputStack.of(Items.FLINT, 0.15f, 1)),
                            new Tuple<>(FactoryFluids.IRON.of(FluidConstants.NUGGET), OutputStack.of(Items.IRON_NUGGET, 0.9f, 1)),
                            new Tuple<>(FactoryFluids.GOLD.of(FluidConstants.NUGGET), OutputStack.of(Items.GOLD_NUGGET, 0.9f, 1)),
                            new Tuple<>(FactoryFluids.COPPER.of(FluidConstants.NUGGET), OutputStack.of(Items.COPPER_NUGGET, 0.9f, 1)),
                            new Tuple<>(FactoryFluids.STEEL.of(FluidConstants.NUGGET), OutputStack.of(FactoryItems.STEEL_NUGGET, 0.9f, 1))
                    )) {
                        destructiveItemCreatingFluidInteraction(output, FactoryRegistries.FLUID_TYPES.getKey(i).getPath() + "_" + FactoryRegistries.FLUID_TYPES.getKey(s.getA().type()).getPath(),
                                (int) (3000 / FluidConstants.NUGGET), List.of(i.of(s.getA().amount() * 2), s.getA()),
                                s.getB(), ParticleTypes.LARGE_SMOKE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value());
                    }
                }


                output.accept(recipeKey("fluid_interaction/snow_melting"), new SimpleFluidInteractionRecipe(
                        List.of(FluidInputStack.from(FactoryFluids.SNOW.of(1000))),
                        List.of(FactoryFluids.WATER.of(800)),
                        List.of(),
                        Optional.empty(), Optional.empty(), 0, BlockHeat.TORCH, Float.POSITIVE_INFINITY, 1
                ), null);

                smelteryOreSet(FactoryFluids.IRON, FactoryFluidConstants.IRON_INGOT_MELTING, ItemTags.IRON_ORES, Items.RAW_IRON, Items.RAW_IRON_BLOCK,
                        FactoryItems.CRUSHED_RAW_IRON, Items.IRON_INGOT, Items.IRON_NUGGET, Items.IRON_BLOCK, null);
                of(output,
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) * 3 / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 3 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.CHIPPED_ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) * 2 / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 3 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.DAMAGED_ANVIL, FactoryFluids.IRON.of(FluidConstants.INGOT * (9 * 3 + 4) / 4),FactoryFluidConstants.IRON_INGOT_MELTING * (9 * 3 + 4) * 2 / 4),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.BUCKET, FactoryFluids.IRON.of(FluidConstants.INGOT * 2) , FactoryFluidConstants.IRON_INGOT_MELTING * 2),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.CAULDRON, FactoryFluids.IRON.of(FluidConstants.INGOT * 6) , FactoryFluidConstants.IRON_INGOT_MELTING * 6),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_CHAIN, FactoryFluids.IRON.of(FluidConstants.INGOT / 2) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.HEAVY_WEIGHTED_PRESSURE_PLATE, FactoryFluids.IRON.of(FluidConstants.INGOT / 2) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_BARS, FactoryFluids.IRON.of(FluidConstants.INGOT * 6 / 16) , FactoryFluidConstants.IRON_INGOT_MELTING * 6 / 16),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_DOOR, FactoryFluids.IRON.of(FluidConstants.INGOT * 1) , FactoryFluidConstants.IRON_INGOT_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.IRON_TRAPDOOR, FactoryFluids.IRON.of(FluidConstants.INGOT * 2) , FactoryFluidConstants.IRON_INGOT_MELTING * 2),
                        SimpleSmelteryRecipe.of("minecraft_iron", Items.MINECART, FactoryFluids.IRON.of(FluidConstants.INGOT * 3) , FactoryFluidConstants.IRON_INGOT_MELTING * 3)
                );

                smelteryOreSet(FactoryFluids.GOLD, FactoryFluidConstants.GOLD_INGOT_MELTING, ItemTags.GOLD_ORES, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK,
                        FactoryItems.CRUSHED_RAW_GOLD, Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.GOLD_BLOCK, null);
                smelteryOreSet(FactoryFluids.COPPER, FactoryFluidConstants.COPPER_INGOT_MELTING, ItemTags.COPPER_ORES, Items.RAW_COPPER, Items.RAW_COPPER_BLOCK,
                        FactoryItems.CRUSHED_RAW_COPPER, Items.COPPER_INGOT, Items.COPPER_NUGGET, Items.COPPER_BLOCK, FactoryItems.COPPER_PLATE);


                of(output,
                        //SimpleSmelteryRecipe.of("minecraft_copper", ConventionalItemTags.STORAGE_BLOCKS_COPPER, FactoryFluids.COPPER.of(FluidConstants.BLOCK), FactoryFluidConstants.COPPER_INGOT_COOLING * 9),
                        SimpleSmelteryRecipe.of("minecraft_copper", FactoryItems.PIPE, FactoryFluids.COPPER.of(FluidConstants.INGOT * 2), FactoryFluidConstants.COPPER_INGOT_MELTING * 2)
                );

                moldRecipes(FactoryItems.PIPE_MOLD, FactoryFluids.COPPER.of(FluidConstants.INGOT * 2), FactoryItems.PIPE);

                smelteryOreSet(FactoryFluids.STEEL, FactoryFluidConstants.STEEL_INGOT_MELTING, null, FactoryItems.STEEL_ALLOY_MIXTURE, null,
                        null, FactoryItems.STEEL_INGOT, FactoryItems.STEEL_NUGGET, FactoryItems.STEEL_BLOCK, FactoryItems.STEEL_PLATE);

                // Smeltery Steel recycling
                of(output,
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
                this.spoutMolds(ConventionalItemTags.CHAINS, FactoryItems.CHAIN_MOLD);

                moldRecipes(FactoryItems.BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), Items.GLASS_BOTTLE);
                moldRecipes(FactoryItems.THROWABLE_BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryItems.THROWABLE_GLASS_BOTTLE);
                moldRecipes(FactoryItems.BRITTLE_BOTTLE_MOLD, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryItems.BRITTLE_GLASS_BOTTLE);
                moldRecipes(FactoryItems.CHAIN_MOLD, FactoryFluids.IRON.of(FluidConstants.NUGGET * 8), Items.IRON_CHAIN);
                moldRecipes(FactoryItems.CHAIN_MOLD, FactoryFluids.COPPER.of(FluidConstants.NUGGET * 8), Items.COPPER_CHAIN.unaffected());

                output.accept(recipeKey("casting/glass_pane"), SimpleCastingRecipe.fluid(FactoryFluids.GLASS.of(FluidConstants.BLOCK * 6 / 16), Items.GLASS_PANE, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null);
                output.accept(recipeKey("casting/iron_bars"), SimpleCastingRecipe.fluid(FactoryFluids.IRON.of(FluidConstants.BLOCK * 6 / 16), Items.IRON_BARS, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null);

                of(output,
                        SimpleSmelteryRecipe.of("minecraft_glass", ItemTags.SMELTS_TO_GLASS, FactoryFluids.GLASS.of(FluidConstants.BLOCK * 10 / 9), FactoryFluidConstants.GLASS_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_glass", ConventionalItemTags.GLASS_BLOCKS, FactoryFluids.GLASS.of(FluidConstants.BLOCK), FactoryFluidConstants.GLASS_MELTING),
                        SimpleSmelteryRecipe.of("minecraft_glass", ConventionalItemTags.GLASS_PANES, FactoryFluids.GLASS.of(FluidConstants.BLOCK * 6 / 16), FactoryFluidConstants.GLASS_MELTING * 6 / 16),
                        SimpleSmelteryRecipe.of("minecraft_glass", FactoryItemTags.GLASS_BOTTLE_MELTABLE, FactoryFluids.GLASS.of(FluidConstants.BLOCK / 2), FactoryFluidConstants.GLASS_MELTING / 2),
                        new RecipeHolder<>(recipeKey("casting/cauldron/glass"),
                                SimpleCauldronCastingRecipe.toItem(FactoryFluids.GLASS.of(FluidConstants.BLOCK), Items.GLASS, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60))
                );

            }

            private void moldRecipes(SpoutMolds molds, FluidStack<?> fluidStack, Item item) {
                output.accept(recipeKey("spout/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_with_molds"),
                        SimpleSpoutRecipe.templateDamaged(molds.tag(), fluidStack,
                                item, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60), null
                );

                output.accept(recipeKey("casting/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_with_molds"),
                        SimpleCastingRecipe.templateDamaged(molds.tag(), fluidStack,
                                item, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60), null
                );
            }

            private void spoutAndCasting(String name, Item base, FluidStack<Unit> fluid, Item result, SoundEvent sound, int coolingSpout, int coolingCast) {
                output.accept(recipeKey("spout/" + name),
                        SimpleSpoutRecipe.toItem(base, fluid, result, sound, coolingSpout), null
                );

                output.accept(recipeKey("casting/" + name),
                        SimpleCastingRecipe.toItem(base, fluid, result, sound, coolingCast), null
                );
            }

            private void spoutMolds(TagKey<Item> tag, SpoutMolds mold) {
                SimpleCookingRecipeBuilder.generic(Ingredient.of(mold.clay()), RecipeCategory.TOOLS, mold.hardened(),
                        0, 40, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLAY))
                        .group("polyfactory:hardened_molds")
                        .save(output);

                output.accept(recipeKey("casting/mold_" + mold.name().getPath()),
                        SimpleCastingRecipe.toItem(tag, FactoryFluids.STEEL.of(FluidConstants.INGOT * 4),
                                mold.mold(), FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null
                );

                this.shaped(RecipeCategory.TOOLS, mold.clay()).pattern(" x ").pattern("xox").pattern(" x ")
                        .define('x', Items.CLAY_BALL)
                        .define('o', tag)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLAY))
                        .group("polyfactory:clay_molds")
                        .save(CraftingWithLeftoverRecipe.asExporter(output, tag(ConventionalItemTags.INGOTS)));
            }

            private void spoutMolds(Item tag, SpoutMolds mold) {
                SimpleCookingRecipeBuilder.generic(Ingredient.of(mold.clay()), RecipeCategory.TOOLS, mold.hardened(),
                                0, 40, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLAY))
                        .group("polyfactory:hardened_molds")
                        .save(output);

                output.accept(recipeKey("casting/mold_" + mold.name().getPath()),
                        SimpleCastingRecipe.toItem(tag, FactoryFluids.STEEL.of(FluidConstants.INGOT * 4),
                                mold.mold(), FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 40), null
                );

                this.shaped(RecipeCategory.TOOLS, mold.clay()).pattern(" x ").pattern("xox").pattern(" x ")
                        .define('x', Items.CLAY_BALL)
                        .define('o', tag)
                        .unlockedBy("steel_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLAY))
                        .group("polyfactory:clay_molds")
                        .save(CraftingWithLeftoverRecipe.asExporter(output, tag(ConventionalItemTags.INGOTS)));
            }


            private void smelteryOreSet(FluidType<?> fluidType, int ingotTime, TagKey<Item> oreBlock, Item raw, Item rawBlock, Item crushed, Item ingot, Item nugget, Item block, Item plate) {
                var group = FactoryRegistries.FLUID_TYPES.getKey(fluidType).toDebugFileName();
                of(output,
                        raw != null ? SimpleSmelteryRecipe.of(group, raw, fluidType.of(FluidConstants.INGOT + FluidConstants.NUGGET * 2), ingotTime * 5 / 4) : null,
                        oreBlock != null ? SimpleSmelteryRecipe.of(group, oreBlock, fluidType.of(FluidConstants.INGOT * 2), ingotTime * 3 / 2) : null,
                        rawBlock != null ? SimpleSmelteryRecipe.of(group, rawBlock, fluidType.of((FluidConstants.INGOT + FluidConstants.NUGGET) * 9), (ingotTime * 3 / 2) * 9) : null,
                        crushed != null ? SimpleSmelteryRecipe.of(group, crushed, fluidType.of(FluidConstants.INGOT + FluidConstants.NUGGET * 2), (ingotTime * 5 / 4)) : null,
                        plate != null ? SimpleSmelteryRecipe.of(group, plate, fluidType.of(FluidConstants.INGOT), ingotTime) : null
                );

                if (ingot != null) {
                    of(output,
                            SimpleSmelteryRecipe.of(group, ingot, fluidType.of(FluidConstants.INGOT), ingotTime)
                    );

                    moldRecipes(FactoryItems.INGOT_MOLD, fluidType.of(FluidConstants.INGOT), ingot);
                }

                if (nugget != null) {
                    of(output,
                            SimpleSmelteryRecipe.of(group, nugget, fluidType.of(FluidConstants.NUGGET), ingotTime / 9)
                    );

                    moldRecipes(FactoryItems.NUGGET_MOLD, fluidType.of(FluidConstants.NUGGET), nugget);
                }

                if (block != null) {
                    of(output,
                            SimpleSmelteryRecipe.of(group, block, fluidType.of(FluidConstants.BLOCK), ingotTime * 9),
                            new RecipeHolder<>(recipeKey("casting/cauldron/" + getSimpleRecipeName(block)),
                                    SimpleCauldronCastingRecipe.toItem(fluidType.of(FluidConstants.BLOCK), block, FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED.value(), 60))
                    );
                }

            }

            private void destructiveItemCreatingFluidInteraction(RecipeOutput exporter, String name, int repeats, List<FluidStack<?>> fluids, OutputStack item, ParticleOptions particleEffect, SoundEvent soundEvent) {
                var base = recipeKey("fluid_interaction/" + name);
                var remove = recipeKey("fluid_interaction/" + name + "_leftover");

                exporter.accept(base, new SimpleFluidInteractionRecipe(
                        fluids.stream().map(FluidInputStack::from).toList(), List.of(), List.of(item), Optional.of(particleEffect), Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent)), 0.25f,
                        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, repeats
                ), null);

                exporter.accept(remove, new RemovingFluidInteractionRecipe(
                        fluids.stream().map(FluidInputStack::from).toList(), Optional.of(particleEffect), Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent)), 0.25f,
                        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
                ), null);
            }


            private void fluidBase(RecipeOutput exporter, Item withFluid, Item emptyContainer, FluidStack<?> fluid, SoundEvent fillSound, SoundEvent emptySound) {
                var base = BuiltInRegistries.ITEM.getKey(withFluid).getPath();

                exporter.accept(recipeKey("drain/from_" + base), SimpleDrainRecipe.fromItem(withFluid, fluid, emptyContainer, emptySound), null);
                exporter.accept(recipeKey("drain/to_" + base), SimpleDrainRecipe.toItem(emptyContainer, fluid, withFluid, fillSound), null);
                exporter.accept(recipeKey("spout/to_" + base), SimpleSpoutRecipe.toItem(emptyContainer, fluid, withFluid, fillSound), null);
            }

            @SuppressWarnings("SameParameterValue")
            private void fluidBasePotion(RecipeOutput exporter, Item withFluid, Item emptyContainer, long fluid, SoundEvent fillSound, SoundEvent emptySound) {
                var base = BuiltInRegistries.ITEM.getKey(withFluid).getPath();

                exporter.accept(recipeKey("drain/from_" + base), PotionAddDrainRecipe.of(withFluid, fluid, emptyContainer, emptySound), null);
                exporter.accept(recipeKey("drain/to_" + base), PotionRemoveDrainRecipe.of(emptyContainer, fluid, withFluid, fillSound), null);
                exporter.accept(recipeKey("spout/to_" + base), PotionSpoutRecipe.of(emptyContainer, fluid, withFluid, fillSound), null);
            }

            private String getShortString(Holder<?> entry) {
                //noinspection OptionalGetWithoutIsPresent
                var key = entry.unwrapKey().get().identifier();

                return key.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? key.getPath().replace("/", "_") : key.toDebugFileName();
            }


            public void of(RecipeOutput exporter, RecipeHolder<?>... recipes) {
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
