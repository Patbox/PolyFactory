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
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlock;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.other.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
    public static final ConveyorBlock CONVEYOR = register("conveyor", settings -> new ConveyorBlock(settings.destroyTime(3).noOcclusion()));
    public static final ConveyorBlock STICKY_CONVEYOR = register("sticky_conveyor", settings -> new ConveyorBlock(settings.destroyTime(3).noOcclusion()));
    public static final FunnelBlock FUNNEL = register("funnel", BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_TRAPDOOR).noOcclusion(), FunnelBlock::new);
    public static final SlotAwareFunnelBlock SLOT_AWARE_FUNNEL = register("slot_aware_funnel", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR).noOcclusion(), SlotAwareFunnelBlock::new);
    public static final SplitterBlock SPLITTER = register("splitter", settings -> new SplitterBlock(settings.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.3F).noOcclusion().sound(SoundType.IRON)));
    public static final FanBlock FAN = register("fan", settings -> new FanBlock(settings.noOcclusion().destroyTime(3).sound(SoundType.IRON).requiresCorrectToolForDrops()));
    public static final EjectorBlock EJECTOR = register("ejector", BlockBehaviour.Properties.ofFullCopy(FAN), EjectorBlock::new);
    public static final SelectivePassthroughBlock METAL_GRID = register("metal_grid", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), settings -> new SelectivePassthroughBlock(settings.strength(4.0F, 3.0F).noOcclusion()));
    public static final HandCrankBlock HAND_CRANK = register("hand_crank", settings -> new HandCrankBlock(settings.destroyTime(1).noOcclusion()));
    public static final SteamEngineBlock STEAM_ENGINE = register("steam_engine", BlockBehaviour.Properties.ofFullCopy(SPLITTER), settings -> new SteamEngineBlock(settings.strength(4F).noOcclusion()));
    public static final GrinderBlock GRINDER = register("grinder", BlockBehaviour.Properties.ofFullCopy(SPLITTER).sound(SoundType.WOOD), GrinderBlock::new);
    public static final PressBlock PRESS = register("press", BlockBehaviour.Properties.ofFullCopy(SPLITTER), PressBlock::new);
    public static final MixerBlock MIXER = register("mixer", BlockBehaviour.Properties.ofFullCopy(SPLITTER), MixerBlock::new);
    public static final MCrafterBlock CRAFTER = register("crafter", BlockBehaviour.Properties.ofFullCopy(SPLITTER), MCrafterBlock::new);
    public static final MinerBlock MINER = register("miner", BlockBehaviour.Properties.ofFullCopy(SPLITTER), MinerBlock::new);
    public static final PlacerBlock PLACER = register("placer", BlockBehaviour.Properties.ofFullCopy(SPLITTER), PlacerBlock::new);
    public static final PlanterBlock PLANTER = register("planter", BlockBehaviour.Properties.ofFullCopy(SPLITTER), PlanterBlock::new);
    public static final AxleBlock AXLE = register("axle", BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new AxleBlock(settings.strength(2.5F).noOcclusion().isSuffocating(Blocks::never)));
    public static final ChainDriveBlock CHAIN_DRIVE = register("chain_drive", BlockBehaviour.Properties.ofFullCopy(AXLE), ChainDriveBlock::new);
    public static final AxleWithGearBlock AXLE_WITH_GEAR = register("axle_with_gear", BlockBehaviour.Properties.ofFullCopy(AXLE).sound(SoundType.IRON), AxleWithGearBlock::new);
    public static final AxleWithLargeGearBlock AXLE_WITH_LARGE_GEAR = register("axle_with_large_gear", BlockBehaviour.Properties.ofFullCopy(AXLE_WITH_GEAR), AxleWithLargeGearBlock::new);
    public static final TurntableBlock TURNTABLE = register("turntable", BlockBehaviour.Properties.ofFullCopy(AXLE), TurntableBlock::new);
    public static final GearboxBlock GEARBOX = register("gearbox", BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new GearboxBlock(settings.strength(2.5F).noOcclusion()));
    public static final ClutchBlock CLUTCH = register("clutch", BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new ClutchBlock(settings.strength(2.5F).noOcclusion()));
    public static final GearshiftBlock GEARSHIFT = register("gearshift", BlockBehaviour.Properties.ofFullCopy(CLUTCH), GearshiftBlock::new);
    public static final WindmillBlock WINDMILL = register("windmill", BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD), settings -> new WindmillBlock(settings.strength(2.5F).noOcclusion()));
    public static final ContainerBlock CONTAINER = register("wooden_container", BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST), settings -> new ContainerBlock(9 * 5, settings.noOcclusion()));
    public static final ItemPackerBlock ITEM_PACKER = register("item_packer", BlockBehaviour.Properties.ofFullCopy(SPLITTER), ItemPackerBlock::new);
    public static final CableBlock CABLE = register("cable", BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).sound(SoundType.WOOL), settings -> new CableBlock(settings.instabreak().noOcclusion()));
    public static final GatedCableBlock GATED_CABLE = register("gated_cable", BlockBehaviour.Properties.ofFullCopy(SPLITTER).strength(2.2F).sound(SoundType.STONE), GatedCableBlock::new);
    public static final Map<Block, WallWithCableBlock> WALL_WITH_CABLE = WallWithCableBlock.MAP;
    public static final DirectionalCabledDataProviderBlock ITEM_COUNTER = register("item_counter", BlockBehaviour.Properties.ofFullCopy(SPLITTER), DirectionalCabledDataProviderBlock::new);

    public static final RedstoneInputBlock REDSTONE_INPUT = register("redstone_input", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RedstoneInputBlock::new);
    public static final RedstoneOutputBlock REDSTONE_OUTPUT = register("redstone_output", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RedstoneOutputBlock::new);
    public static final SpeakerBlock SPEAKER = register("speaker",BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), SpeakerBlock::new);
    public static final RecordPlayerBlock RECORD_PLAYER = register("record_player",BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), RecordPlayerBlock::new);
    public static final ItemReaderBlock ITEM_READER = register("item_reader", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ItemReaderBlock::new);
    public static final BlockObserverBlock BLOCK_OBSERVER = register("block_observer", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), BlockObserverBlock::new);
    public static final TextInputBlock TEXT_INPUT = register("text_input", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), TextInputBlock::new);
    public static final DigitalClockBlock DIGITAL_CLOCK = register("digital_clock", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DigitalClockBlock::new);
    public static final ArithmeticOperatorBlock ARITHMETIC_OPERATOR = register("arithmetic_operator",
            BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ArithmeticOperatorBlock::new);

    public static final DataComparatorBlock DATA_COMPARATOR = register("data_comparator", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataComparatorBlock::new);
    public static final DataExtractorBlock DATA_EXTRACTOR = register("data_extractor", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataExtractorBlock::new);
    public static final ProgrammableDataExtractorBlock PROGRAMMABLE_DATA_EXTRACTOR = register("programmable_data_extractor", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), ProgrammableDataExtractorBlock::new);
    public static final DataMemoryBlock DATA_MEMORY = register("data_memory", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), DataMemoryBlock::new);

    public static final GaugeBlock GAUGE = register("gauge", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR), GaugeBlock::new);
    public static final HologramProjectorBlock HOLOGRAM_PROJECTOR = register("hologram_projector", BlockBehaviour.Properties.ofFullCopy(SPLITTER), HologramProjectorBlock::new);
    public static final NixieTubeBlock NIXIE_TUBE = register("nixie_tube", BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS), settings -> new NixieTubeBlock(settings.noOcclusion()));

    public static final NixieTubeControllerBlock NIXIE_TUBE_CONTROLLER = register("nixie_tube_controller", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), NixieTubeControllerBlock::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_RECEIVER = register("wireless_redstone_receiver", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), WirelessRedstoneBlock.Receiver::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_TRANSMITTER = register("wireless_redstone_transmitter", BlockBehaviour.Properties.ofFullCopy(ITEM_COUNTER), WirelessRedstoneBlock.Transmitter::new);

    public static final RotationMeterBlock TACHOMETER = register("tachometer", settings -> new RotationMeterBlock.Speed(settings.destroyTime(2).noOcclusion()));
    public static final RotationMeterBlock STRESSOMETER = register("stressometer", settings -> new RotationMeterBlock.Stress(settings.destroyTime(2).noOcclusion()));
    public static final ElectricMotorBlock ELECTRIC_MOTOR = register("electric_motor", settings -> new ElectricMotorBlock(settings.destroyTime(2).noOcclusion()));
    public static final ElectricGeneratorBlock ELECTRIC_GENERATOR = register("electric_generator", settings -> new ElectricGeneratorBlock(settings.destroyTime(2).noOcclusion()));
    public static final WorkbenchBlock WORKBENCH = register("workbench", BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE), settings -> new WorkbenchBlock(settings.noOcclusion()));
    public static final BlueprintWorkbenchBlock BLUEPRINT_WORKBENCH = register("blueprint_workbench", BlockBehaviour.Properties.ofFullCopy(WORKBENCH), BlueprintWorkbenchBlock::new);
    public static final MoldMakingTableBlock MOLDMAKING_TABLE = register("moldmaking_table", BlockBehaviour.Properties.ofFullCopy(WORKBENCH), MoldMakingTableBlock::new);
    public static final CreativeMotorBlock CREATIVE_MOTOR = register("creative_motor", settings -> new CreativeMotorBlock(settings.strength(-1, -1).noOcclusion().noLootTable()));
    public static final CreativeContainerBlock CREATIVE_CONTAINER = register("creative_container", settings -> new CreativeContainerBlock(settings.strength(-1, -1).noOcclusion().noLootTable()));
    public static final InvertedRedstoneLampBlock INVERTED_REDSTONE_LAMP = register("inverted_redstone_lamp",
            BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new InvertedRedstoneLampBlock(settings.lightLevel((state) -> {
                return (Boolean)state.getValue(BlockStateProperties.LIT) ? 0 : 15;
            })));
    public static final LampBlock LAMP = register("colored_lamp", BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new LampBlock(settings.noOcclusion(), false));
    public static final LampBlock INVERTED_LAMP = register("inverted_colored_lamp", BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new LampBlock(settings.noOcclusion(), true));

    public static final SidedLampBlock CAGED_LAMP = register("caged_lamp", BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.noOcclusion(), id("caged_lamp"), false));
    public static final SidedLampBlock INVERTED_CAGED_LAMP = register("inverted_caged_lamp", BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.noOcclusion(), id("caged_lamp"), true));
    public static final SidedLampBlock FIXTURE_LAMP = register("fixture_lamp", BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.noOcclusion(), id("fixture_lamp"), false));
    public static final SidedLampBlock INVERTED_FIXTURE_LAMP = register("inverted_fixture_lamp", BlockBehaviour.Properties.ofFullCopy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.noOcclusion(), id("fixture_lamp"), true));
    public static final PolymerButtonBlock STEEL_BUTTON = register("steel_button", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON), settings -> new PolymerButtonBlock("steel", BlockSetType.IRON, 5, settings.noOcclusion()));
    public static final TinyPotatoSpringBlock TINY_POTATO_SPRING = register("tiny_potato_spring", settings -> new TinyPotatoSpringBlock(settings.strength(1).noOcclusion()));
    public static final TinyPotatoSpringBlock GOLDEN_TINY_POTATO_SPRING = register("golden_tiny_potato_spring", settings -> new TinyPotatoSpringBlock(settings.strength(2).noOcclusion()));
    public static final RotationalDebugBlock ROTATION_DEBUG = register("rot_debug", settings -> new RotationalDebugBlock(settings.strength(-1, -1)));
    public static final PipeBlock PIPE = register("pipe", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new PipeBlock(settings.noOcclusion()));
    public static final FilteredPipeBlock FILTERED_PIPE = register("filtered_pipe", BlockBehaviour.Properties.ofFullCopy(PIPE), settings -> new FilteredPipeBlock(settings.noOcclusion()));
    public static final RedstoneValvePipeBlock REDSTONE_VALVE_PIPE = register("redstone_valve_pipe", BlockBehaviour.Properties.ofFullCopy(PIPE), settings -> new RedstoneValvePipeBlock(settings.noOcclusion()));
    public static final Map<Block, PipeInWallBlock> WALL_WITH_PIPE = PipeInWallBlock.MAP;
    public static final SmelteryCoreBlock SMELTERY_CORE = register("smeltery_core", BlockBehaviour.Properties.ofFullCopy(STEAM_ENGINE).sound(SoundType.DEEPSLATE_BRICKS), SmelteryCoreBlock::new);

    public static final IndustrialSmelteryBlock SMELTERY = register("smeltery", BlockBehaviour.Properties.ofFullCopy(SMELTERY_CORE).noLootTable().lightLevel(x -> x.getValue(IndustrialSmelteryBlock.LIT) ? 14 : 0), IndustrialSmelteryBlock::new);
    public static final PrimitiveSmelteryBlock PRIMITIVE_SMELTERY = register("primitive_smeltery", BlockBehaviour.Properties.ofFullCopy(SMELTERY_CORE).sound(SoundType.STONE)
            .lightLevel(x -> x.getValue(PrimitiveSmelteryBlock.LIT) ? 8 : 0), PrimitiveSmelteryBlock::new);
    public static final CastingTableBlock CASTING_TABLE = register("casting_table", BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON), CastingTableBlock::new);
    public static final CastingCauldronBlock CASTING_CAULDRON = register("casting_cauldron", BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).overrideLootTable(Blocks.CAULDRON.getLootTable()), CastingCauldronBlock::new);
    public static final FaucedBlock FAUCED = register("fauced", BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).sound(SoundType.COPPER), FaucedBlock::new);
    public static final PumpBlock PUMP = register("pump", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new PumpBlock(settings.noOcclusion()));
    public static final NozzleBlock NOZZLE = register("nozzle", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new NozzleBlock(settings.noOcclusion()));
    public static final DrainBlock DRAIN = register("drain", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new DrainBlock(settings.noOcclusion()));
    public static final MDrainBlock MECHANICAL_DRAIN = register("mechanical_drain", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new MDrainBlock(settings.noOcclusion()));
    public static final MSpoutBlock MECHANICAL_SPOUT = register("mechanical_spout", BlockBehaviour.Properties.ofFullCopy(SPLITTER), settings -> new MSpoutBlock(settings.noOcclusion()));
    public static final CreativeDrainBlock CREATIVE_DRAIN = register("creative_drain", BlockBehaviour.Properties.ofFullCopy(DRAIN), settings -> new CreativeDrainBlock(settings.noLootTable().strength(-1)));
    public static final FluidTankBlock FLUID_TANK = register("fluid_tank", BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK), settings -> new FluidTankBlock(settings.noOcclusion()));
    public static final PortableFluidTankBlock PORTABLE_FLUID_TANK = register("portable_fluid_tank", settings -> new PortableFluidTankBlock(settings
            .mapColor(MapColor.COLOR_ORANGE).strength(2.0F).noOcclusion().sound(SoundType.COPPER).pushReaction(PushReaction.DESTROY)));

    public static final SimpleFastBlock STEEL_BLOCK = register("steel_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), SimpleFastBlock::create);
    public static final TpsProviderBlock TPS_PROVIDER = register("tps_provider", BlockBehaviour.Properties.ofFullCopy(Blocks.COMMAND_BLOCK).noOcclusion(), TpsProviderBlock::new);


    public static void register() {
        RegistryEntryAddedCallback.allEntries(BuiltInRegistries.BLOCK, block -> {
            if (block.value() instanceof WallBlock wallBlock) {
                var id = BuiltInRegistries.BLOCK.getKey(wallBlock);
                var cableWall = register("wall_with_cable/" + id.getNamespace() + "/" + id.getPath(), BlockBehaviour.Properties.ofFullCopy(wallBlock).noLootTable(), settings -> new WallWithCableBlock(settings, wallBlock));
                var pipeWall = register("wall_with_pipe/" + id.getNamespace() + "/" + id.getPath(), BlockBehaviour.Properties.ofFullCopy(wallBlock).noLootTable(), settings -> new PipeInWallBlock(settings, wallBlock));

                FactoryBlockEntities.CABLE.addSupportedBlock(cableWall);
                FactoryBlockEntities.PIPE.addSupportedBlock(pipeWall);
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

    public static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> function) {
        return register(path, BlockBehaviour.Properties.of(), function);
    }
    public static <T extends Block> T register(String path, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> function) {
        var id = Identifier.fromNamespaceAndPath(ModInit.ID, path);
        var item = function.apply(settings.setId(ResourceKey.create(Registries.BLOCK, id)));
        BLOCKS.add(item);
        return Registry.register(BuiltInRegistries.BLOCK, id, item);
    }
}
