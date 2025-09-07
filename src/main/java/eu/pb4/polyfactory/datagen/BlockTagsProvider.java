package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

class BlockTagsProvider extends FabricTagProvider.BlockTagProvider {
    public BlockTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        this.builder(FactoryBlockTags.SPRAY_CAN_COLORABLE)
                .addOptionalTag(BlockTags.WOOL)
                .addOptionalTag(BlockTags.WOOL_CARPETS)
                .addOptionalTag(BlockTags.BEDS)
                .addOptionalTag(BlockTags.TERRACOTTA)
                .addOptionalTag(BlockTags.CONCRETE_POWDER)
                .addOptionalTag(BlockTags.CANDLES)
                .addOptionalTag(BlockTags.CANDLE_CAKES)
                .addOptionalTag(ConventionalBlockTags.GLASS_BLOCKS)
                .addOptionalTag(ConventionalBlockTags.GLASS_PANES)
                .addOptionalTag(FactoryBlockTags.CONCRETE)
        ;

        this.valueLookupBuilder(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlocks.CONVEYOR)
                .add(FactoryBlocks.STICKY_CONVEYOR);

        this.valueLookupBuilder(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)
                .add(FactoryBlocks.FUNNEL).add(FactoryBlocks.SLOT_AWARE_FUNNEL).add(FactoryBlocks.SPLITTER);

        this.valueLookupBuilder(FactoryBlockTags.GRINDER_TOP_PLACEABLE)
                .add(FactoryBlocks.HAND_CRANK).add(FactoryBlocks.AXLE);

        this.valueLookupBuilder(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .add(FactoryBlocks.PRESS)
                .add(FactoryBlocks.MIXER)
                .add(FactoryBlocks.MECHANICAL_SPOUT)
                .add(FactoryBlocks.MECHANICAL_DRAIN)
                .add(FactoryBlocks.GRINDER)
                .add(Blocks.HOPPER)
                .add(Blocks.COMPOSTER)
        ;

        this.valueLookupBuilder(FactoryBlockTags.SPLITTER_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYORS);

        this.valueLookupBuilder(FactoryBlockTags.WIND_PASSTHROUGH)
                .addOptionalTag(BlockTags.TRAPDOORS)
                .addOptionalTag(BlockTags.FENCES)
                .addOptionalTag(BlockTags.FENCE_GATES)
                .addOptionalTag(BlockTags.BANNERS)
                .addOptionalTag(BlockTags.ALL_SIGNS)
                .add(FactoryBlocks.NIXIE_TUBE)
                .add(FactoryBlocks.HAND_CRANK)
                .add(FactoryBlocks.METAL_GRID)
        ;

        this.valueLookupBuilder(FactoryBlockTags.PIPE_CONNECTABLE)
                .addOptionalTag(BlockTags.CAULDRONS)
                .add(Blocks.BEEHIVE)
        ;

        this.valueLookupBuilder(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                .add(FactoryBlocks.STEEL_BLOCK)
                ;

        this.valueLookupBuilder(BlockTags.PICKAXE_MINEABLE)
                .addOptionalTag(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlocks.FAN,
                        FactoryBlocks.NIXIE_TUBE,
                        FactoryBlocks.PRESS,
                        FactoryBlocks.FUNNEL,
                        FactoryBlocks.SLOT_AWARE_FUNNEL,
                        FactoryBlocks.GRINDER,
                        FactoryBlocks.MINER,
                        FactoryBlocks.SPLITTER)
                .add(FactoryBlocks.MIXER)
                .add(FactoryBlocks.EJECTOR)
                .add(FactoryBlocks.STEEL_BLOCK)
                .add(FactoryBlocks.PLANTER)
                .add(FactoryBlocks.PLACER)
                .add(FactoryBlocks.STEAM_ENGINE)
                .add(FactoryBlocks.PRIMITIVE_SMELTERY)
                .add(FactoryBlocks.SMELTERY)
                .add(FactoryBlocks.SMELTERY_CORE)
                .add(FactoryBlocks.CASTING_TABLE)
                .add(FactoryBlocks.CASTING_CAULDRON)
                .add(FactoryBlocks.FAUCED)
                .add(FactoryBlocks.ITEM_COUNTER)
                .add(FactoryBlocks.TEXT_INPUT)
                .add(FactoryBlocks.GATED_CABLE)
                .add(FactoryBlocks.REDSTONE_INPUT)
                .add(FactoryBlocks.REDSTONE_OUTPUT)
                .add(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlocks.HOLOGRAM_PROJECTOR)
                .add(FactoryBlocks.ARITHMETIC_OPERATOR)
                .add(FactoryBlocks.DATA_COMPARATOR)
                .add(FactoryBlocks.DATA_EXTRACTOR)
                .add(FactoryBlocks.PROGRAMMABLE_DATA_EXTRACTOR)
                .add(FactoryBlocks.METAL_GRID)
                .add(FactoryBlocks.ITEM_READER)
                .add(FactoryBlocks.SPEAKER)
                .add(FactoryBlocks.RECORD_PLAYER)
                .add(FactoryBlocks.BLOCK_OBSERVER)
                .add(FactoryBlocks.NIXIE_TUBE_CONTROLLER)
                .add(FactoryBlocks.GAUGE)
                .add(FactoryBlocks.ELECTRIC_MOTOR)
                .add(FactoryBlocks.ELECTRIC_GENERATOR)
                .add(FactoryBlocks.DATA_MEMORY)
                .add(FactoryBlocks.TACHOMETER)
                .add(FactoryBlocks.STRESSOMETER)
                .add(FactoryBlocks.CABLE)
                .add(FactoryBlocks.CRAFTER)
                .add(FactoryBlocks.CLUTCH)
                .add(FactoryBlocks.AXLE)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.CHAIN_DRIVE)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.PIPE)
                .add(FactoryBlocks.FILTERED_PIPE)
                .add(FactoryBlocks.REDSTONE_VALVE_PIPE)
                .add(FactoryBlocks.PUMP)
                .add(FactoryBlocks.MECHANICAL_DRAIN)
                .add(FactoryBlocks.MECHANICAL_SPOUT)
                .add(FactoryBlocks.ITEM_PACKER)
                .add(FactoryBlocks.DRAIN)
                .add(FactoryBlocks.FLUID_TANK)
                .add(FactoryBlocks.NOZZLE)
                .add(FactoryBlocks.PORTABLE_FLUID_TANK)
                //.add(FactoryBlocks.WALL_WITH_CABLE.values().toArray(new Block[0]))
        ;

        this.valueLookupBuilder(FactoryBlockTags.UNCONNECTABLE)
                .add(FactoryBlocks.PUMP)
                .add(FactoryBlocks.PIPE)
                .add(FactoryBlocks.NOZZLE)
                .add(FactoryBlocks.FILTERED_PIPE)
                .add(FactoryBlocks.REDSTONE_VALVE_PIPE)
                .add(FactoryBlocks.PORTABLE_FLUID_TANK)
                .add(FactoryBlocks.AXLE)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.TINY_POTATO_SPRING)
                .add(FactoryBlocks.GOLDEN_TINY_POTATO_SPRING)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.NIXIE_TUBE)
                .add(FactoryBlocks.HOLOGRAM_PROJECTOR)
                .add(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlocks.CAGED_LAMP)
                .add(FactoryBlocks.INVERTED_CAGED_LAMP)

        ;

        this.valueLookupBuilder(BlockTags.AXE_MINEABLE)
                .add(FactoryBlocks.WINDMILL)
                .add(FactoryBlocks.AXLE)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.HAND_CRANK)
                .add(FactoryBlocks.GEARBOX)
                .add(FactoryBlocks.CHAIN_DRIVE)
                .add(FactoryBlocks.CONTAINER)
                .add(FactoryBlocks.FUNNEL)
                .add(FactoryBlocks.WORKBENCH)
                .add(FactoryBlocks.BLUEPRINT_WORKBENCH)
        ;

        //this.valueLookupBuilder(BlockTags.WALLS)
        //        .add(FactoryBlocks.WALL_WITH_CABLE.values().toArray(new Block[0]));

        var conc = this.valueLookupBuilder(FactoryBlockTags.CONCRETE);

        for (var block : Registries.BLOCK) {
            var id = Registries.BLOCK.getId(block);

            if (id.getPath().endsWith("_concrete")) {
                conc.add(block);
            }
        }
    }
}
