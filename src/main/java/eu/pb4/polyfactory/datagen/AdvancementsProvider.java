package eu.pb4.polyfactory.datagen;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.impl.ExtraItemPredicate;
import eu.pb4.polyfactory.advancement.FactoryItemPredicates;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.FluidShootsCriterion;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryColors;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.WoodUtil;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.triggers.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.EmptyTagLookupWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;
import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

class AdvancementsProvider extends FabricAdvancementProvider {


    protected AdvancementsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> exporter) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);

        var root = Advancement.Builder.advancement()
                .rootDisplay(
                        FactoryItems.WINDMILL_SAIL,
                        Component.translatable("advancements.polyfactory.root.title"),
                        Component.translatable("advancements.polyfactory.root.description"),
                        id("advancements/background"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false
                )
                .addCriterion("any_item", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(itemWrap, FactoryItemTags.ROOT_ADVANCEMENT)
                ))
                .save(exporter, id("main/root"));

        var guideBook = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        FactoryItems.GUIDE_BOOK,
                        Component.translatable("advancements.polyfactory.guidebook.title"),
                        Component.translatable("advancements.polyfactory.guidebook.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.GUIDEBOOK))
                .save(exporter, id("main/guidebook"));

        this.mainline(registryLookup, root, exporter);
        this.smeltery(registryLookup, root, exporter);
        this.taters(registryLookup, root, exporter);
    }

    private void smeltery(HolderLookup.Provider registryLookup, AdvancementHolder root, Consumer<AdvancementHolder> exporter) {
        var primitiveSmeltery = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        FactoryItems.PRIMITIVE_SMELTERY,
                        Component.translatable("advancements.polyfactory.primitive_smeltery.title"),
                        Component.translatable("advancements.polyfactory.primitive_smeltery.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.SMELTERY_MELTS))
                .save(exporter, id("main/smeltery/primitive_smeltery"));

        var smeltery = Advancement.Builder.advancement()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.SMELTERY,
                        Component.translatable("advancements.polyfactory.smeltery.title"),
                        Component.translatable("advancements.polyfactory.smeltery.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.INDUSTRIAL_SMELTERY_CREATED))
                .save(exporter, id("main/smeltery/smeltery"));

        var castingTable = Advancement.Builder.advancement()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.CASTING_TABLE,
                        Component.translatable("advancements.polyfactory.casting_table.title"),
                        Component.translatable("advancements.polyfactory.casting_table.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_METAL))
                .save(exporter, id("main/smeltery/casting_table"));


        var mold = Advancement.Builder.advancement()
                .parent(castingTable)
                .display(
                        FactoryItems.INGOT_MOLD.mold(),
                        Component.translatable("advancements.polyfactory.mold.title"),
                        Component.translatable("advancements.polyfactory.mold.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_MOLD))
                .save(exporter, id("main/smeltery/mold"));
    }

    private void taters(HolderLookup.Provider registryLookup, AdvancementHolder root, Consumer<AdvancementHolder> exporter) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);
        var blockWrap = registryLookup.lookupOrThrow(Registries.BLOCK);

        var tater16 = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        Items.POTATO,
                        Component.translatable("advancements.polyfactory.tater_16.title"),
                        Component.translatable("advancements.polyfactory.tater_16.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_16))
                .save(exporter, id("main/taters/16"));

        var tater128 = Advancement.Builder.advancement()
                .parent(tater16)
                .display(
                        Items.BAKED_POTATO,
                        Component.translatable("advancements.polyfactory.tater_128.title"),
                        Component.translatable("advancements.polyfactory.tater_128.description"),
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_128))
                .save(exporter, id("main/taters/128"));

        var tater1024 = Advancement.Builder.advancement()
                .parent(tater128)
                .display(
                        FactoryItems.TINY_POTATO_SPRING,
                        Component.translatable("advancements.polyfactory.tater_1024.title"),
                        Component.translatable("advancements.polyfactory.tater_1024.description"),
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_1024))
                .save(exporter, id("main/taters/1023"));
    }

    private void mainline(HolderLookup.Provider provider, AdvancementHolder root, Consumer<AdvancementHolder> exporter) {
        var itemWrap = provider.lookupOrThrow(Registries.ITEM);
        var blockWrap = provider.lookupOrThrow(Registries.BLOCK);
        var recipe = provider.lookupOrThrow(Registries.RECIPE);

        // Start

        var handCrank = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        FactoryItems.HAND_CRANK,
                        Component.translatable("advancements.polyfactory.hand_crank.title"),
                        Component.translatable("advancements.polyfactory.hand_crank.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.POWER_HAND_CRANK))
                .save(exporter, id("main/base/hand_crank"));

        var grinderUnf = Advancement.Builder.advancement()
                .parent(handCrank)
                .display(
                        FactoryItems.GRINDER,
                        Component.translatable("advancements.polyfactory.grinder.title"),
                        Component.translatable("advancements.polyfactory.grinder.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/coal_dust")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/planks_saw_dust")))))
                //.addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/logs_saw_dust")))))
                .requirements(AdvancementRequirements.Strategy.OR);

        for (var wood : WoodUtil.VANILLA) {
            var path = WoodUtil.asPath(wood);
            if (!WoodUtil.hasLog(wood)) {
                continue;
            }

            var log = BuiltInRegistries.ITEM.getValue(WoodUtil.getLogId(wood));
            var strippedLog = BuiltInRegistries.ITEM.getValue(WoodUtil.getStrippedLogId(wood));

            if (log != strippedLog && strippedLog != Items.AIR && log != Items.AIR) {
                var id = FactoryUtil.recipeKey("grinding/wood_stripping/" + path);
                grinderUnf.addCriterion("use/" + id.identifier().toLanguageKey(),
                        RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, id))));

            }
        }

        var grinder = grinderUnf.save(exporter, id("main/base/grinder_dust"));



        var dynamite = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.DYNAMITE,
                        Component.translatable("advancements.polyfactory.dynamite.title"),
                        Component.translatable("advancements.polyfactory.dynamite.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.DYNAMITE))
                .save(exporter, id("main/base/dynamite"));

        var crushedRawOre = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.CRUSHED_RAW_IRON,
                        Component.translatable("advancements.polyfactory.crushed_raw_ore.title"),
                        Component.translatable("advancements.polyfactory.crushed_raw_ore.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/crushed_raw_copper")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/crushed_raw_iron")))))
                .addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/crushed_raw_gold")))))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/crushed_raw_ore"));

        // Grinder -> Gravel

        var coloredLamp = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        ColoredItem.template(FactoryItems.LAMP, 1, FactoryColors.YTTR_TEAL),
                        Component.translatable("advancements.polyfactory.colored_lamp.title"),
                        Component.translatable("advancements.polyfactory.colored_lamp.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.LAMP))
                .addCriterion("use2", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.CAGED_LAMP))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/colored_lamp"));

        var gravel = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        Items.GRAVEL,
                        Component.translatable("advancements.polyfactory.gravel.title"),
                        Component.translatable("advancements.polyfactory.gravel.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/cobblestone_to_gravel")))))
                .addCriterion("use1", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/stone_to_cobblestone")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("grinding/gravel_to_sand")))))
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(exporter, id("main/base/gravel"));

        // Grinder -> Steel

        var steel = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.STEEL_INGOT,
                        Component.translatable("advancements.polyfactory.steel_ingot.title"),
                        Component.translatable("advancements.polyfactory.steel_ingot.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("steel_ingot")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("steel_ingot_blasting")))))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/steel_ingot"));

        var windmill = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.WINDMILL_SAIL,
                        Component.translatable("advancements.polyfactory.windmill.title"),
                        Component.translatable("advancements.polyfactory.windmill.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONSTRUCT_WORKING_WINDMILL))
                .save(exporter, id("main/base/windmill"));

        var wrench = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.WRENCH,
                        Component.translatable("advancements.polyfactory.wrench.title"),
                        Component.translatable("advancements.polyfactory.wrench.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.WRENCH))
                .save(exporter, id("main/base/wrench"));

        var multimeter = Advancement.Builder.advancement()
                .parent(wrench)
                .display(
                        FactoryItems.MULTIMETER,
                        Component.translatable("advancements.polyfactory.multimeter.title"),
                        Component.translatable("advancements.polyfactory.multimeter.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.MULTIMETER_MEASURE))
                .save(exporter, id("main/base/multimeter"));

        var mixer = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.MIXER,
                        Component.translatable("advancements.polyfactory.mixer.title"),
                        Component.translatable("advancements.polyfactory.mixer.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MIXER_CRAFTS))
                .save(exporter, id("main/base/mixer"));

        // Steel -> Mixer

        var cake = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        Items.CAKE,
                        Component.translatable("advancements.polyfactory.cake.title"),
                        Component.translatable("advancements.polyfactory.cake.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("mixing/cake")))))
                .save(exporter, id("main/base/cake"));

        var dye = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        ColoredItem.template(FactoryItems.ARTIFICIAL_DYE, 1, 0x42f5a4),
                        Component.translatable("advancements.polyfactory.artificial_dye.title"),
                        Component.translatable("advancements.polyfactory.artificial_dye.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("mixing/artificial_dye")))))
                .save(exporter, id("main/base/mixer/artificial_dye"));

        var firework = Advancement.Builder.advancement()
                .parent(dye)
                .display(
                        Items.FIREWORK_ROCKET,
                        Component.translatable("advancements.polyfactory.firework.title"),
                        Component.translatable("advancements.polyfactory.firework.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, ResourceKey.create(Registries.RECIPE, Identifier.parse("firework_rocket")))), List.of(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.item(), FactoryItemPredicates.CUSTOM_FIREWORK_COLOR))))
                .save(exporter, id("main/base/mixer/firework"));


        this.cables(exporter, provider, mixer);

        // Steel

        var press = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.PRESS,
                        Component.translatable("advancements.polyfactory.press.title"),
                        Component.translatable("advancements.polyfactory.press.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("press/steel_plate")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("press/wooden_plate")))))
                .addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("press/copper_plate")))))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/press"));

        var itemOutputBuffer = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.ITEM_OUTPUT_BUFFER,
                        Component.translatable("advancements.polyfactory.item_output_buffer.title"),
                        Component.translatable("advancements.polyfactory.item_output_buffer.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_OUTPUT_BUFFER))
                .save(exporter, id("main/base/item_output_buffer"));

        var fermenter = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.FERMENTER,
                        Component.translatable("advancements.polyfactory.fermenter.title"),
                        Component.translatable("advancements.polyfactory.fermenter.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.FERMENTER_FERMENTS))
                .save(exporter, id("main/base/fermenters"));

        var cheeseWheel = Advancement.Builder.advancement()
                .parent(fermenter)
                .display(
                        FactoryItems.CHEESE_WEDGE,
                        Component.translatable("advancements.polyfactory.cheese.title"),
                        Component.translatable("advancements.polyfactory.cheese.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("item", ConsumeItemTrigger.TriggerInstance.usedItem(itemWrap, FactoryItems.CHEESE_WEDGE))
                .addCriterion("block", CriteriaTriggers.DEFAULT_BLOCK_USE.createCriterion(new DefaultBlockInteractionTrigger.TriggerInstance(Optional.empty(), Optional.of(
                        Holder.direct(new LocationCheck(Optional.of(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blockWrap, FactoryBlocks.CHEESE_WHEEL)).build()), BlockPos.ZERO))
                ))))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/cheese"));

        var biodiesel = Advancement.Builder.advancement()
                .parent(fermenter)
                .display(
                        FactoryItems.BIODIESEL_BUCKET,
                        Component.translatable("advancements.polyfactory.biodiesel.title"),
                        Component.translatable("advancements.polyfactory.biodiesel.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("fuel", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("mixing/biodiesel")))))
                .addCriterion("engine", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.DIESEL_ENGINE))
                .save(exporter, id("main/base/biodiesel"));

        var portableDrill = Advancement.Builder.advancement()
                .parent(biodiesel)
                .display(
                        new ItemStackTemplate(FactoryItems.PORTABLE_DRILL, DataComponentPatch.builder().set(FactoryDataComponents.DRILL_ATTACHMENT, new ItemStackTemplate(FactoryItems.DIAMOND_DRILL_HEAD)).build()),
                        Component.translatable("advancements.polyfactory.portable_drill.title"),
                        Component.translatable("advancements.polyfactory.portable_drill.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("item", TriggerCriterion.of(FactoryTriggers.PORTABLE_DRILL_MINES))
                .save(exporter, id("main/base/portable_drill"));

        var gear = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.STEEL_GEAR,
                        Component.translatable("advancements.polyfactory.steel_gear.title"),
                        Component.translatable("advancements.polyfactory.steel_gear.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONNECT_DIFFERENT_GEARS))
                .save(exporter, id("main/base/steel_gear"));

        var chainDrive = Advancement.Builder.advancement()
                .parent(gear)
                .display(
                        FactoryItems.CHAIN_DRIVE,
                        Component.translatable("advancements.polyfactory.chain_drive.title"),
                        Component.translatable("advancements.polyfactory.chain_drive.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CHAIN_DRIVES_CONNECTED))
                .save(exporter, id("main/base/chain_drive"));

        var chainLift = Advancement.Builder.advancement()
                .parent(chainDrive)
                .display(
                        FactoryItems.CHAIN_LIFT,
                        Component.translatable("advancements.polyfactory.chain_lift.title"),
                        Component.translatable("advancements.polyfactory.chain_lift.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CHAIN_LIFT))
                .save(exporter, id("main/base/chain_lift"));

        // Steel -> Press

        var sprayCan = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        new ItemStackTemplate(FactoryItems.SPRAY_CAN, DataComponentPatch.builder()
                                .set(FactoryDataComponents.COLOR, DyeColorExtra.getColor(DyeColor.BLUE))
                                .set(FactoryDataComponents.USES_LEFT, DyeSprayItem.MAX_USES)
                                .build()),
                        Component.translatable("advancements.polyfactory.spray_can.title"),
                        Component.translatable("advancements.polyfactory.spray_can.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location(), ItemPredicate.Builder.item().of(itemWrap, FactoryItems.SPRAY_CAN)))
                .save(exporter, id("main/base/spray_can"));

        var crafter = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.CRAFTER,
                        Component.translatable("advancements.polyfactory.crafter.title"),
                        Component.translatable("advancements.polyfactory.crafter.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS))
                .save(exporter, id("main/base/crafter"));

        var crafter2 = Advancement.Builder.advancement()
                .parent(crafter)
                .display(
                        new ItemStackTemplate(FactoryItems.CRAFTER, DataComponentPatch.builder().set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).build()),
                        Component.translatable("advancements.polyfactory.crafter2.title"),
                        Component.translatable("advancements.polyfactory.crafter2.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS_CRAFTER))
                .save(exporter, id("main/base/crafter2"));

        var steamEngine = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.STEAM_ENGINE,
                        Component.translatable("advancements.polyfactory.steam_engine.title"),
                        Component.translatable("advancements.polyfactory.steam_engine.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.FUEL_STEAM_ENGINE))
                .save(exporter, id("main/base/steam_engine"));

        var workbench = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.WORKBENCH,
                        Component.translatable("advancements.polyfactory.workbench.title"),
                        Component.translatable("advancements.polyfactory.workbench.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("craft", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("workbench")))))
                .save(exporter, id("main/base/workbench"));

        var blueprintWorkbench = Advancement.Builder.advancement()
                .parent(workbench)
                .display(
                        FactoryItems.BLUEPRINT_WORKBENCH,
                        Component.translatable("advancements.polyfactory.blueprint_workbench.title"),
                        Component.translatable("advancements.polyfactory.blueprint_workbench.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("craft", TriggerCriterion.of(FactoryTriggers.CRAFT_WITH_BLUEPRINT_WORKBENCH))
                .save(exporter, id("main/base/blueprint_workbench"));

        var container = Advancement.Builder.advancement()
                .parent(workbench)
                .display(
                        FactoryItems.CONTAINER,
                        Component.translatable("advancements.polyfactory.container.title"),
                        Component.translatable("advancements.polyfactory.container.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONTAINER_ADD_ITEM))
                .save(exporter, id("main/base/container"));

        var deepStorageContainer = Advancement.Builder.advancement()
                .parent(container)
                .display(
                        FactoryItems.DEEP_STORAGE_CONTAINER,
                        Component.translatable("advancements.polyfactory.deep_storage_container.title"),
                        Component.translatable("advancements.polyfactory.deep_storage_container.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DEEP_STORAGE_CONTAINER))
                .save(exporter, id("main/base/deep_storage_container"));


        var itemFilter = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.ITEM_FILTER,
                        Component.translatable("advancements.polyfactory.item_filter.title"),
                        Component.translatable("advancements.polyfactory.item_filter.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_FILTER_USE))
                .save(exporter, id("main/base/item_filter"));

        var trommel = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.TROMMEL,
                        Component.translatable("advancements.polyfactory.trommel.title"),
                        Component.translatable("advancements.polyfactory.trommel.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TROMMEL_SUCCESS))
                .save(exporter, id("main/base/trommel"));

        var itemPacker = Advancement.Builder.advancement()
                .parent(container)
                .display(
                        FactoryItems.ITEM_PACKER,
                        Component.translatable("advancements.polyfactory.item_packer.title"),
                        Component.translatable("advancements.polyfactory.item_packer.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_PACKER_ACCESSES))
                .save(exporter, id("main/base/item_packer"));

        var fan = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.FAN,
                        Component.translatable("advancements.polyfactory.fan.title"),
                        Component.translatable("advancements.polyfactory.fan.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN))
                .save(exporter, id("main/base/fan"));

        var fanSky = Advancement.Builder.advancement()
                .parent(fan)
                .display(
                        Items.FEATHER,
                        Component.translatable("advancements.polyfactory.fan_sky.title"),
                        Component.translatable("advancements.polyfactory.fan_sky.description"),
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN_A_LOT))
                .save(exporter, id("main/base/fan_sky"));

        var ejector = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.EJECTOR,
                        Component.translatable("advancements.polyfactory.ejector.title"),
                        Component.translatable("advancements.polyfactory.ejector.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.LAUNCHED_BY_EJECTOR))
                .save(exporter, id("main/base/ejector"));

        var miner = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.MINER,
                        Component.translatable("advancements.polyfactory.miner.title"),
                        Component.translatable("advancements.polyfactory.miner.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MINER_MINES))
                .save(exporter, id("main/base/miner"));

        var placer = Advancement.Builder.advancement()
                .parent(miner)
                .display(
                        FactoryItems.PLACER,
                        Component.translatable("advancements.polyfactory.placer.title"),
                        Component.translatable("advancements.polyfactory.placer.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.PLACER_PLACES))
                .save(exporter, id("main/base/placer"));

        var planter = Advancement.Builder.advancement()
                .parent(miner)
                .display(
                        FactoryItems.PLANTER,
                        Component.translatable("advancements.polyfactory.planter.title"),
                        Component.translatable("advancements.polyfactory.planter.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.PLANTER_PLANTS))
                .save(exporter, id("main/base/planter"));


        // Plates -> Fluids
        this.fluids(exporter, provider, press);
    }

    private Holder<Recipe<?>> fakeRecipe(HolderLookup.RegistryLookup<Recipe<?>> recipe, ResourceKey<Recipe<?>> recipeResourceKey) {
        return Holder.Reference.createStandAlone(recipe instanceof EmptyTagLookupWrapper<Recipe<?>> wrapper ? wrapper.parent() : recipe, recipeResourceKey);
    }

    private void fluids(Consumer<AdvancementHolder> exporter, HolderLookup.Provider registryLookup, AdvancementHolder press) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);
        var blockWrap = registryLookup.lookupOrThrow(Registries.BLOCK);
        var recipe = registryLookup.lookupOrThrow(Registries.RECIPE);

        var pipe = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.PIPE,
                        Component.translatable("advancements.polyfactory.pipe.title"),
                        Component.translatable("advancements.polyfactory.pipe.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blockWrap, FactoryBlocks.PIPE))
                .save(exporter, id("main/base/pipe"));
        var drain = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.DRAIN,
                        Component.translatable("advancements.polyfactory.drain.title"),
                        Component.translatable("advancements.polyfactory.drain.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DRAIN_USE))
                .save(exporter, id("main/base/drain"));
        var mechanicalSpout = Advancement.Builder.advancement()
                .parent(drain)
                .display(
                        FactoryItems.MECHANICAL_SPOUT,
                        Component.translatable("advancements.polyfactory.mechanical_spout.title"),
                        Component.translatable("advancements.polyfactory.mechanical_spout.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.SPOUT_CRAFT))
                .save(exporter, id("main/base/mechanical_spout"));

        var crispHoney = Advancement.Builder.advancement()
                .parent(drain)
                .display(
                        FactoryItems.CRISPY_HONEY,
                        Component.translatable("advancements.polyfactory.crispy_honey.title"),
                        Component.translatable("advancements.polyfactory.crispy_honey.description"),
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("fluid_interaction/honey_lava"))),
                        List.of(ItemPredicate.Builder.item().of(itemWrap, FactoryItems.CRISPY_HONEY))))
                .save(exporter, id("main/base/crispy_honey"));

        var honeyedApple = Advancement.Builder.advancement()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.HONEYED_APPLE,
                        Component.translatable("advancements.polyfactory.honeyed_apple.title"),
                        Component.translatable("advancements.polyfactory.honeyed_apple.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("spout/honeyed_apple")))))
                .save(exporter, id("main/base/honeyed_apple"));

        var brittleGlassBottle = Advancement.Builder.advancement()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.BRITTLE_POTION,
                        Component.translatable("advancements.polyfactory.brittle_glass_bottle.title"),
                        Component.translatable("advancements.polyfactory.brittle_glass_bottle.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("spout/brittle_glass_bottle")))))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("spout/brittle_potion")))))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/brittle_glass_bottle"));


        var fluidTank = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.FLUID_TANK,
                        Component.translatable("advancements.polyfactory.fluid_tank.title"),
                        Component.translatable("advancements.polyfactory.fluid_tank.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.FLUID_TANK_CONNECT))
                .save(exporter, id("main/base/fluid_tank"));
        var portableFluidTank = Advancement.Builder.advancement()
                .parent(fluidTank)
                .display(
                        FactoryItems.PORTABLE_FLUID_TANK,
                        Component.translatable("advancements.polyfactory.portable_fluid_tank.title"),
                        Component.translatable("advancements.polyfactory.portable_fluid_tank.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.item().of(itemWrap, FactoryItems.PORTABLE_FLUID_TANK), FactoryItemPredicates.HAS_FLUIDS)
                ))
                .save(exporter, id("main/base/portable_fluid_tank"));

        var nozzle = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.NOZZLE,
                        Component.translatable("advancements.polyfactory.nozzle.title"),
                        Component.translatable("advancements.polyfactory.nozzle.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", FluidShootsCriterion.ofNozzle())
                .save(exporter, id("main/base/nozzle"));

        var stickyEffect = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        FactoryItems.HONEY_BUCKET,
                        Component.translatable("advancements.polyfactory.sticky_effect.title"),
                        Component.translatable("advancements.polyfactory.sticky_effect.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.STICKY_WALL_SLIDING))
                .save(exporter, id("main/base/sticky_effect"));

        var pressureFluidGun = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        FactoryItems.PRESSURE_FLUID_GUN,
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun.title"),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(HolderSet.direct(fakeRecipe(recipe, recipeKey("pressure_fluid_gun")))))
                .save(exporter, id("main/base/pressure_fluid_gun"));

        var pressureFluidGunHealing = Advancement.Builder.advancement()
                .parent(pressureFluidGun)
                .display(
                        new ItemStackTemplate(FactoryItems.PRESSURE_FLUID_GUN, DataComponentPatch.builder().set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).build()),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun_healing.title"),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun_healing.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", FluidShootsCriterion.ofFluidLauncher(
                        FactoryFluids.getPotion(Potions.HEALING),
                        FactoryFluids.getPotion(Potions.STRONG_HEALING),
                        FactoryFluids.getPotion(Potions.REGENERATION),
                        FactoryFluids.getPotion(Potions.STRONG_REGENERATION),
                        FactoryFluids.getPotion(Potions.LONG_REGENERATION)
                ))
                .save(exporter, id("main/base/pressure_fluid_gun_healing"));


        var nozzleLava = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        new ItemStackTemplate(FactoryItems.NOZZLE, DataComponentPatch.builder().set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).build()),
                        Component.translatable("advancements.polyfactory.nozzle_lava.title"),
                        Component.translatable("advancements.polyfactory.nozzle_lava.description"),
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", FluidShootsCriterion.ofNozzle(FactoryFluids.LAVA.defaultInstance()))
                .addCriterion("use2", FluidShootsCriterion.ofFluidLauncher(FactoryFluids.LAVA.defaultInstance()))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, id("main/base/nozzle_lava"));
    }

    private void cables(Consumer<AdvancementHolder> exporter, HolderLookup.Provider registryLookup, AdvancementHolder mixer) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);
        var blockWrap = registryLookup.lookupOrThrow(Registries.BLOCK);
        var recipe = registryLookup.lookupOrThrow(Registries.RECIPE);

        var cable = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        ColoredItem.template(FactoryItems.CABLE, 1, DyeColor.RED),
                        Component.translatable("advancements.polyfactory.cable.title"),
                        Component.translatable("advancements.polyfactory.cable.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CABLE_CONNECT))
                .save(exporter, id("main/base/cable"));

        var tachometer = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.TACHOMETER,
                        Component.translatable("advancements.polyfactory.tachometer.title"),
                        Component.translatable("advancements.polyfactory.tachometer.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
                        AnyOfCondition.anyOf(
                                LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blockWrap, FactoryBlocks.TACHOMETER))),
                                LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blockWrap, FactoryBlocks.STRESSOMETER)))
                        )))
                .save(exporter, id("main/base/tachometer"));

        var recordPlayer = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.RECORD_PLAYER,
                        Component.translatable("advancements.polyfactory.record_player.title"),
                        Component.translatable("advancements.polyfactory.record_player.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.CONNECT_RECORD_PLAYER_AND_SPEAKERS))
                .save(exporter, id("main/base/record_player"));

        var redstone = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.REDSTONE_INPUT,
                        Component.translatable("advancements.polyfactory.redstone.title"),
                        Component.translatable("advancements.polyfactory.redstone.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.REDSTONE_IN))
                .addCriterion("b", TriggerCriterion.of(FactoryTriggers.REDSTONE_OUT))
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(exporter, id("main/base/redstone"));


        var wirelessRedstone = Advancement.Builder.advancement()
                .parent(redstone)
                .display(
                        FactoryItems.WIRELESS_REDSTONE_TRANSMITTER,
                        Component.translatable("advancements.polyfactory.wireless_redstone.title"),
                        Component.translatable("advancements.polyfactory.wireless_redstone.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.WIRELESS_REDSTONE))
                .save(exporter, id("main/base/wireless_redstone"));

        // Mixer -> Cable

        var itemReader = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.ITEM_READER,
                        Component.translatable("advancements.polyfactory.item_reader.title"),
                        Component.translatable("advancements.polyfactory.item_reader.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_READER))
                .save(exporter, id("main/base/item_reader"));

        var aritheticOperator = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.ARITHMETIC_OPERATOR,
                        Component.translatable("advancements.polyfactory.arithmetic_operator.title"),
                        Component.translatable("advancements.polyfactory.arithmetic_operator.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ARITHMETIC_OPERATOR))
                .save(exporter, id("main/base/arithmetic_operator"));

        var dataComparator = Advancement.Builder.advancement()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_COMPARATOR,
                        Component.translatable("advancements.polyfactory.data_comparator.title"),
                        Component.translatable("advancements.polyfactory.data_comparator.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DATA_COMPARATOR))
                .save(exporter, id("main/base/data_comparator"));

        var dataMemory = Advancement.Builder.advancement()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_MEMORY,
                        Component.translatable("advancements.polyfactory.data_memory.title"),
                        Component.translatable("advancements.polyfactory.data_memory.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DATA_MEMORY))
                .save(exporter, id("main/base/data_memory"));

        var nixieTubes = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.NIXIE_TUBE,
                        Component.translatable("advancements.polyfactory.nixie_tube.title"),
                        Component.translatable("advancements.polyfactory.nixie_tube.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.NIXIE_TUBE_CONNECTED_3_OR_MORE))
                .save(exporter, id("main/base/nixie_tube"));

        var hologramProjector = Advancement.Builder.advancement()
                .parent(nixieTubes)
                .display(
                        FactoryItems.HOLOGRAM_PROJECTOR,
                        Component.translatable("advancements.polyfactory.hologram_projector.title"),
                        Component.translatable("advancements.polyfactory.hologram_projector.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.HOLOGRAM_PROJECTOR_ACTIVATES))
                .save(exporter, id("main/base/hologram_projector"));
    }
}
