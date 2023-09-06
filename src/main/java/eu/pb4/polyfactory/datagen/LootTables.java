package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;

class LootTables extends FabricBlockLootTableProvider {
    protected LootTables(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        this.addDrop(FactoryBlocks.SPLITTER);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.FAN);
        this.addDrop(FactoryBlocks.GRINDER);
        this.addDrop(FactoryBlocks.PRESS, (block) -> this.dropsWithProperty(block, PressBlock.PART, PressBlock.Part.MAIN));
        this.addDrop(FactoryBlocks.MIXER, (block) -> this.dropsWithProperty(block, MixerBlock.PART, MixerBlock.Part.MAIN));
        this.addDrop(FactoryBlocks.HAND_CRANK);
        this.addDrop(FactoryBlocks.CONVEYOR);
        this.addDrop(FactoryBlocks.STICKY_CONVEYOR);
        this.addDrop(FactoryBlocks.ELECTRIC_MOTOR);
        this.addDrop(FactoryBlocks.FUNNEL);
        this.addDrop(FactoryBlocks.AXLE);
        this.addDrop(FactoryBlocks.GEARBOX);
        this.addDrop(FactoryBlocks.CONTAINER);
        this.addDrop(FactoryBlocks.NIXIE_TUBE);
        this.addDrop(FactoryBlocks.METAL_GRID);
        this.addDrop(FactoryBlocks.MINER);
        this.addDrop(FactoryBlocks.STEAM_ENGINE);
        this.addDrop(FactoryBlocks.ITEM_COUNTER);
        this.addDrop(FactoryBlocks.REDSTONE_INPUT);
        this.addDrop(FactoryBlocks.REDSTONE_OUTPUT);
        this.addDrop(FactoryBlocks.WINDMILL, FactoryItems.AXLE);


        {
            var builder = LootTable.builder();

            for (var property : CableBlock.FACING_PROPERTIES.values()) {
                builder.pool(
                        LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1))
                                .with(ItemEntry.builder(FactoryItems.CABLE))
                                .conditionally(SurvivesExplosionLootCondition.builder())
                                .conditionally(BlockStatePropertyLootCondition.builder(FactoryBlocks.CABLE)
                                        .properties(StatePredicate.Builder.create().exactMatch(property, true)))
                );
            }

            this.addDrop(FactoryBlocks.CABLE, builder);
        }
    }
}
