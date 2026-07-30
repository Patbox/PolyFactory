package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlockIds;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.references.BlockItemIds;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;

import java.util.concurrent.CompletableFuture;

class BlockTagsProvider extends FabricTagsProvider.BlockTagsProvider {
    public BlockTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.builder(FactoryBlockTags.SPRAY_CAN_COLORABLE)
                .addOptionalTag(BlockTags.WOOL)
                .addOptionalTag(BlockTags.WOOL_CARPETS)
                .addOptionalTag(BlockTags.BEDS)
                .addOptionalTag(BlockTags.TERRACOTTA)
                .addOptionalTag(BlockItemTags.CONCRETE_POWDERS.block())
                .addOptionalTag(BlockTags.CANDLES)
                .addOptionalTag(BlockTags.CANDLE_CAKES)
                .addOptionalTag(ConventionalBlockTags.GLASS_BLOCKS)
                .addOptionalTag(ConventionalBlockTags.GLASS_PANES)
                .addOptionalTag(FactoryBlockTags.CONCRETE)
        ;

        this.tag(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlockIds.CONVEYOR)
                .add(FactoryBlockIds.STICKY_CONVEYOR);

        this.tag(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)
                .add(FactoryBlockIds.FUNNEL).add(FactoryBlockIds.SLOT_AWARE_FUNNEL).add(FactoryBlockIds.SPLITTER);

        this.tag(FactoryBlockTags.GRINDER_TOP_PLACEABLE)
                .add(FactoryBlockIds.HAND_CRANK).add(FactoryBlockIds.AXLE);

        this.tag(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .add(FactoryBlockIds.PRESS)
                .add(FactoryBlockIds.MIXER)
                .add(FactoryBlockIds.MECHANICAL_SPOUT)
                .add(FactoryBlockIds.MECHANICAL_DRAIN)
                .add(FactoryBlockIds.GRINDER)
                .add(FactoryBlockIds.FERMENTER)
                .add(BlockItemIds.HOPPER.block())
                .add(BlockItemIds.COMPOSTER.block())
        ;

        this.tag(FactoryBlockTags.SPLITTER_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYORS);

        this.tag(FactoryBlockTags.WIND_PASSTHROUGH)
                .addOptionalTag(BlockTags.TRAPDOORS)
                .addOptionalTag(BlockTags.FENCES)
                .addOptionalTag(BlockTags.FENCE_GATES)
                .addOptionalTag(BlockTags.BANNERS)
                .addOptionalTag(BlockTags.ALL_SIGNS)
                .add(FactoryBlockIds.NIXIE_TUBE)
                .add(FactoryBlockIds.HAND_CRANK)
                .add(FactoryBlockIds.METAL_GRID)
        ;

        this.tag(FactoryBlockTags.PIPE_CONNECTABLE)
                .addOptionalTag(BlockTags.CAULDRONS)
                .add(BlockItemIds.BEEHIVE.block())
        ;

        this.tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                .add(FactoryBlockIds.STEEL_BLOCK)
                ;

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addOptionalTag(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlockIds.FAN,
                        FactoryBlockIds.NIXIE_TUBE,
                        FactoryBlockIds.PRESS,
                        FactoryBlockIds.FUNNEL,
                        FactoryBlockIds.SLOT_AWARE_FUNNEL,
                        FactoryBlockIds.GRINDER,
                        FactoryBlockIds.MINER,
                        FactoryBlockIds.SPLITTER)
                .add(FactoryBlockIds.MIXER)
                .add(FactoryBlockIds.FERMENTER)
                .add(FactoryBlockIds.EJECTOR)
                .add(FactoryBlockIds.STEEL_BLOCK)
                .add(FactoryBlockIds.PLANTER)
                .add(FactoryBlockIds.PLACER)
                .add(FactoryBlockIds.STEAM_ENGINE)
                .add(FactoryBlockIds.PRIMITIVE_SMELTERY)
                .add(FactoryBlockIds.ITEM_OUTPUT_BUFFER)
                .add(FactoryBlockIds.SMELTERY)
                .add(FactoryBlockIds.SMELTERY_CORE)
                .add(FactoryBlockIds.CASTING_TABLE)
                .add(FactoryBlockIds.CASTING_CAULDRON)
                .add(FactoryBlockIds.FAUCET)
                .add(FactoryBlockIds.DEEP_STORAGE_CONTAINER)
                .add(FactoryBlockIds.ITEM_COUNTER)
                .add(FactoryBlockIds.TEXT_INPUT)
                .add(FactoryBlockIds.DIGITAL_CLOCK)
                .add(FactoryBlockIds.GATED_CABLE)
                .add(FactoryBlockIds.REDSTONE_INPUT)
                .add(FactoryBlockIds.REDSTONE_OUTPUT)
                .add(FactoryBlockIds.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlockIds.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlockIds.HOLOGRAM_PROJECTOR)
                .add(FactoryBlockIds.ARITHMETIC_OPERATOR)
                .add(FactoryBlockIds.DATA_COMPARATOR)
                .add(FactoryBlockIds.DATA_EXTRACTOR)
                .add(FactoryBlockIds.PROGRAMMABLE_DATA_EXTRACTOR)
                .add(FactoryBlockIds.METAL_GRID)
                .add(FactoryBlockIds.ITEM_READER)
                .add(FactoryBlockIds.SPEAKER)
                .add(FactoryBlockIds.RECORD_PLAYER)
                .add(FactoryBlockIds.BLOCK_OBSERVER)
                .add(FactoryBlockIds.NIXIE_TUBE_CONTROLLER)
                .add(FactoryBlockIds.GAUGE)
                .add(FactoryBlockIds.ELECTRIC_MOTOR)
                .add(FactoryBlockIds.ELECTRIC_GENERATOR)
                .add(FactoryBlockIds.DIESEL_ENGINE)
                .add(FactoryBlockIds.DATA_MEMORY)
                .add(FactoryBlockIds.TACHOMETER)
                .add(FactoryBlockIds.STRESSOMETER)
                .add(FactoryBlockIds.CABLE)
                .add(FactoryBlockIds.CRAFTER)
                .add(FactoryBlockIds.CLUTCH)
                .add(FactoryBlockIds.GEARSHIFT)
                .add(FactoryBlockIds.AXLE)
                .add(FactoryBlockIds.AXLE_WITH_GEAR)
                .add(FactoryBlockIds.CHAIN_DRIVE)
                .add(FactoryBlockIds.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlockIds.PIPE)
                .add(FactoryBlockIds.FILTERED_PIPE)
                .add(FactoryBlockIds.REDSTONE_VALVE_PIPE)
                .add(FactoryBlockIds.PUMP)
                .add(FactoryBlockIds.MECHANICAL_DRAIN)
                .add(FactoryBlockIds.MECHANICAL_SPOUT)
                .add(FactoryBlockIds.ITEM_PACKER)
                .add(FactoryBlockIds.DRAIN)
                .add(FactoryBlockIds.FLUID_TANK)
                .add(FactoryBlockIds.NOZZLE)
                .add(FactoryBlockIds.PORTABLE_FLUID_TANK)
                //.add(FactoryBlockIds.WALL_WITH_CABLE.values().toArray(new Block[0]))
        ;

        this.tag(FactoryBlockTags.UNCONNECTABLE)
                .add(FactoryBlockIds.PUMP)
                .add(FactoryBlockIds.PIPE)
                .add(FactoryBlockIds.NOZZLE)
                .add(FactoryBlockIds.FILTERED_PIPE)
                .add(FactoryBlockIds.REDSTONE_VALVE_PIPE)
                .add(FactoryBlockIds.PORTABLE_FLUID_TANK)
                .add(FactoryBlockIds.AXLE)
                .add(FactoryBlockIds.AXLE_WITH_GEAR)
                .add(FactoryBlockIds.TINY_POTATO_SPRING)
                .add(FactoryBlockIds.GOLDEN_TINY_POTATO_SPRING)
                .add(FactoryBlockIds.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlockIds.NIXIE_TUBE)
                .add(FactoryBlockIds.HOLOGRAM_PROJECTOR)
                .add(FactoryBlockIds.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlockIds.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlockIds.CAGED_LAMP)
                .add(FactoryBlockIds.INVERTED_CAGED_LAMP)
                .add(FactoryBlockIds.DIESEL_ENGINE)
                .add(FactoryBlockIds.CHEESE_WHEEL)

        ;

        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(FactoryBlockIds.WINDMILL)
                .add(FactoryBlockIds.AXLE)
                .add(FactoryBlockIds.AXLE_WITH_GEAR)
                .add(FactoryBlockIds.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlockIds.HAND_CRANK)
                .add(FactoryBlockIds.GEARBOX)
                .add(FactoryBlockIds.CHAIN_DRIVE)
                .add(FactoryBlockIds.WOODEN_CONTAINER)
                .add(FactoryBlockIds.FUNNEL)
                .add(FactoryBlockIds.WORKBENCH)
                .add(FactoryBlockIds.BLUEPRINT_WORKBENCH)
        ;

        this.builder(FactoryBlockTags.MINEABLE_WITH_DRILL)
                .addOptionalTag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addOptionalTag(BlockTags.MINEABLE_WITH_SHOVEL)
        ;

        //this.tag(BlockTags.WALLS)
        //        .add(FactoryBlockIds.WALL_WITH_CABLE.values().toArray(new Block[0]));
    }
}
