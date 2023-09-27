package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.RecipeCraftedCriterion;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.text.Text;

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
