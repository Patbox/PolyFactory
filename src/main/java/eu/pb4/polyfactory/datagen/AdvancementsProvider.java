package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactoryEntityTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.ChangedDimensionCriterion;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.ItemCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;
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
                .build(exporter, "main/root");

        var handPowered = Advancement.Builder.create()
                .parent(root)
                .display(
                        FactoryItems.HAND_CRANK,
                        Text.translatable("advancements.polyfactory.hand_crank.title"),
                        Text.translatable("advancements.polyfactory.hand_crank.description"),
                        null,
                        AdvancementFrame.TASK,
                        false,
                        false,
                        false
                )
                .criterion("use", null)
                .build(exporter, "main/hand_crank");
    }
}
