package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.RecipeCraftedCriterion;
import net.minecraft.item.Items;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

import java.util.function.Consumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

class AdvancementsProvider extends FabricAdvancementProvider {

    protected AdvancementsProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<AdvancementEntry> exporter) {
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

        this.rotationBase(root, exporter);
        this.tools(root, exporter);
        this.machines(root, exporter);
        this.data(root, exporter);
        this.taters(root, exporter);
    }

    private void taters(AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
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

    private void data(AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var cable = Advancement.Builder.create()
                .parent(root)
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
                .build(exporter, "polyfactory:main/data/cable");

         /*var redstone = Advancement.Builder.create()
                .parent(cable)
                .display(
                        FactoryItems.REDSTONE_OUTPUT,
                        Text.translatable("advancements.polyfactory.redstone.title"),
                        Text.translatable("advancements.polyfactory.redstone.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("use", TriggerCriterion.of(FactoryTriggers.REDSTONE_IN_OUT))
                .build(exporter, "polyfactory:main/data/redstone");*/

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
                .build(exporter, "polyfactory:main/data/item_reader");
    }

    private void machines(AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var grinder = Advancement.Builder.create()
                .parent(root)
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
                .build(exporter, "polyfactory:main/machines/grinder_dust");

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
                .build(exporter, "polyfactory:main/machines/steel_ingot");

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
                .build(exporter, "polyfactory:main/machines/gravel");

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
                .build(exporter, "polyfactory:main/machine/mixer");

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
                .build(exporter, "polyfactory:main/machine/cake");

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
                .build(exporter, "polyfactory:main/machine/artificial_dye");


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
                .build(exporter, "polyfactory:main/machine/press");

        var container = Advancement.Builder.create()
                .parent(press)
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
                .build(exporter, "polyfactory:main/machines/container");

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
                .build(exporter, "polyfactory:main/machines/fan");

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
                .build(exporter, "polyfactory:main/machines/fan_sky");

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
                .build(exporter, "polyfactory:main/machines/miner");

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
                .build(exporter, "polyfactory:main/machines/planter");
    }

    private void tools(AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var wrench = Advancement.Builder.create()
                .parent(root)
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
                .build(exporter, "polyfactory:main/tools/wrench");
    }

    private void rotationBase(AdvancementEntry root, Consumer<AdvancementEntry> exporter) {
        var handPowered = Advancement.Builder.create()
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
                .build(exporter, "polyfactory:main/rotation/hand_crank");

        var windmill = Advancement.Builder.create()
                .parent(handPowered)
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
                .build(exporter, "polyfactory:main/rotation/windmill");


        var steamEngine = Advancement.Builder.create()
                .parent(windmill)
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
                .build(exporter, "polyfactory:main/rotation/steam_engine");
    }
}
