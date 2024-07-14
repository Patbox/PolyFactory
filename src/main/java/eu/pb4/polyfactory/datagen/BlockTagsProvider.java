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
        this.getOrCreateTagBuilder(FactoryBlockTags.STRIPPED_LOGS)
                .add(Blocks.STRIPPED_OAK_LOG)
                .add(Blocks.STRIPPED_BIRCH_LOG)
                .add(Blocks.STRIPPED_SPRUCE_LOG)
                .add(Blocks.STRIPPED_JUNGLE_LOG)
                .add(Blocks.STRIPPED_ACACIA_LOG)
                .add(Blocks.STRIPPED_DARK_OAK_LOG)
                .add(Blocks.STRIPPED_MANGROVE_LOG)
                .add(Blocks.STRIPPED_CHERRY_LOG)
                .add(Blocks.STRIPPED_CRIMSON_STEM)
                .add(Blocks.STRIPPED_WARPED_STEM)
        ;

        this.getOrCreateTagBuilder(FactoryBlockTags.SPRAY_CAN_COLORABLE)
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

        this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlocks.CONVEYOR)
                .add(FactoryBlocks.STICKY_CONVEYOR);

        this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)
                .add(FactoryBlocks.FUNNEL).add(FactoryBlocks.SPLITTER);

        this.getOrCreateTagBuilder(FactoryBlockTags.GRINDER_TOP_PLACEABLE)
                .add(FactoryBlocks.HAND_CRANK).add(FactoryBlocks.AXLE);

        this.getOrCreateTagBuilder(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .add(FactoryBlocks.PRESS)
                .add(FactoryBlocks.MIXER)
                .add(Blocks.HOPPER)
                .add(Blocks.COMPOSTER)
        ;

        this.getOrCreateTagBuilder(FactoryBlockTags.SPLITTER_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYOR_SIDE_OUTPUT)
                .addOptionalTag(FactoryBlockTags.CONVEYORS);

        this.getOrCreateTagBuilder(FactoryBlockTags.WIND_PASSTHROUGH)
                .addOptionalTag(BlockTags.TRAPDOORS)
                .addOptionalTag(BlockTags.FENCES)
                .addOptionalTag(BlockTags.FENCE_GATES)
                .addOptionalTag(BlockTags.BANNERS)
                .addOptionalTag(BlockTags.ALL_SIGNS)
                .add(FactoryBlocks.NIXIE_TUBE)
                .add(FactoryBlocks.HAND_CRANK)
                .add(FactoryBlocks.METAL_GRID)
        ;

        this.getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .addOptionalTag(FactoryBlockTags.CONVEYORS)
                .add(FactoryBlocks.FAN,
                        FactoryBlocks.NIXIE_TUBE,
                        FactoryBlocks.PRESS,
                        FactoryBlocks.FUNNEL,
                        FactoryBlocks.GRINDER,
                        FactoryBlocks.MINER,
                        FactoryBlocks.SPLITTER)
                .add(FactoryBlocks.MIXER)
                .add(FactoryBlocks.PLANTER)
                .add(FactoryBlocks.PLACER)
                .add(FactoryBlocks.STEAM_ENGINE)
                .add(FactoryBlocks.ITEM_COUNTER)
                .add(FactoryBlocks.REDSTONE_INPUT)
                .add(FactoryBlocks.REDSTONE_OUTPUT)
                .add(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlocks.HOLOGRAM_PROJECTOR)
                .add(FactoryBlocks.ARITHMETIC_OPERATOR)
                .add(FactoryBlocks.METAL_GRID)
                .add(FactoryBlocks.ITEM_READER)
                .add(FactoryBlocks.BLOCK_OBSERVER)
                .add(FactoryBlocks.NIXIE_TUBE_CONTROLLER)
                .add(FactoryBlocks.ELECTRIC_MOTOR)
                .add(FactoryBlocks.ELECTRIC_GENERATOR)
                .add(FactoryBlocks.DATA_MEMORY)
                .add(FactoryBlocks.TACHOMETER)
                .add(FactoryBlocks.STRESSOMETER)
                .add(FactoryBlocks.CABLE)
                .add(FactoryBlocks.CRAFTER)
                .add(FactoryBlocks.CLUTCH)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.PIPE)
                .add(FactoryBlocks.PUMP)
                .add(FactoryBlocks.WALL_WITH_CABLE.values().toArray(new Block[0]))
        ;

        this.getOrCreateTagBuilder(FactoryBlockTags.UNCONNECTABLE)
                .add(FactoryBlocks.PUMP)
                .add(FactoryBlocks.PIPE)
                .add(FactoryBlocks.AXLE)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.NIXIE_TUBE)
                .add(FactoryBlocks.HOLOGRAM_PROJECTOR)
                .add(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER)
                .add(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                .add(FactoryBlocks.CAGED_LAMP)
                .add(FactoryBlocks.INVERTED_CAGED_LAMP)
        ;

        this.getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(FactoryBlocks.WINDMILL)
                .add(FactoryBlocks.AXLE)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
                .add(FactoryBlocks.HAND_CRANK)
                .add(FactoryBlocks.GEARBOX)
                .add(FactoryBlocks.CONTAINER)
                .add(FactoryBlocks.FUNNEL)
        ;

        this.getOrCreateTagBuilder(BlockTags.WALLS)
                .add(FactoryBlocks.WALL_WITH_CABLE.values().toArray(new Block[0]));

        var conc = this.getOrCreateTagBuilder(FactoryBlockTags.CONCRETE);

        for (var block : Registries.BLOCK) {
            var id = Registries.BLOCK.getId(block);

            if (id.getPath().endsWith("_concrete")) {
                conc.add(block);
            }
        }
    }
}
