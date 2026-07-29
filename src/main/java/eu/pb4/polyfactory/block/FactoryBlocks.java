package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlock;
import eu.pb4.polyfactory.block.creative.CreativeDrainBlock;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.GatedCableBlock;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.data.creative.TpsProviderBlock;
import eu.pb4.polyfactory.block.data.io.*;
import eu.pb4.polyfactory.block.data.output.*;
import eu.pb4.polyfactory.block.data.providers.*;
import eu.pb4.polyfactory.block.electric.ElectricGeneratorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.fluids.*;
import eu.pb4.polyfactory.block.fluids.smeltery.*;
import eu.pb4.polyfactory.block.fluids.transport.*;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SlotAwareFunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlacerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.*;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlock;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.other.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryBlocks {
    private static final List<Block> BLOCKS = new ArrayList<>();
    public static final ConveyorBlock CONVEYOR = register(FactoryBlockIds.CONVEYOR, settings -> new ConveyorBlock(settings.destroyTime(3).noOcclusion()));
    public static final ConveyorBlock STICKY_CONVEYOR = register(FactoryBlockIds.STICKY_CONVEYOR, settings -> new ConveyorBlock(settings.destroyTime(3).noOcclusion()));
    public static final FunnelBlock FUNNEL = register(FactoryBlockIds.FUNNEL, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_TRAPDOOR).noOcclusion(), FunnelBlock::new);
    public static final SlotAwareFunnelBlock SLOT_AWARE_FUNNEL = register(FactoryBlockIds.SLOT_AWARE_FUNNEL, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR).noOcclusion(), SlotAwareFunnelBlock::new);
    public static final SplitterBlock SPLITTER = register(FactoryBlockIds.SPLITTER, settings -> new SplitterBlock(settings.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.3F).noOcclusion().sound(SoundType.IRON)));
    public static final FanBlock FAN = register(FactoryBlockIds.FAN, settings -> new FanBlock(settings.noOcclusion().destroyTime(3).sound(SoundType.IRON).requiresCorrectToolForDrops()));
    public static final EjectorBlock EJECTOR = register(FactoryBlockIds.EJECTOR, BlockBehaviour.Properties.ofFullCopy(FAN), EjectorBlock::new);
    public static final SelectivePassthroughBlock METAL_GRID = register(FactoryBlockIds.METAL_GRID, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), settings -> new SelectivePassthroughBlock(settings.strength(4.0F, 3.0F).noOcclusion()));
    public static final HandCrankBlock HAND_CRANK = register(FactoryBlockIds.HAND_CRANK, settings -> new HandCrankBlock(settings.destroyTime(1).noOcclusion()));
    public static final SteamEngineBlock STEAM_ENGINE = register(FactoryBlockIds.STEAM_ENGINE, BlockBehaviour.Properties.ofFullCopy(SPLITTER), settings -> new SteamEngineBlock(settings.strength(4F).noOcclusion()));
    public static final GrinderBlock GRINDER = register(FactoryBlockIds.GRINDER, BlockBehaviour.Properties.ofFullCopy(SPLITTER).sound(SoundType.WOOD), GrinderBlock::new);
    public static final PressBlock PRESS = register(FactoryBlockIds.PRESS, BlockBehaviour.Properties.ofFullCopy(SPLITTER), PressBlock::new);
    public static final MixerBlock MIXER = register(FactoryBlockIds.MIXER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), MixerBlock::new);
    public static final FermenterBlock FERMENTER = register(FactoryBlockIds.FERMENTER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), FermenterBlock::new);
    public static final MCrafterBlock CRAFTER = register(FactoryBlockIds.CRAFTER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), MCrafterBlock::new);
    public static final MinerBlock MINER = register(FactoryBlockIds.MINER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), MinerBlock::new);
    public static final PlacerBlock PLACER = register(FactoryBlockIds.PLACER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), PlacerBlock::new);
    public static final PlanterBlock PLANTER = register(FactoryBlockIds.PLANTER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), PlanterBlock::new);
    public static final AxleBlock AXLE = register(FactoryBlockIds.AXLE, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new AxleBlock(settings.strength(2.5F).noOcclusion().isSuffocating(Blocks::never)));
    public static final ChainDriveBlock CHAIN_DRIVE = register(FactoryBlockIds.CHAIN_DRIVE, BlockBehaviour.Properties.ofFullCopy(AXLE), ChainDriveBlock::new);
    public static final AxleWithGearBlock AXLE_WITH_GEAR = register(FactoryBlockIds.AXLE_WITH_GEAR, BlockBehaviour.Properties.ofFullCopy(AXLE).sound(SoundType.IRON), AxleWithGearBlock::new);
    public static final AxleWithLargeGearBlock AXLE_WITH_LARGE_GEAR = register(FactoryBlockIds.AXLE_WITH_LARGE_GEAR, BlockBehaviour.Properties.ofFullCopy(AXLE_WITH_GEAR), AxleWithLargeGearBlock::new);
    public static final TurntableBlock TURNTABLE = register(FactoryBlockIds.TURNTABLE, BlockBehaviour.Properties.ofFullCopy(AXLE), TurntableBlock::new);
    public static final GearboxBlock GEARBOX = register(FactoryBlockIds.GEARBOX, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new GearboxBlock(settings.strength(2.5F).noOcclusion()));
    public static final ClutchBlock CLUTCH = register(FactoryBlockIds.CLUTCH, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new ClutchBlock(settings.strength(2.5F).noOcclusion()));
    public static final GearshiftBlock GEARSHIFT = register(FactoryBlockIds.GEARSHIFT, BlockBehaviour.Properties.ofFullCopy(CLUTCH), GearshiftBlock::new);
    public static final WindmillBlock WINDMILL = register(FactoryBlockIds.WINDMILL, BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new WindmillBlock(settings.strength(2.5F).noOcclusion()));
    public static final ContainerBlock WOODEN_CONTAINER = register(FactoryBlockIds.WOODEN_CONTAINER, BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST), settings -> new ContainerBlock(9 * 5, settings.noOcclusion()));
    public static final DeepStorageContainerBlock DEEP_STORAGE_CONTAINER = register(FactoryBlockIds.DEEP_STORAGE_CONTAINER, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), DeepStorageContainerBlock::new);
    public static final ItemOutputBufferBlock ITEM_OUTPUT_BUFFER = register(FactoryBlockIds.ITEM_OUTPUT_BUFFER, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion(), ItemOutputBufferBlock::new);
    public static final ItemPackerBlock ITEM_PACKER = register(FactoryBlockIds.ITEM_PACKER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), ItemPackerBlock::new);
    public static final CableBlock CABLE = register(FactoryBlockIds.CABLE, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).sound(SoundType.WOOL), settings -> new CableBlock(settings.instabreak().noOcclusion()));
    public static final GatedCableBlock GATED_CABLE = register(FactoryBlockIds.GATED_CABLE, BlockBehaviour.Properties.ofFullCopy(SPLITTER).strength(2.2F).sound(SoundType.STONE), GatedCableBlock::new);
    public static final Map<Block, WallWithCableBlock> WALL_WITH_CABLE = WallWithCableBlock.MAP;
    public static final DirectionalCabledDataProviderBlock ITEM_COUNTER = register(FactoryBlockIds.ITEM_COUNTER, BlockBehaviour.Properties.ofFullCopy(SPLITTER), DirectionalCabledDataProviderBlock::new);

    public static final RedstoneInputBlock REDSTONE_INPUT = register(FactoryBlockIds.REDSTONE_INPUT, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RedstoneInputBlock::new);
    public static final RedstoneOutputBlock REDSTONE_OUTPUT = register(FactoryBlockIds.REDSTONE_OUTPUT, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RedstoneOutputBlock::new);
    public static final SpeakerBlock SPEAKER = register(FactoryBlockIds.SPEAKER,BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), SpeakerBlock::new);
    public static final RecordPlayerBlock RECORD_PLAYER = register(FactoryBlockIds.RECORD_PLAYER,BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RecordPlayerBlock::new);
    public static final ItemReaderBlock ITEM_READER = register(FactoryBlockIds.ITEM_READER, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ItemReaderBlock::new);
    public static final BlockObserverBlock BLOCK_OBSERVER = register(FactoryBlockIds.BLOCK_OBSERVER, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), BlockObserverBlock::new);
    public static final TextInputBlock TEXT_INPUT = register(FactoryBlockIds.TEXT_INPUT, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), TextInputBlock::new);
    public static final DigitalClockBlock DIGITAL_CLOCK = register(FactoryBlockIds.DIGITAL_CLOCK, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DigitalClockBlock::new);
    public static final ArithmeticOperatorBlock ARITHMETIC_OPERATOR = register(FactoryBlockIds.ARITHMETIC_OPERATOR,
            BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ArithmeticOperatorBlock::new);

    public static final DataComparatorBlock DATA_COMPARATOR = register(FactoryBlockIds.DATA_COMPARATOR, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataComparatorBlock::new);
    public static final DataExtractorBlock DATA_EXTRACTOR = register(FactoryBlockIds.DATA_EXTRACTOR, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataExtractorBlock::new);
    public static final ProgrammableDataExtractorBlock PROGRAMMABLE_DATA_EXTRACTOR = register(FactoryBlockIds.PROGRAMMABLE_DATA_EXTRACTOR, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ProgrammableDataExtractorBlock::new);
    public static final DataMemoryBlock DATA_MEMORY = register(FactoryBlockIds.DATA_MEMORY, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataMemoryBlock::new);

    public static final GaugeBlock GAUGE = register(FactoryBlockIds.GAUGE, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR), GaugeBlock::new);
    public static final HologramProjectorBlock HOLOGRAM_PROJECTOR = register(FactoryBlockIds.HOLOGRAM_PROJECTOR, BlockBehaviour.Properties.ofFullCopy(SPLITTER), HologramProjectorBlock::new);
    public static final NixieTubeBlock NIXIE_TUBE = register(FactoryBlockIds.NIXIE_TUBE, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS), settings -> new NixieTubeBlock(settings.noOcclusion()));

    public static final NixieTubeControllerBlock NIXIE_TUBE_CONTROLLER = register(FactoryBlockIds.NIXIE_TUBE_CONTROLLER, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), NixieTubeControllerBlock::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_RECEIVER = register(FactoryBlockIds.WIRELESS_REDSTONE_RECEIVER, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), WirelessRedstoneBlock.Receiver::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_TRANSMITTER = register(FactoryBlockIds.WIRELESS_REDSTONE_TRANSMITTER, BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), WirelessRedstoneBlock.Transmitter::new);

    public static final RotationMeterBlock TACHOMETER = register(FactoryBlockIds.TACHOMETER, settings -> new RotationMeterBlock.Speed(settings.destroyTime(2).noOcclusion()));
    public static final RotationMeterBlock STRESSOMETER = register(FactoryBlockIds.STRESSOMETER, settings -> new RotationMeterBlock.Stress(settings.destroyTime(2).noOcclusion()));
    public static final ElectricMotorBlock ELECTRIC_MOTOR = register(FactoryBlockIds.ELECTRIC_MOTOR, settings -> new ElectricMotorBlock(settings.destroyTime(2).noOcclusion()));
    public static final ElectricGeneratorBlock ELECTRIC_GENERATOR = register(FactoryBlockIds.ELECTRIC_GENERATOR, settings -> new ElectricGeneratorBlock(settings.destroyTime(2).noOcclusion()));
    public static final WorkbenchBlock WORKBENCH = register(FactoryBlockIds.WORKBENCH, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE), settings -> new WorkbenchBlock(settings.noOcclusion()));
    public static final BlueprintWorkbenchBlock BLUEPRINT_WORKBENCH = register(FactoryBlockIds.BLUEPRINT_WORKBENCH, BlockBehaviour.Properties.ofFullCopy(WORKBENCH), BlueprintWorkbenchBlock::new);
    public static final MoldMakingTableBlock MOLDMAKING_TABLE = register(FactoryBlockIds.MOLDMAKING_TABLE, BlockBehaviour.Properties.ofFullCopy(WORKBENCH), MoldMakingTableBlock::new);
    public static final CreativeMotorBlock CREATIVE_MOTOR = register(FactoryBlockIds.CREATIVE_MOTOR, settings -> new CreativeMotorBlock(settings.strength(-1, -1).noOcclusion().noLootTable()));
    public static final CreativeContainerBlock CREATIVE_CONTAINER = register(FactoryBlockIds.CREATIVE_CONTAINER, settings -> new CreativeContainerBlock(settings.strength(-1, -1).noOcclusion().noLootTable()));
    public static final InvertedRedstoneLampBlock INVERTED_REDSTONE_LAMP = register(FactoryBlockIds.INVERTED_REDSTONE_LAMP,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new InvertedRedstoneLampBlock(settings.lightLevel((state) -> {
                return (Boolean)state.getValue(BlockStateProperties.LIT) ? 0 : 15;
            })));
    public static final LampBlock COLORED_LAMP = register(FactoryBlockIds.COLORED_LAMP, BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new LampBlock(settings.noOcclusion(), false));
    public static final LampBlock INVERTED_COLORED_LAMP = register(FactoryBlockIds.INVERTED_COLORED_LAMP, BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new LampBlock(settings.noOcclusion(), true));

    public static final SidedLampBlock CAGED_LAMP = register(FactoryBlockIds.CAGED_LAMP, BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.noOcclusion(), id("caged_lamp"), false));
    public static final SidedLampBlock INVERTED_CAGED_LAMP = register(FactoryBlockIds.INVERTED_CAGED_LAMP, BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.noOcclusion(), id("caged_lamp"), true));
    public static final SidedLampBlock FIXTURE_LAMP = register(FactoryBlockIds.FIXTURE_LAMP, BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.noOcclusion(), id("fixture_lamp"), false));
    public static final SidedLampBlock INVERTED_FIXTURE_LAMP = register(FactoryBlockIds.INVERTED_FIXTURE_LAMP, BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.noOcclusion(), id("fixture_lamp"), true));
    public static final PolymerButtonBlock STEEL_BUTTON = register(FactoryBlockIds.STEEL_BUTTON, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON), settings -> new PolymerButtonBlock("steel", BlockSetType.IRON, 5, settings.noOcclusion()));
    public static final TinyPotatoSpringBlock TINY_POTATO_SPRING = register(FactoryBlockIds.TINY_POTATO_SPRING, settings -> new TinyPotatoSpringBlock(settings.strength(1).noOcclusion()));
    public static final TinyPotatoSpringBlock GOLDEN_TINY_POTATO_SPRING = register(FactoryBlockIds.GOLDEN_TINY_POTATO_SPRING, settings -> new TinyPotatoSpringBlock(settings.strength(2).noOcclusion()));

    public static final CheeseBlock CHEESE = register(FactoryBlockIds.CHEESE, BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE), CheeseBlock::new);

    public static final RotationalDebugBlock ROTATION_DEBUG = register(FactoryBlockIds.ROTATION_DEBUG, settings -> new RotationalDebugBlock(settings.noOcclusion().strength(-1, -1)));
    public static final PipeBlock PIPE = register(FactoryBlockIds.PIPE, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new PipeBlock(settings.noOcclusion()));
    public static final FilteredPipeBlock FILTERED_PIPE = register(FactoryBlockIds.FILTERED_PIPE, BlockBehaviour.Properties.ofFullCopy(PIPE), settings -> new FilteredPipeBlock(settings.noOcclusion()));
    public static final RedstoneValvePipeBlock REDSTONE_VALVE_PIPE = register(FactoryBlockIds.REDSTONE_VALVE_PIPE, BlockBehaviour.Properties.ofFullCopy(PIPE), settings -> new RedstoneValvePipeBlock(settings.noOcclusion()));
    public static final Map<Block, PipeInWallBlock> WALL_WITH_PIPE = PipeInWallBlock.MAP;
    public static final SmelteryCoreBlock SMELTERY_CORE = register(FactoryBlockIds.SMELTERY_CORE, BlockBehaviour.Properties.ofFullCopy(STEAM_ENGINE).sound(SoundType.DEEPSLATE_BRICKS), SmelteryCoreBlock::new);

    public static final IndustrialSmelteryBlock SMELTERY = register(FactoryBlockIds.SMELTERY, BlockBehaviour.Properties.ofFullCopy(SMELTERY_CORE).noLootTable().lightLevel(x -> x.getValue(IndustrialSmelteryBlock.LIT) ? 14 : 0), IndustrialSmelteryBlock::new);
    public static final PrimitiveSmelteryBlock PRIMITIVE_SMELTERY = register(FactoryBlockIds.PRIMITIVE_SMELTERY, BlockBehaviour.Properties.ofFullCopy(SMELTERY_CORE).sound(SoundType.STONE)
            .lightLevel(x -> x.getValue(PrimitiveSmelteryBlock.LIT) ? 8 : 0), PrimitiveSmelteryBlock::new);
    public static final CastingTableBlock CASTING_TABLE = register(FactoryBlockIds.CASTING_TABLE, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON), CastingTableBlock::new);
    public static final CastingCauldronBlock CASTING_CAULDRON = register(FactoryBlockIds.CASTING_CAULDRON, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).overrideLootTable(Blocks.CAULDRON.getLootTable()), CastingCauldronBlock::new);
    public static final FaucetBlock FAUCET = register(FactoryBlockIds.FAUCET, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).sound(SoundType.COPPER), FaucetBlock::new);
    public static final PumpBlock PUMP = register(FactoryBlockIds.PUMP, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new PumpBlock(settings.noOcclusion()));
    public static final NozzleBlock NOZZLE = register(FactoryBlockIds.NOZZLE, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new NozzleBlock(settings.noOcclusion()));
    public static final DrainBlock DRAIN = register(FactoryBlockIds.DRAIN, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new DrainBlock(settings.noOcclusion()));
    public static final MDrainBlock MECHANICAL_DRAIN = register(FactoryBlockIds.MECHANICAL_DRAIN, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new MDrainBlock(settings.noOcclusion()));
    public static final MSpoutBlock MECHANICAL_SPOUT = register(FactoryBlockIds.MECHANICAL_SPOUT, BlockBehaviour.Properties.ofFullCopy(SPLITTER), settings -> new MSpoutBlock(settings.noOcclusion()));
    public static final CreativeDrainBlock CREATIVE_DRAIN = register(FactoryBlockIds.CREATIVE_DRAIN, BlockBehaviour.Properties.ofFullCopy(DRAIN), settings -> new CreativeDrainBlock(settings.noLootTable().strength(-1)));
    public static final FluidTankBlock FLUID_TANK = register(FactoryBlockIds.FLUID_TANK, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()), settings -> new FluidTankBlock(settings.noOcclusion()));
    public static final PortableFluidTankBlock PORTABLE_FLUID_TANK = register(FactoryBlockIds.PORTABLE_FLUID_TANK, settings -> new PortableFluidTankBlock(settings
            .mapColor(MapColor.COLOR_ORANGE).strength(2.0F).noOcclusion().sound(SoundType.COPPER).pushReaction(PushReaction.DESTROY)));

    public static final SimpleFastBlock STEEL_BLOCK = register(FactoryBlockIds.STEEL_BLOCK, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), SimpleFastBlock::create);
    public static final TpsProviderBlock TPS_PROVIDER = register(FactoryBlockIds.TPS_PROVIDER, BlockBehaviour.Properties.ofFullCopy(Blocks.COMMAND_BLOCK).noOcclusion(), TpsProviderBlock::new);


    public static void register() {
        BuiltInRegistries.BLOCK.addAlias(id("fauced"), id("faucet"));
        RegistryEntryAddedCallback.allEntries(BuiltInRegistries.BLOCK, block -> {
            if (block.value() instanceof WallBlock wallBlock) {
                var id = BuiltInRegistries.BLOCK.getKey(wallBlock);
                var cableWall = register(FactoryBlockIds.of("wall_with_cable/" + id.getNamespace() + "/" + id.getPath()), BlockBehaviour.Properties.ofFullCopy(wallBlock).noLootTable(), settings -> new WallWithCableBlock(settings, wallBlock));
                var pipeWall = register(FactoryBlockIds.of("wall_with_pipe/" + id.getNamespace() + "/" + id.getPath()), BlockBehaviour.Properties.ofFullCopy(wallBlock).noLootTable(), settings -> new PipeInWallBlock(settings, wallBlock));

                FactoryBlockEntities.CABLE.addValidBlock(cableWall);
                FactoryBlockEntities.PIPE.addValidBlock(pipeWall);
            }
        });

        if (ModInit.DEV_MODE) {
            ServerLifecycleEvents.SERVER_STARTED.register((FactoryBlocks::validate));
            ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(((server, resourceManager, success) -> {
                validate(server);
            }));
        }
    }

    private static void validate(MinecraftServer server) {
        //Registries.BLOCK.stream().sorted(Comparator.comparing(x -> -x.getStateManager().getStates().size()))
        //        .forEachOrdered(block ->
        //                System.out.println(block.getRegistryEntry().getIdAsString() + " -> " + block.getStateManager().getStates().size()));
        for (var block : BLOCKS) {
            if (block.getLootTable().isPresent()) {
                var lt = server.reloadableRegistries().getLootTable(block.getLootTable().get());
                if (lt == LootTable.EMPTY) {
                    ModInit.LOGGER.warn("Missing loot table? " + block.getLootTable().get().identifier());
                }
            }
            if (block instanceof EntityBlock provider) {
                var be = provider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                if (be != null) {
                     assert be.getType().isValid(block.defaultBlockState());
                }
            }
        }
    }

    public static <T extends Block> T register(ResourceKey<Block> id, Function<BlockBehaviour.Properties, T> function) {
        return register(id, BlockBehaviour.Properties.of(), function);
    }
    public static <T extends Block> T register(ResourceKey<Block> id, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> function) {
        var item = function.apply(settings.setId(id));
        BLOCKS.add(item);
        return Registry.register(BuiltInRegistries.BLOCK, id, item);
    }
}
