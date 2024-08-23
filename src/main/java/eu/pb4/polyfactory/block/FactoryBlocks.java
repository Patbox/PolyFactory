package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlock;
import eu.pb4.polyfactory.block.creative.CreativeDrainBlock;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.data.io.ArithmeticOperatorBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.block.data.output.*;
import eu.pb4.polyfactory.block.data.providers.*;
import eu.pb4.polyfactory.block.electric.ElectricGeneratorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.fluids.*;
import eu.pb4.polyfactory.block.mechanical.machines.PlacerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlock;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlock;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FactoryBlocks {
    private static final List<Block> BLOCKS = new ArrayList<>();
    public static final ConveyorBlock CONVEYOR = register("conveyor", new ConveyorBlock(Block.Settings.create().hardness(3).nonOpaque()));
    public static final ConveyorBlock STICKY_CONVEYOR = register("sticky_conveyor", new ConveyorBlock(Block.Settings.create().hardness(3).nonOpaque()));
    public static final FunnelBlock FUNNEL = register("funnel", new FunnelBlock(Block.Settings.copy(Blocks.SPRUCE_TRAPDOOR).nonOpaque()));
    public static final SplitterBlock SPLITTER = register("splitter", new SplitterBlock(Block.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(3.3F).nonOpaque()));
    public static final FanBlock FAN = register("fan", new FanBlock(Block.Settings.create().nonOpaque().hardness(1)));
    public static final SelectivePassthroughBlock METAL_GRID = register("metal_grid", new SelectivePassthroughBlock(Block.Settings.copy(Blocks.IRON_BLOCK).strength(4.0F, 3.0F).nonOpaque()));
    public static final HandCrankBlock HAND_CRANK = register("hand_crank", new HandCrankBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final SteamEngineBlock STEAM_ENGINE = register("steam_engine", new SteamEngineBlock(Block.Settings.copy(SPLITTER).strength(4F).nonOpaque()));
    public static final GrinderBlock GRINDER = register("grinder", new GrinderBlock(Block.Settings.copy(SPLITTER)));
    public static final PressBlock PRESS = register("press", new PressBlock(Block.Settings.copy(SPLITTER)));
    public static final MixerBlock MIXER = register("mixer", new MixerBlock(Block.Settings.copy(SPLITTER)));
    public static final MCrafterBlock CRAFTER = register("crafter", new MCrafterBlock(Block.Settings.copy(SPLITTER)));
    public static final MinerBlock MINER = register("miner", new MinerBlock(Block.Settings.copy(SPLITTER)));
    public static final PlacerBlock PLACER = register("placer", new PlacerBlock(Block.Settings.copy(SPLITTER)));
    public static final PlanterBlock PLANTER = register("planter", new PlanterBlock(Block.Settings.copy(SPLITTER)));
    public static final AxleBlock AXLE = register("axle", new AxleBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final AxleWithGearBlock AXLE_WITH_GEAR = register("axle_with_gear", new AxleWithGearBlock(Block.Settings.copy(AXLE)));
    public static final AxleWithLargeGearBlock AXLE_WITH_LARGE_GEAR = register("axle_with_large_gear", new AxleWithLargeGearBlock(Block.Settings.copy(AXLE)));
    public static final TurntableBlock TURNTABLE = register("turntable", new TurntableBlock(Block.Settings.copy(AXLE)));
    public static final GearboxBlock GEARBOX = register("gearbox", new GearboxBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final ClutchBlock CLUTCH = register("clutch", new ClutchBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final WindmillBlock WINDMILL = register("windmill", new WindmillBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final ContainerBlock CONTAINER = register("wooden_container", new ContainerBlock(9 * 5, Block.Settings.copy(Blocks.CHEST).nonOpaque()));
    public static final CableBlock CABLE = register("cable", new CableBlock(Block.Settings.copy(Blocks.GLASS).breakInstantly().nonOpaque()));
    public static final Map<Block, WallWithCableBlock> WALL_WITH_CABLE = WallWithCableBlock.MAP;
    public static final CabledDataProviderBlock ITEM_COUNTER = register("item_counter", new CabledDataProviderBlock(AbstractBlock.Settings.copy(SPLITTER)));

    public static final RedstoneInputBlock REDSTONE_INPUT = register("redstone_input", new RedstoneInputBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final RedstoneOutputBlock REDSTONE_OUTPUT = register("redstone_output", new RedstoneOutputBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final ItemReaderBlock ITEM_READER = register("item_reader", new ItemReaderBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final BlockObserverBlock BLOCK_OBSERVER = register("block_observer", new BlockObserverBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final ArithmeticOperatorBlock ARITHMETIC_OPERATOR = register("arithmetic_operator",
            new ArithmeticOperatorBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));

    public static final DataMemoryBlock DATA_MEMORY = register("data_memory",
            new DataMemoryBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));

    public static final HologramProjectorBlock HOLOGRAM_PROJECTOR = register("hologram_projector", new HologramProjectorBlock(AbstractBlock.Settings.copy(SPLITTER)));
    public static final NixieTubeBlock NIXIE_TUBE = register("nixie_tube", new NixieTubeBlock(Block.Settings.copy(Blocks.GLASS).nonOpaque()));

    public static final NixieTubeControllerBlock NIXIE_TUBE_CONTROLLER = register("nixie_tube_controller", new NixieTubeControllerBlock(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_RECEIVER = register("wireless_redstone_receiver", new WirelessRedstoneBlock.Receiver(AbstractBlock.Settings.copy(ITEM_COUNTER)));
    public static final WirelessRedstoneBlock WIRELESS_REDSTONE_TRANSMITTER = register("wireless_redstone_transmitter", new WirelessRedstoneBlock.Transmitter(AbstractBlock.Settings.copy(ITEM_COUNTER)));

    public static final RotationMeterBlock TACHOMETER = register("tachometer", new RotationMeterBlock.Speed(Block.Settings.create().hardness(2).nonOpaque()));
    public static final RotationMeterBlock STRESSOMETER = register("stressometer", new RotationMeterBlock.Stress(Block.Settings.create().hardness(2).nonOpaque()));
    public static final ElectricMotorBlock ELECTRIC_MOTOR = register("electric_motor", new ElectricMotorBlock(Block.Settings.create().hardness(2).nonOpaque()));
    public static final ElectricGeneratorBlock ELECTRIC_GENERATOR = register("electric_generator", new ElectricGeneratorBlock(Block.Settings.create().hardness(2).nonOpaque()));
    public static final WorkbenchBlock WORKBENCH = register("workbench", new WorkbenchBlock(Block.Settings.copy(Blocks.CRAFTING_TABLE).nonOpaque()));

    public static final CreativeMotorBlock CREATIVE_MOTOR = register("creative_motor", new CreativeMotorBlock(AbstractBlock.Settings.create().strength(-1, -1).nonOpaque().dropsNothing()));
    public static final CreativeContainerBlock CREATIVE_CONTAINER = register("creative_container", new CreativeContainerBlock(AbstractBlock.Settings.create().strength(-1, -1).nonOpaque().dropsNothing()));
    public static final InvertedRedstoneLampBlock INVERTED_REDSTONE_LAMP = register("inverted_redstone_lamp",
            new InvertedRedstoneLampBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_LAMP).luminance((state) -> {
                return (Boolean)state.get(Properties.LIT) ? 0 : 15;
            })));
    public static final LampBlock LAMP = register("colored_lamp", new LampBlock(Block.Settings.copy(Blocks.REDSTONE_LAMP).nonOpaque(), false));
    public static final LampBlock INVERTED_LAMP = register("inverted_colored_lamp", new LampBlock(Block.Settings.copy(INVERTED_REDSTONE_LAMP).nonOpaque(), true));

    public static final SmallLampBlock CAGED_LAMP = register("caged_lamp", new SmallLampBlock(Block.Settings.copy(Blocks.REDSTONE_LAMP).nonOpaque(), false));
    public static final SmallLampBlock INVERTED_CAGED_LAMP = register("inverted_caged_lamp", new SmallLampBlock(Block.Settings.copy(INVERTED_REDSTONE_LAMP).nonOpaque(), true));
    public static final PolymerButtonBlock STEEL_BUTTON = register("steel_button", new PolymerButtonBlock("steel", BlockSetType.IRON, 5, Block.Settings.copy(Blocks.STONE_BUTTON).nonOpaque()));
    public static final TinyPotatoSpringBlock TINY_POTATO_SPRING = register("tiny_potato_spring", new TinyPotatoSpringBlock(AbstractBlock.Settings.create().strength(1).nonOpaque()));
    public static final RotationalDebugBlock ROTATION_DEBUG = register("rot_debug", new RotationalDebugBlock(AbstractBlock.Settings.create().strength(-1, -1)));
    public static final PipeBlock PIPE = register("pipe", new PipeBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final FilteredPipeBlock FILTERED_PIPE = register("filtered_pipe", new FilteredPipeBlock(AbstractBlock.Settings.copy(PIPE).nonOpaque()));
    public static final RedstoneValvePipeBlock REDSTONE_VALVE_PIPE = register("redstone_valve_pipe", new RedstoneValvePipeBlock(AbstractBlock.Settings.copy(PIPE).nonOpaque()));
    public static final Map<Block, PipeInWallBlock> WALL_WITH_PIPE = PipeInWallBlock.MAP;

    public static final PumpBlock PUMP = register("pump", new PumpBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final NozzleBlock NOZZLE = register("nozzle", new NozzleBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final DrainBlock DRAIN = register("drain", new DrainBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final MDrainBlock MECHANICAL_DRAIN = register("mechanical_drain", new MDrainBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final MSpoutBlock MECHANICAL_SPOUT = register("mechanical_spout", new MSpoutBlock(AbstractBlock.Settings.copy(SPLITTER).nonOpaque()));
    public static final CreativeDrainBlock CREATIVE_DRAIN = register("creative_drain", new CreativeDrainBlock(AbstractBlock.Settings.copy(DRAIN).dropsNothing().strength(-1)));
    public static final FluidTankBlock FLUID_TANK = register("fluid_tank", new FluidTankBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));
    public static final PortableFluidTankBlock PORTABLE_FLUID_TANK = register("portable_fluid_tank", new PortableFluidTankBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.ORANGE).strength(2.0F).nonOpaque().sounds(BlockSoundGroup.COPPER).pistonBehavior(PistonBehavior.DESTROY)));

    public static void register() {
        var s = System.currentTimeMillis();
        for (var block : Registries.BLOCK) {
            if (block instanceof WallBlock wallBlock) {
                var id = Registries.BLOCK.getId(wallBlock);
                register("wall_with_cable/" + id.getNamespace() + "/" + id.getPath(), new WallWithCableBlock(wallBlock));
                register("wall_with_pipe/" + id.getNamespace() + "/" + id.getPath(), new PipeInWallBlock(wallBlock));
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
            var lt = server.getReloadableRegistries().getLootTable(block.getLootTableKey());
            if (lt == LootTable.EMPTY) {
                ModInit.LOGGER.warn("Missing loot table? " + block.getLootTableKey().getValue());
            }
            if (block instanceof BlockEntityProvider provider) {
                var be = provider.createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
                if (be != null) {
                     assert be.getType().supports(block.getDefaultState());
                }
            }
        }
    }

    public static <T extends Block> T register(String path, T item) {
        BLOCKS.add(item);
        return Registry.register(Registries.BLOCK, Identifier.of(ModInit.ID, path), item);
    }
}
