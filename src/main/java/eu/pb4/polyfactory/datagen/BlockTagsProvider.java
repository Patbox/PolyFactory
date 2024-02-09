package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Blocks;
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
                .add(FactoryBlocks.METAL_GRID)
                .add(FactoryBlocks.ITEM_READER)
                .add(FactoryBlocks.BLOCK_OBSERVER)
                .add(FactoryBlocks.NIXIE_TUBE_CONTROLLER)
                .add(FactoryBlocks.ELECTRIC_MOTOR)
                .add(FactoryBlocks.ELECTRIC_GENERATOR)
                .add(FactoryBlocks.CRAFTER)
                .add(FactoryBlocks.CLUTCH)
                .add(FactoryBlocks.WITHER_SKULL_GENERATOR)
                .add(FactoryBlocks.AXLE_WITH_GEAR)
                .add(FactoryBlocks.AXLE_WITH_LARGE_GEAR)
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
    }
}
