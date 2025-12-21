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
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.RecipeCraftedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;
import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

class AdvancementsProvider extends FabricAdvancementProvider {


    protected AdvancementsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> exporter) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);
        var root = Advancement.Builder.advancement()
                .display(
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
                .save(exporter, "polyfactory:main/root");

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
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.SMELTERY_MELTS))
                .save(exporter, "polyfactory:main/smeltery/primitive_smeltery");

        var smeltery = Advancement.Builder.advancement()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.SMELTERY,
                        Component.translatable("advancements.polyfactory.smeltery.title"),
                        Component.translatable("advancements.polyfactory.smeltery.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.INDUSTRIAL_SMELTERY_CREATED))
                .save(exporter, "polyfactory:main/smeltery/smeltery");

        var castingTable = Advancement.Builder.advancement()
                .parent(primitiveSmeltery)
                .display(
                        FactoryItems.CASTING_TABLE,
                        Component.translatable("advancements.polyfactory.casting_table.title"),
                        Component.translatable("advancements.polyfactory.casting_table.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_METAL))
                .save(exporter, "polyfactory:main/smeltery/casting_table");


        var mold = Advancement.Builder.advancement()
                .parent(castingTable)
                .display(
                        FactoryItems.INGOT_MOLD.mold(),
                        Component.translatable("advancements.polyfactory.mold.title"),
                        Component.translatable("advancements.polyfactory.mold.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CASTING_MOLD))
                .save(exporter, "polyfactory:main/smeltery/mold");
    }

    private void taters(HolderLookup.Provider registryLookup, AdvancementHolder root, Consumer<AdvancementHolder> exporter) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);

        var tater16 = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        Items.POTATO,
                        Component.translatable("advancements.polyfactory.tater_16.title"),
                        Component.translatable("advancements.polyfactory.tater_16.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_16))
                .save(exporter, "polyfactory:main/taters/16");

        var tater128 = Advancement.Builder.advancement()
                .parent(tater16)
                .display(
                        Items.BAKED_POTATO,
                        Component.translatable("advancements.polyfactory.tater_128.title"),
                        Component.translatable("advancements.polyfactory.tater_128.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_128))
                .save(exporter, "polyfactory:main/taters/128");

        var tater1024 = Advancement.Builder.advancement()
                .parent(tater128)
                .display(
                        FactoryItems.TINY_POTATO_SPRING,
                        Component.translatable("advancements.polyfactory.tater_1024.title"),
                        Component.translatable("advancements.polyfactory.tater_1024.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.TATER_1024))
                .save(exporter, "polyfactory:main/taters/1023");
    }

    private void mainline(HolderLookup.Provider registryLookup, AdvancementHolder root, Consumer<AdvancementHolder> exporter) {
        var itemWrap = registryLookup.lookupOrThrow(Registries.ITEM);

        // Start

        var handCrank = Advancement.Builder.advancement()
                .parent(root)
                .display(
                        FactoryItems.HAND_CRANK,
                        Component.translatable("advancements.polyfactory.hand_crank.title"),
                        Component.translatable("advancements.polyfactory.hand_crank.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.POWER_HAND_CRANK))
                .save(exporter, "polyfactory:main/base/hand_crank");

        var grinder = Advancement.Builder.advancement()
                .parent(handCrank)
                .display(
                        FactoryItems.GRINDER,
                        Component.translatable("advancements.polyfactory.grinder.title"),
                        Component.translatable("advancements.polyfactory.grinder.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/coal_dust")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/planks_saw_dust")))
                .addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/logs_saw_dust")))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/grinder_dust");

        var dynamite = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.DYNAMITE,
                        Component.translatable("advancements.polyfactory.dynamite.title"),
                        Component.translatable("advancements.polyfactory.dynamite.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.DYNAMITE))
                .save(exporter, "polyfactory:main/base/dynamite");

        var crushedRawOre = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.CRUSHED_RAW_IRON,
                        Component.translatable("advancements.polyfactory.crushed_raw_ore.title"),
                        Component.translatable("advancements.polyfactory.crushed_raw_ore.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/crushed_raw_copper")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/crushed_raw_iron")))
                .addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/crushed_raw_gold")))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/crushed_raw_ore");

        // Grinder -> Gravel

        var coloredLamp = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        ColoredItem.stack(FactoryItems.LAMP, 1, FactoryColors.YTTR_TEAL),
                        Component.translatable("advancements.polyfactory.colored_lamp.title"),
                        Component.translatable("advancements.polyfactory.colored_lamp.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.LAMP))
                .addCriterion("use2", InventoryChangeTrigger.TriggerInstance.hasItems(FactoryItems.CAGED_LAMP))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/colored_lamp");

        var gravel = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        Items.GRAVEL,
                        Component.translatable("advancements.polyfactory.gravel.title"),
                        Component.translatable("advancements.polyfactory.gravel.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/cobblestone_to_gravel")))
                .addCriterion("use1", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/stone_to_cobblestone")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("grinding/gravel_to_sand")))
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(exporter, "polyfactory:main/base/gravel");

        // Grinder -> Steel

        var steel = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        FactoryItems.STEEL_INGOT,
                        Component.translatable("advancements.polyfactory.steel_ingot.title"),
                        Component.translatable("advancements.polyfactory.steel_ingot.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("steel_ingot")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("steel_ingot_blasting")))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/steel_ingot");

        var windmill = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.WINDMILL_SAIL,
                        Component.translatable("advancements.polyfactory.windmill.title"),
                        Component.translatable("advancements.polyfactory.windmill.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONSTRUCT_WORKING_WINDMILL))
                .save(exporter, "polyfactory:main/base/windmill");

        var wrench = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.WRENCH,
                        Component.translatable("advancements.polyfactory.wrench.title"),
                        Component.translatable("advancements.polyfactory.wrench.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.WRENCH))
                .save(exporter, "polyfactory:main/base/wrench");


        var mixer = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.MIXER,
                        Component.translatable("advancements.polyfactory.mixer.title"),
                        Component.translatable("advancements.polyfactory.mixer.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MIXER_CRAFTS))
                .save(exporter, "polyfactory:main/base/mixer");

        // Steel -> Mixer

        var cake = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        Items.CAKE,
                        Component.translatable("advancements.polyfactory.cake.title"),
                        Component.translatable("advancements.polyfactory.cake.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("mixing/cake")))
                .save(exporter, "polyfactory:main/base/cake");

        var dye = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        ColoredItem.stack(FactoryItems.ARTIFICIAL_DYE, 1, 0x42f5a4),
                        Component.translatable("advancements.polyfactory.artificial_dye.title"),
                        Component.translatable("advancements.polyfactory.artificial_dye.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("mixing/artificial_dye")))
                .save(exporter, "polyfactory:main/base/mixer/artificial_dye");

        var firework = Advancement.Builder.advancement()
                .parent(dye)
                .display(
                        Items.FIREWORK_ROCKET,
                        Component.translatable("advancements.polyfactory.firework.title"),
                        Component.translatable("advancements.polyfactory.firework.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(ResourceKey.create(Registries.RECIPE, Identifier.parse("firework_rocket")), List.of(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.item(), FactoryItemPredicates.CUSTOM_FIREWORK_COLOR))))
                .save(exporter, "polyfactory:main/base/mixer/firework");


        this.cables(exporter, itemWrap, mixer);

        // Steel

        var press = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.PRESS,
                        Component.translatable("advancements.polyfactory.press.title"),
                        Component.translatable("advancements.polyfactory.press.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("press/steel_plate")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("press/wooden_plate")))
                .addCriterion("use3", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("press/copper_plate")))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/press");

        var gear = Advancement.Builder.advancement()
                .parent(steel)
                .display(
                        FactoryItems.STEEL_GEAR,
                        Component.translatable("advancements.polyfactory.steel_gear.title"),
                        Component.translatable("advancements.polyfactory.steel_gear.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONNECT_DIFFERENT_GEARS))
                .save(exporter, "polyfactory:main/base/steel_gear");

        var tachometer = Advancement.Builder.advancement()
                .parent(gear)
                .display(
                        FactoryItems.TACHOMETER,
                        Component.translatable("advancements.polyfactory.tachometer.title"),
                        Component.translatable("advancements.polyfactory.tachometer.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
                        AnyOfCondition.anyOf(
                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(FactoryBlocks.TACHOMETER),
                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(FactoryBlocks.STRESSOMETER)
                        )))
                .save(exporter, "polyfactory:main/base/tachometer");

        var chainDrive = Advancement.Builder.advancement()
                .parent(gear)
                .display(
                        FactoryItems.CHAIN_DRIVE,
                        Component.translatable("advancements.polyfactory.chain_drive.title"),
                        Component.translatable("advancements.polyfactory.chain_drive.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CHAIN_DRIVES_CONNECTED))
                .save(exporter, "polyfactory:main/base/chain_drive");

        var chainLift = Advancement.Builder.advancement()
                .parent(chainDrive)
                .display(
                        FactoryItems.CHAIN_LIFT,
                        Component.translatable("advancements.polyfactory.chain_lift.title"),
                        Component.translatable("advancements.polyfactory.chain_lift.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CHAIN_LIFT))
                .save(exporter, "polyfactory:main/base/chain_lift");

        // Steel -> Press

        var sprayCan = Advancement.Builder.advancement()
                .parent(grinder)
                .display(
                        new GuiElementBuilder(FactoryItems.SPRAY_CAN)
                                .setComponent(FactoryDataComponents.COLOR, DyeColorExtra.getColor(DyeColor.BLUE))
                                .setComponent(FactoryDataComponents.USES_LEFT, DyeSprayItem.MAX_USES)
                                .asStack(),
                        Component.translatable("advancements.polyfactory.spray_can.title"),
                        Component.translatable("advancements.polyfactory.spray_can.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location(), ItemPredicate.Builder.item().of(itemWrap, FactoryItems.SPRAY_CAN)))
                .save(exporter, "polyfactory:main/base/spray_can");

        var crafter = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.CRAFTER,
                        Component.translatable("advancements.polyfactory.crafter.title"),
                        Component.translatable("advancements.polyfactory.crafter.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS))
                .save(exporter, "polyfactory:main/base/crafter");

        var crafter2 = Advancement.Builder.advancement()
                .parent(crafter)
                .display(
                        new GuiElementBuilder(FactoryItems.CRAFTER).glow().asStack(),
                        Component.translatable("advancements.polyfactory.crafter2.title"),
                        Component.translatable("advancements.polyfactory.crafter2.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CRAFTER_CRAFTS_CRAFTER))
                .save(exporter, "polyfactory:main/base/crafter2");

        var steamEngine = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.STEAM_ENGINE,
                        Component.translatable("advancements.polyfactory.steam_engine.title"),
                        Component.translatable("advancements.polyfactory.steam_engine.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.FUEL_STEAM_ENGINE))
                .save(exporter, "polyfactory:main/base/steam_engine");

        var workbench = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.WORKBENCH,
                        Component.translatable("advancements.polyfactory.workbench.title"),
                        Component.translatable("advancements.polyfactory.workbench.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("craft", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("workbench")))
                .save(exporter, "polyfactory:main/base/workbench");

        var blueprintWorkbench = Advancement.Builder.advancement()
                .parent(workbench)
                .display(
                        FactoryItems.BLUEPRINT_WORKBENCH,
                        Component.translatable("advancements.polyfactory.blueprint_workbench.title"),
                        Component.translatable("advancements.polyfactory.blueprint_workbench.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("craft", TriggerCriterion.of(FactoryTriggers.CRAFT_WITH_BLUEPRINT_WORKBENCH))
                .save(exporter, "polyfactory:main/base/blueprint_workbench");

        var container = Advancement.Builder.advancement()
                .parent(workbench)
                .display(
                        FactoryItems.CONTAINER,
                        Component.translatable("advancements.polyfactory.container.title"),
                        Component.translatable("advancements.polyfactory.container.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CONTAINER_ADD_ITEM))
                .save(exporter, "polyfactory:main/base/container");


        var itemFilter = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.ITEM_FILTER,
                        Component.translatable("advancements.polyfactory.item_filter.title"),
                        Component.translatable("advancements.polyfactory.item_filter.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_FILTER_USE))
                .save(exporter, "polyfactory:main/base/item_filter");

        var itemPacker = Advancement.Builder.advancement()
                .parent(container)
                .display(
                        FactoryItems.ITEM_PACKER,
                        Component.translatable("advancements.polyfactory.item_packer.title"),
                        Component.translatable("advancements.polyfactory.item_packer.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_PACKER_ACCESSES))
                .save(exporter, "polyfactory:main/base/item_packer");

        var fan = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.FAN,
                        Component.translatable("advancements.polyfactory.fan.title"),
                        Component.translatable("advancements.polyfactory.fan.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN))
                .save(exporter, "polyfactory:main/base/fan");

        var fanSky = Advancement.Builder.advancement()
                .parent(fan)
                .display(
                        Items.FEATHER,
                        Component.translatable("advancements.polyfactory.fan_sky.title"),
                        Component.translatable("advancements.polyfactory.fan_sky.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MOVED_BY_FAN_A_LOT))
                .save(exporter, "polyfactory:main/base/fan_sky");

        var ejector = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.EJECTOR,
                        Component.translatable("advancements.polyfactory.ejector.title"),
                        Component.translatable("advancements.polyfactory.ejector.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.LAUNCHED_BY_EJECTOR))
                .save(exporter, "polyfactory:main/base/ejector");

        var miner = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.MINER,
                        Component.translatable("advancements.polyfactory.miner.title"),
                        Component.translatable("advancements.polyfactory.miner.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.MINER_MINES))
                .save(exporter, "polyfactory:main/base/miner");

        var placer = Advancement.Builder.advancement()
                .parent(miner)
                .display(
                        FactoryItems.PLACER,
                        Component.translatable("advancements.polyfactory.placer.title"),
                        Component.translatable("advancements.polyfactory.placer.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.PLACER_PLACES))
                .save(exporter, "polyfactory:main/base/placer");

        var planter = Advancement.Builder.advancement()
                .parent(miner)
                .display(
                        FactoryItems.PLANTER,
                        Component.translatable("advancements.polyfactory.planter.title"),
                        Component.translatable("advancements.polyfactory.planter.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.PLANTER_PLANTS))
                .save(exporter, "polyfactory:main/base/planter");


        // Plates -> Fluids
        this.fluids(exporter, itemWrap, press);
    }

    private void fluids(Consumer<AdvancementHolder> exporter, HolderLookup.RegistryLookup<Item> itemWrap, AdvancementHolder press) {
        var pipe = Advancement.Builder.advancement()
                .parent(press)
                .display(
                        FactoryItems.PIPE,
                        Component.translatable("advancements.polyfactory.pipe.title"),
                        Component.translatable("advancements.polyfactory.pipe.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(FactoryBlocks.PIPE))
                .save(exporter, "polyfactory:main/base/pipe");
        var drain = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.DRAIN,
                        Component.translatable("advancements.polyfactory.drain.title"),
                        Component.translatable("advancements.polyfactory.drain.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DRAIN_USE))
                .save(exporter, "polyfactory:main/base/drain");
        var mechanicalSpout = Advancement.Builder.advancement()
                .parent(drain)
                .display(
                        FactoryItems.MECHANICAL_SPOUT,
                        Component.translatable("advancements.polyfactory.mechanical_spout.title"),
                        Component.translatable("advancements.polyfactory.mechanical_spout.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.SPOUT_CRAFT))
                .save(exporter, "polyfactory:main/base/mechanical_spout");

        var crispHoney = Advancement.Builder.advancement()
                .parent(drain)
                .display(
                        FactoryItems.CRISPY_HONEY,
                        Component.translatable("advancements.polyfactory.crispy_honey.title"),
                        Component.translatable("advancements.polyfactory.crispy_honey.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("fluid_interaction/honey_lava"),
                        List.of(ItemPredicate.Builder.item().of(itemWrap, FactoryItems.CRISPY_HONEY))))
                .save(exporter, "polyfactory:main/base/crispy_honey");

        var honeyedApple = Advancement.Builder.advancement()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.HONEYED_APPLE,
                        Component.translatable("advancements.polyfactory.honeyed_apple.title"),
                        Component.translatable("advancements.polyfactory.honeyed_apple.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("spout/honeyed_apple")))
                .save(exporter, "polyfactory:main/base/honeyed_apple");

        var brittleGlassBottle = Advancement.Builder.advancement()
                .parent(mechanicalSpout)
                .display(
                        FactoryItems.BRITTLE_POTION.getDefaultInstance(),
                        Component.translatable("advancements.polyfactory.brittle_glass_bottle.title"),
                        Component.translatable("advancements.polyfactory.brittle_glass_bottle.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("spout/brittle_glass_bottle")))
                .addCriterion("use2", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("spout/brittle_potion")))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/brittle_glass_bottle");


        var fluidTank = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.FLUID_TANK,
                        Component.translatable("advancements.polyfactory.fluid_tank.title"),
                        Component.translatable("advancements.polyfactory.fluid_tank.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.FLUID_TANK_CONNECT))
                .save(exporter, "polyfactory:main/base/fluid_tank");
        var portableFluidTank = Advancement.Builder.advancement()
                .parent(fluidTank)
                .display(
                        FactoryItems.PORTABLE_FLUID_TANK,
                        Component.translatable("advancements.polyfactory.portable_fluid_tank.title"),
                        Component.translatable("advancements.polyfactory.portable_fluid_tank.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.item().of(itemWrap, FactoryItems.PORTABLE_FLUID_TANK), FactoryItemPredicates.HAS_FLUIDS)
                ))
                .save(exporter, "polyfactory:main/base/portable_fluid_tank");

        var nozzle = Advancement.Builder.advancement()
                .parent(pipe)
                .display(
                        FactoryItems.NOZZLE,
                        Component.translatable("advancements.polyfactory.nozzle.title"),
                        Component.translatable("advancements.polyfactory.nozzle.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", FluidShootsCriterion.ofNozzle())
                .save(exporter, "polyfactory:main/base/nozzle");

        var stickyEffect = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        FactoryItems.HONEY_BUCKET,
                        Component.translatable("advancements.polyfactory.sticky_effect.title"),
                        Component.translatable("advancements.polyfactory.sticky_effect.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.STICKY_WALL_SLIDING))
                .save(exporter, "polyfactory:main/base/sticky_effect");

        var pressureFluidGun = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        FactoryItems.PRESSURE_FLUID_GUN,
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun.title"),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", RecipeCraftedTrigger.TriggerInstance.craftedItem(recipeKey("pressure_fluid_gun")))
                .save(exporter, "polyfactory:main/base/pressure_fluid_gun");

        var pressureFluidGunHealing = Advancement.Builder.advancement()
                .parent(pressureFluidGun)
                .display(
                        new GuiElementBuilder(FactoryItems.PRESSURE_FLUID_GUN).glow().asStack(),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun_healing.title"),
                        Component.translatable("advancements.polyfactory.pressure_fluid_gun_healing.description"),
                        null,
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
                .save(exporter, "polyfactory:main/base/pressure_fluid_gun_healing");


        var nozzleLava = Advancement.Builder.advancement()
                .parent(nozzle)
                .display(
                        new GuiElementBuilder(FactoryItems.NOZZLE).glow().asStack(),
                        Component.translatable("advancements.polyfactory.nozzle_lava.title"),
                        Component.translatable("advancements.polyfactory.nozzle_lava.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false
                )
                .addCriterion("use", FluidShootsCriterion.ofNozzle(FactoryFluids.LAVA.defaultInstance()))
                .addCriterion("use2", FluidShootsCriterion.ofFluidLauncher(FactoryFluids.LAVA.defaultInstance()))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(exporter, "polyfactory:main/base/nozzle_lava");
    }

    private void cables(Consumer<AdvancementHolder> exporter, HolderLookup.RegistryLookup<Item> itemWrap, AdvancementHolder mixer) {
        var cable = Advancement.Builder.advancement()
                .parent(mixer)
                .display(
                        ColoredItem.stack(FactoryItems.CABLE, 1, DyeColor.RED),
                        Component.translatable("advancements.polyfactory.cable.title"),
                        Component.translatable("advancements.polyfactory.cable.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.CABLE_CONNECT))
                .save(exporter, "polyfactory:main/base/cable");

        var recordPlayer = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.RECORD_PLAYER,
                        Component.translatable("advancements.polyfactory.record_player.title"),
                        Component.translatable("advancements.polyfactory.record_player.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.CONNECT_RECORD_PLAYER_AND_SPEAKERS))
                .save(exporter, "polyfactory:main/base/record_player");

        var redstone = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.REDSTONE_INPUT,
                        Component.translatable("advancements.polyfactory.redstone.title"),
                        Component.translatable("advancements.polyfactory.redstone.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.REDSTONE_IN))
                .addCriterion("b", TriggerCriterion.of(FactoryTriggers.REDSTONE_OUT))
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(exporter, "polyfactory:main/base/redstone");


        var wirelessRedstone = Advancement.Builder.advancement()
                .parent(redstone)
                .display(
                        FactoryItems.WIRELESS_REDSTONE_TRANSMITTER,
                        Component.translatable("advancements.polyfactory.wireless_redstone.title"),
                        Component.translatable("advancements.polyfactory.wireless_redstone.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("a", TriggerCriterion.of(FactoryTriggers.WIRELESS_REDSTONE))
                .save(exporter, "polyfactory:main/base/wireless_redstone");

        // Mixer -> Cable

        var itemReader = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.ITEM_READER,
                        Component.translatable("advancements.polyfactory.item_reader.title"),
                        Component.translatable("advancements.polyfactory.item_reader.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ITEM_READER))
                .save(exporter, "polyfactory:main/base/item_reader");

        var aritheticOperator = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.ARITHMETIC_OPERATOR,
                        Component.translatable("advancements.polyfactory.arithmetic_operator.title"),
                        Component.translatable("advancements.polyfactory.arithmetic_operator.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.ARITHMETIC_OPERATOR))
                .save(exporter, "polyfactory:main/base/arithmetic_operator");

        var dataComparator = Advancement.Builder.advancement()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_COMPARATOR,
                        Component.translatable("advancements.polyfactory.data_comparator.title"),
                        Component.translatable("advancements.polyfactory.data_comparator.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DATA_COMPARATOR))
                .save(exporter, "polyfactory:main/base/data_comparator");

        var dataMemory = Advancement.Builder.advancement()
                .parent(aritheticOperator)
                .display(
                        FactoryItems.DATA_MEMORY,
                        Component.translatable("advancements.polyfactory.data_memory.title"),
                        Component.translatable("advancements.polyfactory.data_memory.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.DATA_MEMORY))
                .save(exporter, "polyfactory:main/base/data_memory");

        var nixieTubes = Advancement.Builder.advancement()
                .parent(cable)
                .display(
                        FactoryItems.NIXIE_TUBE,
                        Component.translatable("advancements.polyfactory.nixie_tube.title"),
                        Component.translatable("advancements.polyfactory.nixie_tube.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.NIXIE_TUBE_CONNECTED_3_OR_MORE))
                .save(exporter, "polyfactory:main/base/nixie_tube");

        var hologramProjector = Advancement.Builder.advancement()
                .parent(nixieTubes)
                .display(
                        FactoryItems.HOLOGRAM_PROJECTOR,
                        Component.translatable("advancements.polyfactory.hologram_projector.title"),
                        Component.translatable("advancements.polyfactory.hologram_projector.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion("use", TriggerCriterion.of(FactoryTriggers.HOLOGRAM_PROJECTOR_ACTIVATES))
                .save(exporter, "polyfactory:main/base/hologram_projector");
    }
}
