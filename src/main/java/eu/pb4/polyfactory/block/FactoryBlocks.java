package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlock;
import eu.pb4.polyfactory.block.creative.CreativeDrainBlock;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.GatedCableBlock;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.data.io.ArithmeticOperatorBlock;
import eu.pb4.polyfactory.block.data.io.DataComparatorBlock;
import eu.pb4.polyfactory.block.data.io.DataExtractorBlock;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.block.data.output.*;
import eu.pb4.polyfactory.block.data.providers.*;
import eu.pb4.polyfactory.block.electric.ElectricGeneratorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.fluids.*;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
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
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryBlocks {
    private static final List<Block> BLOCKS = new ArrayList<>();
    public static final ConveyorBlock CONVEYOR = register("conveyor", settings -> new ConveyorBlock(settings.hardness(3).nonOpaque()));
    public static final ConveyorBlock STICKY_CONVEYOR = register("sticky_conveyor", settings -> new ConveyorBlock(settings.hardness(3).nonOpaque()));
    public static final FunnelBlock FUNNEL = register("funnel", Block.Settings.copy(Blocks.SPRUCE_TRAPDOOR), settings -> new FunnelBlock(settings.nonOpaque()));
    public static final SplitterBlock SPLITTER = register("splitter", settings -> new SplitterBlock(settings.mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(3.3F).nonOpaque()));
    public static final FanBlock FAN = register("fan", settings -> new FanBlock(settings.nonOpaque().hardness(3).requiresTool()));
    public static final EjectorBlock EJECTOR = register("ejector", AbstractBlock.Settings.copy(FAN), EjectorBlock::new);
    public static final SelectivePassthroughBlock METAL_GRID = register("metal_grid", Block.Settings.copy(Blocks.IRON_BLOCK), settings -> new SelectivePassthroughBlock(settings.strength(4.0F, 3.0F).nonOpaque()));
    public static final HandCrankBlock HAND_CRANK = register("hand_crank", settings -> new HandCrankBlock(settings.hardness(1).nonOpaque()));
    public static final SteamEngineBlock STEAM_ENGINE = register("steam_engine", Block.Settings.copy(SPLITTER), settings -> new SteamEngineBlock(settings.strength(4F).nonOpaque()));
    public static final GrinderBlock GRINDER = register("grinder", Block.Settings.copy(SPLITTER), GrinderBlock::new);
    public static final PressBlock PRESS = register("press", Block.Settings.copy(SPLITTER), PressBlock::new);
    public static final MixerBlock MIXER = register("mixer", Block.Settings.copy(SPLITTER), MixerBlock::new);
    public static final MCrafterBlock CRAFTER = register("crafter", Block.Settings.copy(SPLITTER), MCrafterBlock::new);
    public static final MinerBlock MINER = register("miner", Block.Settings.copy(SPLITTER), MinerBlock::new);
    public static final PlacerBlock PLACER = register("placer", Block.Settings.copy(SPLITTER), PlacerBlock::new);
    public static final PlanterBlock PLANTER = register("planter", Block.Settings.copy(SPLITTER), PlanterBlock::new);
    public static final AxleBlock AXLE = register("axle", Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD), settings -> new AxleBlock(settings.strength(2.5F).nonOpaque()));
    public static final AxleWithGearBlock AXLE_WITH_GEAR = register("axle_with_gear", Block.Settings.copy(AXLE), AxleWithGearBlock::new);
    public static final AxleWithLargeGearBlock AXLE_WITH_LARGE_GEAR = register("axle_with_large_gear", Block.Settings.copy(AXLE), AxleWithLargeGearBlock::new);
    public static final TurntableBlock TURNTABLE = register("turntable", Block.Settings.copy(AXLE), TurntableBlock::new);
    public static final GearboxBlock GEARBOX = register("gearbox", Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD), settings -> new GearboxBlock(settings.strength(2.5F).nonOpaque()));
    public static final ClutchBlock CLUTCH = register("clutch", Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD), settings -> new ClutchBlock(settings.strength(2.5F).nonOpaque()));
    public static final WindmillBlock WINDMILL = register("windmill", Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD), settings -> new WindmillBlock(settings.strength(2.5F).nonOpaque()));
    public static final ContainerBlock CONTAINER = register("wooden_container", Block.Settings.copy(Blocks.CHEST), settings -> new ContainerBlock(9 * 5, settings.nonOpaque()));
    public static final ItemPackerBlock ITEM_PACKER = register("item_packer", Block.Settings.copy(SPLITTER), ItemPackerBlock::new);
    public static final CableBlock CABLE = register("cable", Block.Settings.copy(Blocks.GLASS), settings -> new CableBlock(settings.breakInstantly().nonOpaque()));
    public static final GatedCableBlock GATED_CABLE = register("gated_cable", Block.Settings.copy(SPLITTER), GatedCableBlock::new);
    public static final Map<Block, WallWithCableBlock> WALL_WITH_CABLE = WallWithCableBlock.MAP;
    public static final CabledDataProviderBlock ITEM_COUNTER = register("item_counter", Block.Settings.copy(SPLITTER), CabledDataProviderBlock::new);

    public static final RedstoneInputBlock REDSTONE_INPUT = register("redstone_input", Block.Settings.copy(ITEM_COUNTER), RedstoneInputBlock::new);
    public static final RedstoneOutputBlock REDSTONE_OUTPUT = register("redstone_output", Block.Settings.copy(ITEM_COUNTER), RedstoneOutputBlock::new);
    public static final SpeakerBlock SPEAKER = register("speaker",Block.Settings.copy(ITEM_COUNTER), SpeakerBlock::new);
    public static final RecordPlayerBlock RECORD_PLAYER = register("record_player",Block.Settings.copy(ITEM_COUNTER), RecordPlayerBlock::new);
    public static final ItemReaderBlock ITEM_READER = register("item_reader", Block.Settings.copy(ITEM_COUNTER), ItemReaderBlock::new);
    public static final BlockObserverBlock BLOCK_OBSERVER = register("block_observer", Block.Settings.copy(ITEM_COUNTER), BlockObserverBlock::new);
    public static final TextInputBlock TEXT_INPUT = register("text_input", Block.Settings.copy(ITEM_COUNTER), TextInputBlock::new);
    public static final ArithmeticOperatorBlock ARITHMETIC_OPERATOR = register("arithmetic_operator",
            Block.Settings.copy(ITEM_COUNTER), ArithmeticOperatorBlock::new);

    public static final DataComparatorBlock DATA_COMPARATOR = register("data_comparator", Block.Settings.copy(ITEM_COUNTER), DataComparatorBlock::new);
    public static final DataExtractorBlock DATA_EXTRACTOR = register("data_extractor", Block.Settings.copy(ITEM_COUNTER), DataExtractorBlock::new);
    public static final DataMemoryBlock DATA_MEMORY = register("data_memory", Block.Settings.copy(ITEM_COUNTER), DataMemoryBlock::new);

    public static final HologramProjectorBlock HOLOGRAM_PROJECTOR = register("hologram_projector", Block.Settings.copy(SPLITTER), HologramProjectorBlock::new);
    public static final NixieTubeBlock NIXIE_TUBE = register("nixie_tube", Block.Settings.copy(Blocks.GLASS), settings -> new NixieTubeBlock(settings.nonOpaque()));

    public static final NixieTubeControllerBlock NIXIE_TUBE_CONTROLLER = register("nixie_tube_controller", Block.Settings.copy(ITEM_COUNTER), NixieTubeControllerBlock::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_RECEIVER = register("wireless_redstone_receiver", AbstractBlock.Settings.copy(ITEM_COUNTER), WirelessRedstoneBlock.Receiver::new);
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_TRANSMITTER = register("wireless_redstone_transmitter", AbstractBlock.Settings.copy(ITEM_COUNTER), WirelessRedstoneBlock.Transmitter::new);

    public static final RotationMeterBlock TACHOMETER = register("tachometer", settings -> new RotationMeterBlock.Speed(settings.hardness(2).nonOpaque()));
    public static final RotationMeterBlock STRESSOMETER = register("stressometer", settings -> new RotationMeterBlock.Stress(settings.hardness(2).nonOpaque()));
    public static final ElectricMotorBlock ELECTRIC_MOTOR = register("electric_motor", settings -> new ElectricMotorBlock(settings.hardness(2).nonOpaque()));
    public static final ElectricGeneratorBlock ELECTRIC_GENERATOR = register("electric_generator", settings -> new ElectricGeneratorBlock(settings.hardness(2).nonOpaque()));
    public static final WorkbenchBlock WORKBENCH = register("workbench", Block.Settings.copy(Blocks.CRAFTING_TABLE), settings -> new WorkbenchBlock(settings.nonOpaque()));

    public static final CreativeMotorBlock CREATIVE_MOTOR = register("creative_motor", settings -> new CreativeMotorBlock(settings.strength(-1, -1).nonOpaque().dropsNothing()));
    public static final CreativeContainerBlock CREATIVE_CONTAINER = register("creative_container", settings -> new CreativeContainerBlock(settings.strength(-1, -1).nonOpaque().dropsNothing()));
    public static final InvertedRedstoneLampBlock INVERTED_REDSTONE_LAMP = register("inverted_redstone_lamp",
            Block.Settings.copy(Blocks.REDSTONE_LAMP), settings -> new InvertedRedstoneLampBlock(settings.luminance((state) -> {
                return (Boolean)state.get(Properties.LIT) ? 0 : 15;
            })));
    public static final LampBlock LAMP = register("colored_lamp", Block.Settings.copy(Blocks.REDSTONE_LAMP), settings -> new LampBlock(settings.nonOpaque(), false));
    public static final LampBlock INVERTED_LAMP = register("inverted_colored_lamp", Block.Settings.copy(INVERTED_REDSTONE_LAMP), settings -> new LampBlock(settings.nonOpaque(), true));

    public static final SidedLampBlock CAGED_LAMP = register("caged_lamp", Block.Settings.copy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.nonOpaque(), id("caged_lamp"), false));
    public static final SidedLampBlock INVERTED_CAGED_LAMP = register("inverted_caged_lamp", Block.Settings.copy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Full(settings.nonOpaque(), id("caged_lamp"), true));
    public static final SidedLampBlock FIXTURE_LAMP = register("fixture_lamp", Block.Settings.copy(Blocks.REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.nonOpaque(), id("fixture_lamp"), false));
    public static final SidedLampBlock INVERTED_FIXTURE_LAMP = register("inverted_fixture_lamp", Block.Settings.copy(INVERTED_REDSTONE_LAMP), settings -> new SidedLampBlock.Flat(settings.nonOpaque(), id("fixture_lamp"), true));
    public static final PolymerButtonBlock STEEL_BUTTON = register("steel_button", Block.Settings.copy(Blocks.STONE_BUTTON), settings -> new PolymerButtonBlock("steel", BlockSetType.IRON, 5, settings.nonOpaque()));
    public static final TinyPotatoSpringBlock TINY_POTATO_SPRING = register("tiny_potato_spring", settings -> new TinyPotatoSpringBlock(settings.strength(1).nonOpaque()));
    public static final RotationalDebugBlock ROTATION_DEBUG = register("rot_debug", settings -> new RotationalDebugBlock(settings.strength(-1, -1)));
    public static final PipeBlock PIPE = register("pipe", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new PipeBlock(settings.nonOpaque()));
    public static final FilteredPipeBlock FILTERED_PIPE = register("filtered_pipe", Block.Settings.copy(PIPE), settings -> new FilteredPipeBlock(settings.nonOpaque()));
    public static final RedstoneValvePipeBlock REDSTONE_VALVE_PIPE = register("redstone_valve_pipe", Block.Settings.copy(PIPE), settings -> new RedstoneValvePipeBlock(settings.nonOpaque()));
    public static final Map<Block, PipeInWallBlock> WALL_WITH_PIPE = PipeInWallBlock.MAP;

    public static final PumpBlock PUMP = register("pump", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new PumpBlock(settings.nonOpaque()));
    public static final NozzleBlock NOZZLE = register("nozzle", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new NozzleBlock(settings.nonOpaque()));
    public static final DrainBlock DRAIN = register("drain", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new DrainBlock(settings.nonOpaque()));
    public static final MDrainBlock MECHANICAL_DRAIN = register("mechanical_drain", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new MDrainBlock(settings.nonOpaque()));
    public static final MSpoutBlock MECHANICAL_SPOUT = register("mechanical_spout", Block.Settings.copy(SPLITTER), settings -> new MSpoutBlock(settings.nonOpaque()));
    public static final CreativeDrainBlock CREATIVE_DRAIN = register("creative_drain", Block.Settings.copy(DRAIN), settings -> new CreativeDrainBlock(settings.dropsNothing().strength(-1)));
    public static final FluidTankBlock FLUID_TANK = register("fluid_tank", Block.Settings.copy(Blocks.COPPER_BLOCK), settings -> new FluidTankBlock(settings.nonOpaque()));
    public static final PortableFluidTankBlock PORTABLE_FLUID_TANK = register("portable_fluid_tank", settings -> new PortableFluidTankBlock(settings
            .mapColor(MapColor.ORANGE).strength(2.0F).nonOpaque().sounds(BlockSoundGroup.COPPER).pistonBehavior(PistonBehavior.DESTROY)));

    public static void register() {
        var s = System.currentTimeMillis();
        for (var block : Registries.BLOCK) {
            if (block instanceof WallBlock wallBlock) {
                var id = Registries.BLOCK.getId(wallBlock);
                register("wall_with_cable/" + id.getNamespace() + "/" + id.getPath(), AbstractBlock.Settings.copy(wallBlock), settings -> new WallWithCableBlock(wallBlock));
                register("wall_with_pipe/" + id.getNamespace() + "/" + id.getPath(), AbstractBlock.Settings.copy(wallBlock), settings -> new PipeInWallBlock(wallBlock));
            }
        }

        if (ModInit.DEV_MODE) {
            ServerLifecycleEvents.SERVER_STARTED.register((FactoryBlocks::validate));
            ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(((server, resourceManager, success) -> {
                validate(server);
            }));
        }
    }

    private static void validate(MinecraftServer server) {
        for (var block : BLOCKS) {
            //if (block.getLootTableKey() != ) {
                var lt = server.getReloadableRegistries().getLootTable(block.getLootTableKey());
                if (lt == LootTable.EMPTY) {
                    ModInit.LOGGER.warn("Missing loot table? " + block.getLootTableKey().getValue());
                }
            //}
            if (block instanceof BlockEntityProvider provider) {
                var be = provider.createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
                if (be != null) {
                     assert be.getType().supports(block.getDefaultState());
                }
            }
        }
    }

    public static <T extends Block> T register(String path, Function<AbstractBlock.Settings, T> function) {
        return register(path, AbstractBlock.Settings.create(), function);
    }
    public static <T extends Block> T register(String path, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, T> function) {
        var id = Identifier.of(ModInit.ID, path);
        var item = function.apply(settings);
        BLOCKS.add(item);
        return Registry.register(Registries.BLOCK, id, item);
    }
}
