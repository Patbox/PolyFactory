package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.loottable.CopyColorLootFunction;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.CopyNbtLootFunction;
import net.minecraft.loot.provider.nbt.ContextLootNbtProvider;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

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

        this.addDrop(FactoryBlocks.CABLE, LootTable.builder()
                .pool(this.addSurvivesExplosionCondition(FactoryBlocks.CABLE, LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(ItemEntry.builder(FactoryBlocks.CABLE)
                                .apply(() -> CopyColorLootFunction.INSTANCE)
                        ))));
    }
}
