package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.block.fluids.transport.PipeInWallBlock;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.loottable.CopyCachedDataLootFunction;
import eu.pb4.polyfactory.loottable.CopyColorLootFunction;
import eu.pb4.polyfactory.loottable.CopyFluidsLootFunction;
import eu.pb4.polyfactory.loottable.CopyReadOnlyLootFunction;
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
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class LootTables extends FabricBlockLootTableProvider {

    protected LootTables(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        this.addDrop(FactoryBlocks.SPLITTER);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.PLANTER);
        this.addDrop(FactoryBlocks.FAN);
        this.addDrop(FactoryBlocks.EJECTOR);
        this.addDrop(FactoryBlocks.GRINDER);
        this.addDrop(FactoryBlocks.PLACER);
        this.addDrop(FactoryBlocks.STEEL_BUTTON);
        this.addTallMachineDrop(FactoryBlocks.PRESS);
        this.addTallMachineDrop(FactoryBlocks.MIXER);
        this.addDrop(FactoryBlocks.HAND_CRANK);
        this.addDrop(FactoryBlocks.CONVEYOR);
        this.addDrop(FactoryBlocks.STICKY_CONVEYOR);
        this.addDrop(FactoryBlocks.ELECTRIC_MOTOR);
        this.addDrop(FactoryBlocks.FUNNEL);
        this.addDrop(FactoryBlocks.STEEL_BLOCK);
        this.addDrop(FactoryBlocks.SLOT_AWARE_FUNNEL);
        this.addDrop(FactoryBlocks.AXLE);
        this.addDrop(FactoryBlocks.ITEM_PACKER);
        this.addDrop(FactoryBlocks.GEARBOX);
        this.addDrop(FactoryBlocks.CLUTCH);
        this.addDrop(FactoryBlocks.CONTAINER);
        this.addDrop(FactoryBlocks.TEXT_INPUT);
        this.addDrop(FactoryBlocks.NIXIE_TUBE);
        this.addDrop(FactoryBlocks.METAL_GRID);
        this.addDrop(FactoryBlocks.DATA_COMPARATOR);
        this.addDrop(FactoryBlocks.DATA_EXTRACTOR);
        this.addDrop(FactoryBlocks.PROGRAMMABLE_DATA_EXTRACTOR);
        this.addDrop(FactoryBlocks.SPEAKER);
        this.addDrop(FactoryBlocks.RECORD_PLAYER);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.STEAM_ENGINE);
        this.addDrop(FactoryBlocks.SMELTERY_CORE);
        this.addDrop(FactoryBlocks.PRIMITIVE_SMELTERY);
        this.addDrop(FactoryBlocks.CASTING_TABLE);
        this.addDrop(FactoryBlocks.FAUCED);
        this.addDrop(FactoryBlocks.HOLOGRAM_PROJECTOR);
        this.addDrop(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
        this.addDrop(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
        this.addDrop(FactoryBlocks.ARITHMETIC_OPERATOR);
        this.addDrop(FactoryBlocks.TACHOMETER);
        this.addDrop(FactoryBlocks.GATED_CABLE);
        this.addDrop(FactoryBlocks.STRESSOMETER);
        this.addDrop(FactoryBlocks.PIPE);
        this.addDrop(FactoryBlocks.FILTERED_PIPE);
        this.addDrop(FactoryBlocks.REDSTONE_VALVE_PIPE);
        this.addDrop(FactoryBlocks.PUMP);
        this.addDrop(FactoryBlocks.NOZZLE);
        this.addDrop(FactoryBlocks.DRAIN);
        this.addTallMachineDrop(FactoryBlocks.MECHANICAL_DRAIN);
        this.addTallMachineDrop(FactoryBlocks.MECHANICAL_SPOUT);
        this.addDrop(FactoryBlocks.FLUID_TANK);
        this.addDrop(FactoryBlocks.PORTABLE_FLUID_TANK, LootTable.builder().pool(LootPool.builder()
                .conditionally(SurvivesExplosionLootCondition.builder())
                .rolls(ConstantLootNumberProvider.create(1.0F))
                .with(ItemEntry.builder(FactoryBlocks.PORTABLE_FLUID_TANK)
                        .apply(() -> CopyFluidsLootFunction.INSTANCE)
                )));

        this.addDrop(FactoryBlocks.DATA_MEMORY, LootTable.builder().pool(LootPool.builder()
                .conditionally(SurvivesExplosionLootCondition.builder())
                .rolls(ConstantLootNumberProvider.create(1.0F))
                .with(ItemEntry.builder(FactoryItems.DATA_MEMORY)
                        .apply(() -> CopyCachedDataLootFunction.INSTANCE)
                        .apply(() -> CopyReadOnlyLootFunction.INSTANCE)
                )));

        this.addDrop(FactoryBlocks.INVERTED_REDSTONE_LAMP);
        this.addDrop(FactoryBlocks.ELECTRIC_GENERATOR);
        this.addDrop(FactoryBlocks.TINY_POTATO_SPRING);
        this.addDrop(FactoryBlocks.GOLDEN_TINY_POTATO_SPRING);
        this.addDrop(FactoryBlocks.CRAFTER);
        this.addDrop(FactoryBlocks.WORKBENCH);
        this.addDrop(FactoryBlocks.BLUEPRINT_WORKBENCH);
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

        this.addColored(FactoryBlocks.CABLE);
        this.addColored(FactoryBlocks.LAMP);
        this.addColored(FactoryBlocks.INVERTED_LAMP);
        this.addColored(FactoryBlocks.CAGED_LAMP);
        this.addColored(FactoryBlocks.INVERTED_CAGED_LAMP);
        this.addColored(FactoryBlocks.FIXTURE_LAMP);
        this.addColored(FactoryBlocks.INVERTED_FIXTURE_LAMP);
        FactoryBlocks.WALL_WITH_CABLE.values().forEach(this::addWallWithCable);
        FactoryBlocks.WALL_WITH_PIPE.values().forEach(this::addWallWithPipe);
    }

    private void addTallMachineDrop(TallItemMachineBlock block) {
        this.addDrop(block, (block2) -> this.dropsWithProperty(block2, MixerBlock.PART, MixerBlock.Part.MAIN));
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

    private void addWallWithCable(WallWithCableBlock block) {
        this.addDrop(block, LootTable.builder()
                .pool(LootPool.builder()
                        .with(ItemEntry.builder(FactoryItems.CABLE)
                                .apply(() -> CopyColorLootFunction.INSTANCE)
                        )
                )
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder())
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(block.getBacking())))
        );
    }

    private void addWallWithPipe(PipeInWallBlock block) {
        this.addDrop(block, LootTable.builder()
                .pool(LootPool.builder()
                        .with(ItemEntry.builder(FactoryItems.PIPE))
                )
                .pool(LootPool.builder()
                        .conditionally(SurvivesExplosionLootCondition.builder())
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(block.getBacking())))
        );
    }

    private void addColored(Block block) {
        addColored(block, (x) -> {
        });
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
