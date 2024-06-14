package eu.pb4.polyfactory.datagen;

import eu.pb4.factorytools.impl.ExtraItemPredicate;
import eu.pb4.polyfactory.advancement.FactoryItemPredicates;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.FactoryColors;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.ItemCriterion;
import net.minecraft.advancement.criterion.RecipeCraftedCriterion;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

class AdvancementsProvider extends FabricAdvancementProvider {


    protected AdvancementsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> exporter) {
        var root = Advancement.Builder.create()
                .display(
                        FactoryItems.WINDMILL_SAIL,
                        Text.translatable("advancements.polyfactory.root.title"),
                        Text.translatable("advancements.polyfactory.root.description"),
                        id("textures/advancements/background.png"),
                        AdvancementFrame.TASK,
                        false,
                        false,
                        false
                )
                .criterion("any_item", InventoryChangedCriterion.Conditions.items(
                        ItemPredicate.Builder.create().tag(FactoryItemTags.ROOT_ADVANCEMENT)
                ))
                .build(exporter, "polyfactory:main/root");

        this.base(registryLookup, root, exporter);
        this.taters(registryLookup, root, exporter);
    }

    private void taters(RegistryWrapper.WrapperLookup registryLookup, AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
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

    private void base(RegistryWrapper.WrapperLookup registryLookup, AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("grinding/coal_dust")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(id("grinding/planks_saw_dust")))
                .criterion("use3", RecipeCraftedCriterion.Conditions.create(id("grinding/logs_saw_dust")))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(exporter, "polyfactory:main/base/grinder_dust");

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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("grinding/cobblestone_to_gravel")))
                .criterion("use1", RecipeCraftedCriterion.Conditions.create(id("grinding/stone_to_cobblestone")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(id("grinding/gravel_to_sand")))
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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("steel_ingot")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(id("steel_ingot_blasting")))
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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("mixing/cake")))
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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("mixing/artificial_dye")))
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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(Identifier.of("firework_rocket"), List.of(
                        ExtraItemPredicate.withStatic(ItemPredicate.Builder.create(), FactoryItemPredicates.CUSTOM_FIREWORK_COLOR))))
                .build(exporter, "polyfactory:main/base/mixer/firework");


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
                .criterion("use", RecipeCraftedCriterion.Conditions.create(id("press/steel_plate")))
                .criterion("use2", RecipeCraftedCriterion.Conditions.create(id("press/wooden_plate")))
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
                .criterion("craft", RecipeCraftedCriterion.Conditions.create(id("workbench")))
                .build(exporter, "polyfactory:main/base/workbench");

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
    }
}
