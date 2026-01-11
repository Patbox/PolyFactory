package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
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
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class LootTables extends FabricBlockLootTableProvider {

    protected LootTables(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        this.dropSelf(FactoryBlocks.SPLITTER);
        this.dropSelf(FactoryBlocks.MINER);
        this.dropSelf(FactoryBlocks.PLANTER);
        this.dropSelf(FactoryBlocks.FAN);
        this.dropSelf(FactoryBlocks.EJECTOR);
        this.dropSelf(FactoryBlocks.GRINDER);
        this.dropSelf(FactoryBlocks.PLACER);
        this.dropSelf(FactoryBlocks.STEEL_BUTTON);
        this.addTallMachineDrop(FactoryBlocks.PRESS);
        this.addTallMachineDrop(FactoryBlocks.MIXER);
        this.dropSelf(FactoryBlocks.HAND_CRANK);
        this.dropSelf(FactoryBlocks.CONVEYOR);
        this.dropSelf(FactoryBlocks.STICKY_CONVEYOR);
        this.dropSelf(FactoryBlocks.ELECTRIC_MOTOR);
        this.dropSelf(FactoryBlocks.FUNNEL);
        this.dropSelf(FactoryBlocks.STEEL_BLOCK);
        this.dropSelf(FactoryBlocks.SLOT_AWARE_FUNNEL);
        this.dropSelf(FactoryBlocks.AXLE);
        this.dropSelf(FactoryBlocks.CHAIN_DRIVE);
        this.dropSelf(FactoryBlocks.ITEM_PACKER);
        this.dropSelf(FactoryBlocks.DIGITAL_CLOCK);
        this.dropSelf(FactoryBlocks.GEARBOX);
        this.dropSelf(FactoryBlocks.CLUTCH);
        this.dropSelf(FactoryBlocks.GEARSHIFT);
        this.dropSelf(FactoryBlocks.CONTAINER);
        this.dropSelf(FactoryBlocks.TEXT_INPUT);
        this.dropSelf(FactoryBlocks.NIXIE_TUBE);
        this.dropSelf(FactoryBlocks.METAL_GRID);
        this.dropSelf(FactoryBlocks.DATA_COMPARATOR);
        this.dropSelf(FactoryBlocks.DATA_EXTRACTOR);
        this.dropSelf(FactoryBlocks.PROGRAMMABLE_DATA_EXTRACTOR);
        this.dropSelf(FactoryBlocks.SPEAKER);
        this.dropSelf(FactoryBlocks.RECORD_PLAYER);
        this.dropSelf(FactoryBlocks.MINER);
        this.dropSelf(FactoryBlocks.STEAM_ENGINE);
        this.dropSelf(FactoryBlocks.SMELTERY_CORE);
        this.dropSelf(FactoryBlocks.PRIMITIVE_SMELTERY);
        this.dropSelf(FactoryBlocks.CASTING_TABLE);
        this.dropSelf(FactoryBlocks.FAUCED);
        this.dropSelf(FactoryBlocks.HOLOGRAM_PROJECTOR);
        this.dropSelf(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
        this.dropSelf(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
        this.dropSelf(FactoryBlocks.ARITHMETIC_OPERATOR);
        this.dropSelf(FactoryBlocks.TACHOMETER);
        this.dropSelf(FactoryBlocks.GAUGE);
        this.dropSelf(FactoryBlocks.GATED_CABLE);
        this.dropSelf(FactoryBlocks.STRESSOMETER);
        this.dropSelf(FactoryBlocks.PIPE);
        this.dropSelf(FactoryBlocks.FILTERED_PIPE);
        this.dropSelf(FactoryBlocks.REDSTONE_VALVE_PIPE);
        this.dropSelf(FactoryBlocks.PUMP);
        this.dropSelf(FactoryBlocks.NOZZLE);
        this.dropSelf(FactoryBlocks.DRAIN);
        this.addTallMachineDrop(FactoryBlocks.MECHANICAL_DRAIN);
        this.addTallMachineDrop(FactoryBlocks.MECHANICAL_SPOUT);
        this.dropSelf(FactoryBlocks.FLUID_TANK);
        this.add(FactoryBlocks.PORTABLE_FLUID_TANK, LootTable.lootTable().withPool(LootPool.lootPool()
                .when(ExplosionCondition.survivesExplosion())
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(FactoryBlocks.PORTABLE_FLUID_TANK)
                        .apply(() -> CopyFluidsLootFunction.INSTANCE)
                )));

        this.add(FactoryBlocks.DATA_MEMORY, LootTable.lootTable().withPool(LootPool.lootPool()
                .when(ExplosionCondition.survivesExplosion())
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(FactoryItems.DATA_MEMORY)
                        .apply(() -> CopyCachedDataLootFunction.INSTANCE)
                        .apply(() -> CopyReadOnlyLootFunction.INSTANCE)
                )));

        this.dropSelf(FactoryBlocks.INVERTED_REDSTONE_LAMP);
        this.dropSelf(FactoryBlocks.ELECTRIC_GENERATOR);
        this.dropSelf(FactoryBlocks.TINY_POTATO_SPRING);
        this.dropSelf(FactoryBlocks.GOLDEN_TINY_POTATO_SPRING);
        this.dropSelf(FactoryBlocks.CRAFTER);
        this.dropSelf(FactoryBlocks.WORKBENCH);
        this.dropSelf(FactoryBlocks.BLUEPRINT_WORKBENCH);
        this.dropSelf(FactoryBlocks.MOLDMAKING_TABLE);
        this.dropOther(FactoryBlocks.WINDMILL, FactoryItems.AXLE);
        this.dropSelf(FactoryBlocks.TURNTABLE);


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
        //FactoryBlocks.WALL_WITH_CABLE.values().forEach(this::addWallWithCable);
        //FactoryBlocks.WALL_WITH_PIPE.values().forEach(this::addWallWithPipe);
    }

    private void addTallMachineDrop(TallItemMachineBlock block) {
        this.add(block, (block2) -> this.createSinglePropConditionTable(block2, MixerBlock.PART, MixerBlock.Part.MAIN));
    }

    private void addAxle(Block block, Item item) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(FactoryBlocks.AXLE)))
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(item)))
        );
    }

    private void addOptionalCable(Block block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion()
                                .and(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(DirectionalCabledDataBlock.HAS_CABLE, true))))
                        .add(LootItem.lootTableItem(FactoryItems.CABLE)
                                .apply(() -> CopyColorLootFunction.INSTANCE)
                        )
                )
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block)))
        );
    }

    private void addWallWithCable(WallWithCableBlock block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(FactoryItems.CABLE)
                                .apply(() -> CopyColorLootFunction.INSTANCE)
                        )
                )
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block.backing())))
        );
    }

    private void addWallWithPipe(PipeInWallBlock block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(FactoryItems.PIPE))
                )
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block.backing())))
        );
    }

    private void addColored(Block block) {
        addColored(block, (x) -> {
        });
    }

    private void addColored(Block block, Consumer<LootTable.Builder> consumer) {
        var x = LootTable.lootTable().withPool(LootPool.lootPool()
                .when(ExplosionCondition.survivesExplosion())
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block)
                        .apply(() -> CopyColorLootFunction.INSTANCE)
                ));
        consumer.accept(x);

        this.add(block, x);
    }
}
