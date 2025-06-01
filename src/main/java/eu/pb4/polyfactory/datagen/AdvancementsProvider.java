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
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.criterion.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;
import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

class AdvancementsProvider extends FabricAdvancementProvider {


    protected AdvancementsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> exporter) {
        var itemWrap = registryLookup.getOrThrow(RegistryKeys.ITEM);
        var root = Advancement.Builder.create()
                .display(
                        FactoryItems.WINDMILL_SAIL,
                        Text.translatable("advancements.polyfactory.root.title"),
                        Text.translatable("advancements.polyfactory.root.description"),
                        id("advancements/background"),
                        AdvancementFrame.TASK,
                        false,
                        false,
                        false
                )
                .criterion("any_item", InventoryChangedCriterion.Conditions.items(
                        ItemPredicate.Builder.create().tag(itemWrap, FactoryItemTags.ROOT_ADVANCEMENT)
                ))
                .build(exporter, "polyfactory:main/root");

        this.mainline(registryLookup, root, exporter);
        this.smeltery(registryLookup, root, exporter);
        this.taters(registryLookup, root, exporter);
    }

    private void smeltery(RegistryWrapper.WrapperLookup registryLookup, AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var primitiveSmeltery = Advancement.Builder.create()
                .parent(root)
                .display(
                        FactoryItems.PRIMITIVE_SMELTERY,
                        Text.translatable("advancements.polyfactory.primitive_smeltery.title"),
                        Text.translatable("advancements.polyfactory.primitive_smeltery.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.SMELTERY_MELTS))
                .build(exporter, "polyfactory:main/smeltery/primitive_smeltery");

        var smeltery = Advancement.Builder.create()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.SMELTERY,
                        Text.translatable("advancements.polyfactory.smeltery.title"),
                        Text.translatable("advancements.polyfactory.smeltery.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.INDUSTRIAL_SMELTERY_CREATED))
                .build(exporter, "polyfactory:main/smeltery/smeltery");

        var castingTable = Advancement.Builder.create()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.CASTING_TABLE,
                        Text.translatable("advancements.polyfactory.casting_table.title"),
                        Text.translatable("advancements.polyfactory.casting_table.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_METAL))
                .build(exporter, "polyfactory:main/smeltery/casting_table");


        var mold = Advancement.Builder.create()
                .parent(castingTable)
                .display(
                        FactoryItems.INGOT_MOLD.mold(),
                        Text.translatable("advancements.polyfactory.mold.title"),
                        Text.translatable("advancements.polyfactory.mold.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_MOLD))
                .build(exporter, "polyfactory:main/smeltery/mold");
    }

    private void taters(RegistryWrapper.WrapperLookup registryLookup, AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var itemWrap = registryLookup.getOrThrow(RegistryKeys.ITEM);

        var tater16 = Advancement.Builder.create()
                .parent(root)
                .display(
                        Items.POTATO,
                        Text.translatable("advancements.polyfactory.tater_16.title"),
                        Text.translatable("advancements.polyfactory.tater_16.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        true
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.TATER_16))
                .build(exporter, "polyfactory:main/taters/16");

        var tater128 = Advancement.Builder.create()
                .parent(tater16)
                .display(
                        Items.BAKED_POTATO,
                        Text.translatable("advancements.polyfactory.tater_128.title"),
                        Text.translatable("advancements.polyfactory.tater_128.description"),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        true,
                        true
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.TATER_128))
                .build(exporter, "polyfactory:main/taters/128");

        var tater1024 = Advancement.Builder.create()
                .parent(tater128)
                .display(
                        FactoryItems.TINY_POTATO_SPRING,
                        Text.translatable("advancements.polyfactory.tater_1024.title"),
                        Text.translatable("advancements.polyfactory.tater_1024.description"),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        true,
                        true
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.TATER_1024))
                .build(exporter, "polyfactory:main/taters/1023");
    }

    private void mainline(RegistryWrapper.WrapperLookup registryLookup, AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var itemWrap = registryLookup.getOrThrow(RegistryKeys.ITEM);

        // Start

        var handCrank = Advancement.Builder.create()
                .parent(root)
                .display(
                        FactoryItems.HAND_CRANK,
                        Text.translatable("advancements.polyfactory.hand_crank.title"),
                        Text.translatable("advancements.polyfactory.hand_crank.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.POWER_HAND_CRANK))
                .build(exporter, "polyfactory:main/base/hand_crank");

        var grinder = Advancement.Builder.create()
                .parent(handCrank)
                .display(
                        FactoryItems.GRINDER,
                        Text.translatable("advancements.polyfactory.grinder.title"),
                        Text.translatable("advancements.polyfactory.grinder.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/coal_dust")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/planks_saw_dust")))
                .criterion("use3", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/logs_saw_dust")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/grinder_dust");

        var dynamite = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        FactoryItems.DYNAMITE,
                        Text.translatable("advancements.polyfactory.dynamite.title"),
                        Text.translatable("advancements.polyfactory.dynamite.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", InventoryChangedCriterion.Conditions.items(FactoryItems.DYNAMITE))
                .build(exporter, "polyfactory:main/base/dynamite");

        var crushedRawOre = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        FactoryItems.CRUSHED_RAW_IRON,
                        Text.translatable("advancements.polyfactory.crushed_raw_ore.title"),
                        Text.translatable("advancements.polyfactory.crushed_raw_ore.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/crushed_raw_copper")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/crushed_raw_iron")))
                .criterion("use3", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/crushed_raw_gold")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/crushed_raw_ore");

        // Grinder -> Gravel

        var coloredLamp = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        ColoredItem.stack(FactoryItems.LAMP, 1, FactoryColors.YTTR_TEAL),
                        Text.translatable("advancements.polyfactory.colored_lamp.title"),
                        Text.translatable("advancements.polyfactory.colored_lamp.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", InventoryChangedCriterion.Conditions.items(FactoryItems.LAMP))
                .criterion("use2", InventoryChangedCriterion.Conditions.items(FactoryItems.CAGED_LAMP))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/colored_lamp");

        var gravel = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        Items.GRAVEL,
                        Text.translatable("advancements.polyfactory.gravel.title"),
                        Text.translatable("advancements.polyfactory.gravel.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/cobblestone_to_gravel")))
                .criterion("use1", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/stone_to_cobblestone")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("grinding/gravel_to_sand")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.AND)
                .build(exporter, "polyfactory:main/base/gravel");

        // Grinder -> Steel

        var steel = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        FactoryItems.STEEL_INGOT,
                        Text.translatable("advancements.polyfactory.steel_ingot.title"),
                        Text.translatable("advancements.polyfactory.steel_ingot.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("steel_ingot")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("steel_ingot_blasting")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/steel_ingot");

        var windmill = Advancement.Builder.create()
                .parent(steel)
                .display(
                        FactoryItems.WINDMILL_SAIL,
                        Text.translatable("advancements.polyfactory.windmill.title"),
                        Text.translatable("advancements.polyfactory.windmill.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CONSTRUCT_WORKING_WINDMILL))
                .build(exporter, "polyfactory:main/base/windmill");

        var wrench = Advancement.Builder.create()
                .parent(steel)
                .display(
                        FactoryItems.WRENCH,
                        Text.translatable("advancements.polyfactory.wrench.title"),
                        Text.translatable("advancements.polyfactory.wrench.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.WRENCH))
                .build(exporter, "polyfactory:main/base/wrench");


        var mixer = Advancement.Builder.create()
                .parent(steel)
                .display(
                        FactoryItems.MIXER,
                        Text.translatable("advancements.polyfactory.mixer.title"),
                        Text.translatable("advancements.polyfactory.mixer.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.MIXER_CRAFTS))
                .build(exporter, "polyfactory:main/base/mixer");

        // Steel -> Mixer

        var cake = Advancement.Builder.create()
                .parent(mixer)
                .display(
                        Items.CAKE,
                        Text.translatable("advancements.polyfactory.cake.title"),
                        Text.translatable("advancements.polyfactory.cake.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        true
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("mixing/cake")))
                .build(exporter, "polyfactory:main/base/cake");

        var dye = Advancement.Builder.create()
                .parent(mixer)
                .display(
                        ColoredItem.stack(FactoryItems.ARTIFICIAL_DYE, 1, 0x42f5a4),
                        Text.translatable("advancements.polyfactory.artificial_dye.title"),
                        Text.translatable("advancements.polyfactory.artificial_dye.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("mixing/artificial_dye")))
                .build(exporter, "polyfactory:main/base/mixer/artificial_dye");

        var firework = Advancement.Builder.create()
                .parent(dye)
                .display(
                        Items.FIREWORK_ROCKET,
                        Text.translatable("advancements.polyfactory.firework.title"),
                        Text.translatable("advancements.polyfactory.firework.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(RegistryKey.of(RegistryKeys.RECIPE, Identifier.of("firework_rocket")), List.of(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.create(), FactoryItemPredicates.CUSTOM_FIREWORK_COLOR))))
                .build(exporter, "polyfactory:main/base/mixer/firework");


        this.cables(exporter, itemWrap, mixer);

        // Steel

        var press = Advancement.Builder.create()
                .parent(steel)
                .display(
                        FactoryItems.PRESS,
                        Text.translatable("advancements.polyfactory.press.title"),
                        Text.translatable("advancements.polyfactory.press.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("press/steel_plate")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("press/wooden_plate")))
                .criterion("use3", RecipeCraftedCriterion.Conditions.create(recipeKey("press/copper_plate")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/press");

        var gear = Advancement.Builder.create()
                .parent(steel)
                .display(
                        FactoryItems.STEEL_GEAR,
                        Text.translatable("advancements.polyfactory.steel_gear.title"),
                        Text.translatable("advancements.polyfactory.steel_gear.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CONNECT_DIFFERENT_GEARS))
                .build(exporter, "polyfactory:main/base/steel_gear");

        var tachometer = Advancement.Builder.create()
                .parent(gear)
                .display(
                        FactoryItems.TACHOMETER,
                        Text.translatable("advancements.polyfactory.tachometer.title"),
                        Text.translatable("advancements.polyfactory.tachometer.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", ItemCriterion.Conditions.createPlacedBlock(
                        AnyOfLootCondition.builder(
                                BlockStatePropertyLootCondition.builder(FactoryBlocks.TACHOMETER),
                                BlockStatePropertyLootCondition.builder(FactoryBlocks.STRESSOMETER)
                        )))
                .build(exporter, "polyfactory:main/base/tachometer");

        // Steel -> Press

        var sprayCan = Advancement.Builder.create()
                .parent(grinder)
                .display(
                        new GuiElementBuilder(FactoryItems.SPRAY_CAN)
                                .setComponent(FactoryDataComponents.COLOR, DyeColorExtra.getColor(DyeColor.BLUE))
                                .setComponent(FactoryDataComponents.USES_LEFT, DyeSprayItem.MAX_USES)
                                .asStack(),
                        Text.translatable("advancements.polyfactory.spray_can.title"),
                        Text.translatable("advancements.polyfactory.spray_can.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", ItemCriterion.Conditions.createItemUsedOnBlock(LocationPredicate.Builder.create(), ItemPredicate.Builder.create().items(itemWrap, FactoryItems.SPRAY_CAN)))
                .build(exporter, "polyfactory:main/base/spray_can");

        var crafter = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.CRAFTER,
                        Text.translatable("advancements.polyfactory.crafter.title"),
                        Text.translatable("advancements.polyfactory.crafter.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS))
                .build(exporter, "polyfactory:main/base/crafter");

        var crafter2 = Advancement.Builder.create()
                .parent(crafter)
                .display(
                        new GuiElementBuilder(FactoryItems.CRAFTER).glow().asStack(),
                        Text.translatable("advancements.polyfactory.crafter2.title"),
                        Text.translatable("advancements.polyfactory.crafter2.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        true
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS_CRAFTER))
                .build(exporter, "polyfactory:main/base/crafter2");

        var steamEngine = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.STEAM_ENGINE,
                        Text.translatable("advancements.polyfactory.steam_engine.title"),
                        Text.translatable("advancements.polyfactory.steam_engine.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.FUEL_STEAM_ENGINE))
                .build(exporter, "polyfactory:main/base/steam_engine");

        var workbench = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.WORKBENCH,
                        Text.translatable("advancements.polyfactory.workbench.title"),
                        Text.translatable("advancements.polyfactory.workbench.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("craft", RecipeCraftedCriterion.Conditions.create(recipeKey("workbench")))
                .build(exporter, "polyfactory:main/base/workbench");

        var blueprintWorkbench = Advancement.Builder.create()
                .parent(workbench)
                .display(
                        FactoryItems.BLUEPRINT_WORKBENCH,
                        Text.translatable("advancements.polyfactory.blueprint_workbench.title"),
                        Text.translatable("advancements.polyfactory.blueprint_workbench.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("craft", TriggerCriterion.of(FactoryTriggers.CRAFT_WITH_BLUEPRINT_WORKBENCH))
                .build(exporter, "polyfactory:main/base/blueprint_workbench");

        var container = Advancement.Builder.create()
                .parent(workbench)
                .display(
                        FactoryItems.CONTAINER,
                        Text.translatable("advancements.polyfactory.container.title"),
                        Text.translatable("advancements.polyfactory.container.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CONTAINER_ADD_ITEM))
                .build(exporter, "polyfactory:main/base/container");


        var itemFilter = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.ITEM_FILTER,
                        Text.translatable("advancements.polyfactory.item_filter.title"),
                        Text.translatable("advancements.polyfactory.item_filter.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_FILTER_USE))
                .build(exporter, "polyfactory:main/base/item_filter");

        var itemPacker = Advancement.Builder.create()
                .parent(container)
                .display(
                        FactoryItems.ITEM_PACKER,
                        Text.translatable("advancements.polyfactory.item_packer.title"),
                        Text.translatable("advancements.polyfactory.item_packer.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_PACKER_ACCESSES))
                .build(exporter, "polyfactory:main/base/item_packer");

        var fan = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.FAN,
                        Text.translatable("advancements.polyfactory.fan.title"),
                        Text.translatable("advancements.polyfactory.fan.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN))
                .build(exporter, "polyfactory:main/base/fan");

        var fanSky = Advancement.Builder.create()
                .parent(fan)
                .display(
                        Items.FEATHER,
                        Text.translatable("advancements.polyfactory.fan_sky.title"),
                        Text.translatable("advancements.polyfactory.fan_sky.description"),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        true,
                        true
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN_A_LOT))
                .build(exporter, "polyfactory:main/base/fan_sky");

        var ejector = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.EJECTOR,
                        Text.translatable("advancements.polyfactory.ejector.title"),
                        Text.translatable("advancements.polyfactory.ejector.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.LAUNCHED_BY_EJECTOR))
                .build(exporter, "polyfactory:main/base/ejector");

        var miner = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.MINER,
                        Text.translatable("advancements.polyfactory.miner.title"),
                        Text.translatable("advancements.polyfactory.miner.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.MINER_MINES))
                .build(exporter, "polyfactory:main/base/miner");

        var placer = Advancement.Builder.create()
                .parent(miner)
                .display(
                        FactoryItems.PLACER,
                        Text.translatable("advancements.polyfactory.placer.title"),
                        Text.translatable("advancements.polyfactory.placer.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.PLACER_PLACES))
                .build(exporter, "polyfactory:main/base/placer");

        var planter = Advancement.Builder.create()
                .parent(miner)
                .display(
                        FactoryItems.PLANTER,
                        Text.translatable("advancements.polyfactory.planter.title"),
                        Text.translatable("advancements.polyfactory.planter.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.PLANTER_PLANTS))
                .build(exporter, "polyfactory:main/base/planter");


        // Plates -> Fluids
        this.fluids(exporter, itemWrap, press);
    }

    private void fluids(Consumer<AdvancementEntry> exporter, RegistryWrapper.Impl<Item> itemWrap, AdvancementEntry press) {
        var pipe = Advancement.Builder.create()
                .parent(press)
                .display(
                        FactoryItems.PIPE,
                        Text.translatable("advancements.polyfactory.pipe.title"),
                        Text.translatable("advancements.polyfactory.pipe.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", ItemCriterion.Conditions.createPlacedBlock(FactoryBlocks.PIPE))
                .build(exporter, "polyfactory:main/base/pipe");
        var drain = Advancement.Builder.create()
                .parent(pipe)
                .display(
                        FactoryItems.DRAIN,
                        Text.translatable("advancements.polyfactory.drain.title"),
                        Text.translatable("advancements.polyfactory.drain.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.DRAIN_USE))
                .build(exporter, "polyfactory:main/base/drain");
        var mechanicalSpout = Advancement.Builder.create()
                .parent(drain)
                .display(
                        FactoryItems.MECHANICAL_SPOUT,
                        Text.translatable("advancements.polyfactory.mechanical_spout.title"),
                        Text.translatable("advancements.polyfactory.mechanical_spout.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.SPOUT_CRAFT))
                .build(exporter, "polyfactory:main/base/mechanical_spout");

        var crispHoney = Advancement.Builder.create()
                .parent(drain)
                .display(
                        FactoryItems.CRISPY_HONEY,
                        Text.translatable("advancements.polyfactory.crispy_honey.title"),
                        Text.translatable("advancements.polyfactory.crispy_honey.description"),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        true,
                        true
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("fluid_interaction/honey_lava"),
                        List.of(ItemPredicate.Builder.create().items(itemWrap, FactoryItems.CRISPY_HONEY))))
                .build(exporter, "polyfactory:main/base/crispy_honey");

        var honeyedApple = Advancement.Builder.create()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.HONEYED_APPLE,
                        Text.translatable("advancements.polyfactory.honeyed_apple.title"),
                        Text.translatable("advancements.polyfactory.honeyed_apple.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("spout/honeyed_apple")))
                .build(exporter, "polyfactory:main/base/honeyed_apple");

        var brittleGlassBottle = Advancement.Builder.create()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.BRITTLE_POTION.getDefaultStack(),
                        Text.translatable("advancements.polyfactory.brittle_glass_bottle.title"),
                        Text.translatable("advancements.polyfactory.brittle_glass_bottle.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("spout/brittle_glass_bottle")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(recipeKey("spout/brittle_potion")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/brittle_glass_bottle");


        var fluidTank = Advancement.Builder.create()
                .parent(pipe)
                .display(
                        FactoryItems.FLUID_TANK,
                        Text.translatable("advancements.polyfactory.fluid_tank.title"),
                        Text.translatable("advancements.polyfactory.fluid_tank.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.FLUID_TANK_CONNECT))
                .build(exporter, "polyfactory:main/base/fluid_tank");
        var portableFluidTank = Advancement.Builder.create()
                .parent(fluidTank)
                .display(
                        FactoryItems.PORTABLE_FLUID_TANK,
                        Text.translatable("advancements.polyfactory.portable_fluid_tank.title"),
                        Text.translatable("advancements.polyfactory.portable_fluid_tank.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", InventoryChangedCriterion.Conditions.items(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.create().items(itemWrap, FactoryItems.PORTABLE_FLUID_TANK), FactoryItemPredicates.HAS_FLUIDS)
                ))
                .build(exporter, "polyfactory:main/base/portable_fluid_tank");

        var nozzle = Advancement.Builder.create()
                .parent(pipe)
                .display(
                        FactoryItems.NOZZLE,
                        Text.translatable("advancements.polyfactory.nozzle.title"),
                        Text.translatable("advancements.polyfactory.nozzle.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", FluidShootsCriterion.ofNozzle())
                .build(exporter, "polyfactory:main/base/nozzle");

        var stickyEffect = Advancement.Builder.create()
                .parent(nozzle)
                .display(
                        FactoryItems.HONEY_BUCKET,
                        Text.translatable("advancements.polyfactory.sticky_effect.title"),
                        Text.translatable("advancements.polyfactory.sticky_effect.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.STICKY_WALL_SLIDING))
                .build(exporter, "polyfactory:main/base/sticky_effect");

        var pressureFluidGun = Advancement.Builder.create()
                .parent(nozzle)
                .display(
                        FactoryItems.PRESSURE_FLUID_GUN,
                        Text.translatable("advancements.polyfactory.pressure_fluid_gun.title"),
                        Text.translatable("advancements.polyfactory.pressure_fluid_gun.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", RecipeCraftedCriterion.Conditions.create(recipeKey("pressure_fluid_gun")))
                .build(exporter, "polyfactory:main/base/pressure_fluid_gun");

        var pressureFluidGunHealing = Advancement.Builder.create()
                .parent(pressureFluidGun)
                .display(
                        new GuiElementBuilder(FactoryItems.PRESSURE_FLUID_GUN).glow().asStack(),
                        Text.translatable("advancements.polyfactory.pressure_fluid_gun_healing.title"),
                        Text.translatable("advancements.polyfactory.pressure_fluid_gun_healing.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        true
                )
                .criterion("use", FluidShootsCriterion.ofFluidLauncher(
                        FactoryFluids.getPotion(Potions.HEALING),
                        FactoryFluids.getPotion(Potions.STRONG_HEALING),
                        FactoryFluids.getPotion(Potions.REGENERATION),
                        FactoryFluids.getPotion(Potions.STRONG_REGENERATION),
                        FactoryFluids.getPotion(Potions.LONG_REGENERATION)
                ))
                .build(exporter, "polyfactory:main/base/pressure_fluid_gun_healing");


        var nozzleLava = Advancement.Builder.create()
                .parent(nozzle)
                .display(
                        new GuiElementBuilder(FactoryItems.NOZZLE).glow().asStack(),
                        Text.translatable("advancements.polyfactory.nozzle_lava.title"),
                        Text.translatable("advancements.polyfactory.nozzle_lava.description"),
                        null,
                        AdvancementFrame.GOAL,
                        true,
                        true,
                        false
                )
                .criterion("use", FluidShootsCriterion.ofNozzle(FactoryFluids.LAVA.defaultInstance()))
                .criterion("use2", FluidShootsCriterion.ofFluidLauncher(FactoryFluids.LAVA.defaultInstance()))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/nozzle_lava");
    }

    private void cables(Consumer<AdvancementEntry> exporter, RegistryWrapper.Impl<Item> itemWrap, AdvancementEntry mixer) {
        var cable = Advancement.Builder.create()
                .parent(mixer)
                .display(
                        ColoredItem.stack(FactoryItems.CABLE, 1, DyeColor.RED),
                        Text.translatable("advancements.polyfactory.cable.title"),
                        Text.translatable("advancements.polyfactory.cable.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.CABLE_CONNECT))
                .build(exporter, "polyfactory:main/base/cable");

        var recordPlayer = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.RECORD_PLAYER,
                        Text.translatable("advancements.polyfactory.record_player.title"),
                        Text.translatable("advancements.polyfactory.record_player.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("a", TriggerCriterion.of(FactoryTriggers.CONNECT_RECORD_PLAYER_AND_SPEAKERS))
                .build(exporter, "polyfactory:main/base/record_player");

        var redstone = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.REDSTONE_INPUT,
                        Text.translatable("advancements.polyfactory.redstone.title"),
                        Text.translatable("advancements.polyfactory.redstone.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("a", TriggerCriterion.of(FactoryTriggers.REDSTONE_IN))
                .criterion("b", TriggerCriterion.of(FactoryTriggers.REDSTONE_OUT))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.AND)
                .build(exporter, "polyfactory:main/base/redstone");


        var wirelessRedstone = Advancement.Builder.create()
                .parent(redstone)
                .display(
                        FactoryItems.WIRELESS_REDSTONE_TRANSMITTER,
                        Text.translatable("advancements.polyfactory.wireless_redstone.title"),
                        Text.translatable("advancements.polyfactory.wireless_redstone.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("a", TriggerCriterion.of(FactoryTriggers.WIRELESS_REDSTONE))
                .build(exporter, "polyfactory:main/base/wireless_redstone");

        // Mixer -> Cable

        var itemReader = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.ITEM_READER,
                        Text.translatable("advancements.polyfactory.item_reader.title"),
                        Text.translatable("advancements.polyfactory.item_reader.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_READER))
                .build(exporter, "polyfactory:main/base/item_reader");

        var aritheticOperator = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.ARITHMETIC_OPERATOR,
                        Text.translatable("advancements.polyfactory.arithmetic_operator.title"),
                        Text.translatable("advancements.polyfactory.arithmetic_operator.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.ARITHMETIC_OPERATOR))
                .build(exporter, "polyfactory:main/base/arithmetic_operator");

        var dataComparator = Advancement.Builder.create()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_COMPARATOR,
                        Text.translatable("advancements.polyfactory.data_comparator.title"),
                        Text.translatable("advancements.polyfactory.data_comparator.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.DATA_COMPARATOR))
                .build(exporter, "polyfactory:main/base/data_comparator");

        var dataMemory = Advancement.Builder.create()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_MEMORY,
                        Text.translatable("advancements.polyfactory.data_memory.title"),
                        Text.translatable("advancements.polyfactory.data_memory.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.DATA_MEMORY))
                .build(exporter, "polyfactory:main/base/data_memory");

        var nixieTubes = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.NIXIE_TUBE,
                        Text.translatable("advancements.polyfactory.nixie_tube.title"),
                        Text.translatable("advancements.polyfactory.nixie_tube.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.NIXIE_TUBE_CONNECTED_3_OR_MORE))
                .build(exporter, "polyfactory:main/base/nixie_tube");

        var hologramProjector = Advancement.Builder.create()
                .parent(nixieTubes)
                .display(
                        FactoryItems.HOLOGRAM_PROJECTOR,
                        Text.translatable("advancements.polyfactory.hologram_projector.title"),
                        Text.translatable("advancements.polyfactory.hologram_projector.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.HOLOGRAM_PROJECTOR_ACTIVATES))
                .build(exporter, "polyfactory:main/base/hologram_projector");
    }
}
