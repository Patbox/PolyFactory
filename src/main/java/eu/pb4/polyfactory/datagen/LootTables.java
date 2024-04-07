package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.loottable.CopyColorLootFunction;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;

import java.util.function.Consumer;

class LootTables extends FabricBlockLootTableProvider {
    protected LootTables(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        this.addDrop(FactoryBlocks.SPLITTER);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.PLANTER);
        this.addDrop(FactoryBlocks.FAN);
        this.addDrop(FactoryBlocks.GRINDER);
        this.addDrop(FactoryBlocks.PLACER);
        this.addDrop(FactoryBlocks.STEEL_BUTTON);
        this.addDrop(FactoryBlocks.PRESS, (block) -> this.dropsWithProperty(block, PressBlock.PART, PressBlock.Part.MAIN));
        this.addDrop(FactoryBlocks.MIXER, (block) -> this.dropsWithProperty(block, MixerBlock.PART, MixerBlock.Part.MAIN));
        this.addDrop(FactoryBlocks.HAND_CRANK);
        this.addDrop(FactoryBlocks.CONVEYOR);
        this.addDrop(FactoryBlocks.STICKY_CONVEYOR);
        this.addDrop(FactoryBlocks.ELECTRIC_MOTOR);
        this.addDrop(FactoryBlocks.FUNNEL);
        this.addDrop(FactoryBlocks.AXLE);
        this.addDrop(FactoryBlocks.GEARBOX);
        this.addDrop(FactoryBlocks.CLUTCH);
        this.addDrop(FactoryBlocks.CONTAINER);
        this.addDrop(FactoryBlocks.NIXIE_TUBE);
        this.addDrop(FactoryBlocks.METAL_GRID);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.STEAM_ENGINE);
        this.addDrop(FactoryBlocks.HOLOGRAM_PROJECTOR);
        this.addDrop(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
        this.addDrop(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
        this.addDrop(FactoryBlocks.TACHOMETER);
        this.addDrop(FactoryBlocks.STRESSOMETER);

        this.addDrop(FactoryBlocks.INVERTED_REDSTONE_LAMP);
        this.addDrop(FactoryBlocks.ELECTRIC_GENERATOR);
        this.addDrop(FactoryBlocks.TINY_POTATO_SPRING);
        this.addDrop(FactoryBlocks.WITHER_SKULL_GENERATOR);
        this.addDrop(FactoryBlocks.CRAFTER);
        this.addDrop(FactoryBlocks.WORKBENCH);
        this.addDrop(FactoryBlocks.WINDMILL, FactoryItems.AXLE);
        this.addDrop(FactoryBlocks.TURNTABLE);


        this.addOptionalCable(FactoryBlocks.NIXIE_TUBE_CONTROLLER);
        this.addOptionalCable(FactoryBlocks.ITEM_COUNTER);
        this.addOptionalCable(FactoryBlocks.ITEM_READER);
        this.addOptionalCable(FactoryBlocks.BLOCK_OBSERVER);
        this.addOptionalCable(FactoryBlocks.REDSTONE_INPUT);
        this.addOptionalCable(FactoryBlocks.REDSTONE_OUTPUT);

        this.addAxle(FactoryBlocks.AXLE_WITH_LARGE_GEAR, FactoryItems.LARGE_STEEL_GEAR);
        this.addAxle(FactoryBlocks.AXLE_WITH_GEAR, FactoryItems.STEEL_GEAR);

        this.addColored(FactoryBlocks.CABLE, (x) -> x.pool(LootPool.builder()
                .with(ItemEntry.builder(FactoryItems.FRAME))
                .conditionally(BlockStatePropertyLootCondition.builder(FactoryBlocks.CABLE)
                        .properties(StatePredicate.Builder.create().exactMatch(CableBlock.FRAMED, true)))));
        this.addColored(FactoryBlocks.LAMP);
        this.addColored(FactoryBlocks.INVERTED_LAMP);
        this.addColored(FactoryBlocks.CAGED_LAMP);
        this.addColored(FactoryBlocks.INVERTED_CAGED_LAMP);
    }

    private void addAxle(Block block, Item item) {
        this.addDrop(block, LootTable.builder()
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder())
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(FactoryBlocks.AXLE)))
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder())
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(item)))
        );
    }

    private void addOptionalCable(Block block) {
        this.addDrop(block, LootTable.builder()
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder()
                                .and(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create()
                                .exactMatch(GenericCabledDataBlock.HAS_CABLE, true))))
                        .with(ItemEntry.builder(FactoryItems.CABLE)
                                .apply(() -> CopyColorLootFunction.INSTANCE)
                        )
                )
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder())
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(block)))
        );
    }

    private void addColored(Block block) {
        addColored(block, (x) -> {});
    }
    private void addColored(Block block, Consumer<LootTable.Builder> consumer) {
        var x = LootTable.builder().pool(LootPool.builder()
                .conditionally(SurvivesExplosionLootCondition.builder())
                .rolls(ConstantLootNumberProvider.create(1.0F))
                .with(ItemEntry.builder(block)
                        .apply(() -> CopyColorLootFunction.INSTANCE)
                ));
        consumer.accept(x);

        this.addDrop(block, x);
    }
}
