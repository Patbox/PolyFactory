package eu.pb4.polyfactory.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;

import java.util.ArrayList;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

// ,([a-zA-Z \->\(\)\._,\:0-9?"\n]*);

public class FactoryBlockIds {
    private static final List<Block> BLOCKS = new ArrayList<>();
    public static final ResourceKey<Block> CONVEYOR = of("conveyor");
    public static final ResourceKey<Block> STICKY_CONVEYOR = of("sticky_conveyor");
    public static final ResourceKey<Block> FUNNEL = of("funnel");
    public static final ResourceKey<Block> SLOT_AWARE_FUNNEL = of("slot_aware_funnel");
    public static final ResourceKey<Block> SPLITTER = of("splitter");
    public static final ResourceKey<Block> FAN = of("fan");
    public static final ResourceKey<Block> EJECTOR = of("ejector");
    public static final ResourceKey<Block> METAL_GRID = of("metal_grid");
    public static final ResourceKey<Block> HAND_CRANK = of("hand_crank");
    public static final ResourceKey<Block> STEAM_ENGINE = of("steam_engine");
    public static final ResourceKey<Block> GRINDER = of("grinder");
    public static final ResourceKey<Block> PRESS = of("press");
    public static final ResourceKey<Block> MIXER = of("mixer");
    public static final ResourceKey<Block> FERMENTER = of("fermenter");
    public static final ResourceKey<Block> CRAFTER = of("crafter");
    public static final ResourceKey<Block> MINER = of("miner");
    public static final ResourceKey<Block> PLACER = of("placer");
    public static final ResourceKey<Block> PLANTER = of("planter");
    public static final ResourceKey<Block> AXLE = of("axle");
    public static final ResourceKey<Block> CHAIN_DRIVE = of("chain_drive");
    public static final ResourceKey<Block> AXLE_WITH_GEAR = of("axle_with_gear");
    public static final ResourceKey<Block> AXLE_WITH_LARGE_GEAR = of("axle_with_large_gear");
    public static final ResourceKey<Block> TURNTABLE = of("turntable");
    public static final ResourceKey<Block> GEARBOX = of("gearbox");
    public static final ResourceKey<Block> CLUTCH = of("clutch");
    public static final ResourceKey<Block> GEARSHIFT = of("gearshift");
    public static final ResourceKey<Block> WINDMILL = of("windmill");
    public static final ResourceKey<Block> WOODEN_CONTAINER = of("wooden_container");
    public static final ResourceKey<Block> DEEP_STORAGE_CONTAINER = of("deep_storage_container");
    public static final ResourceKey<Block> ITEM_OUTPUT_BUFFER = of("item_output_buffer");
    public static final ResourceKey<Block> ITEM_PACKER = of("item_packer");
    public static final ResourceKey<Block> CABLE = of("cable");
    public static final ResourceKey<Block> GATED_CABLE = of("gated_cable");
    //public static final Map<Block, WallWithCableBlock> WALL_WITH_CABLE = WallWithCableBlock.MAP;
    public static final ResourceKey<Block> ITEM_COUNTER = of("item_counter");

    public static final ResourceKey<Block> REDSTONE_INPUT = of("redstone_input");
    public static final ResourceKey<Block> REDSTONE_OUTPUT = of("redstone_output");
    public static final ResourceKey<Block> SPEAKER = of("speaker");
    public static final ResourceKey<Block> RECORD_PLAYER = of("record_player");
    public static final ResourceKey<Block> ITEM_READER = of("item_reader");
    public static final ResourceKey<Block> BLOCK_OBSERVER = of("block_observer");
    public static final ResourceKey<Block> TEXT_INPUT = of("text_input");
    public static final ResourceKey<Block> DIGITAL_CLOCK = of("digital_clock");
    public static final ResourceKey<Block> ARITHMETIC_OPERATOR = of("arithmetic_operator");

    public static final ResourceKey<Block> DATA_COMPARATOR = of("data_comparator");
    public static final ResourceKey<Block> DATA_EXTRACTOR = of("data_extractor");
    public static final ResourceKey<Block> PROGRAMMABLE_DATA_EXTRACTOR = of("programmable_data_extractor");
    public static final ResourceKey<Block> DATA_MEMORY = of("data_memory");

    public static final ResourceKey<Block> GAUGE = of("gauge");
    public static final ResourceKey<Block> HOLOGRAM_PROJECTOR = of("hologram_projector");
    public static final ResourceKey<Block> NIXIE_TUBE = of("nixie_tube");

    public static final ResourceKey<Block> NIXIE_TUBE_CONTROLLER = of("nixie_tube_controller");
    public static final ResourceKey<Block> WIRELESS_REDSTONE_RECEIVER = of("wireless_redstone_receiver");
    public static final ResourceKey<Block> WIRELESS_REDSTONE_TRANSMITTER = of("wireless_redstone_transmitter");

    public static final ResourceKey<Block> TACHOMETER = of("tachometer");
    public static final ResourceKey<Block> STRESSOMETER = of("stressometer");
    public static final ResourceKey<Block> ELECTRIC_MOTOR = of("electric_motor");
    public static final ResourceKey<Block> ELECTRIC_GENERATOR = of("electric_generator");
    public static final ResourceKey<Block> WORKBENCH = of("workbench");
    public static final ResourceKey<Block> BLUEPRINT_WORKBENCH = of("blueprint_workbench");
    public static final ResourceKey<Block> MOLDMAKING_TABLE = of("moldmaking_table");
    public static final ResourceKey<Block> CREATIVE_MOTOR = of("creative_motor");
    public static final ResourceKey<Block> CREATIVE_CONTAINER = of("creative_container");
    public static final ResourceKey<Block> INVERTED_REDSTONE_LAMP = of("inverted_redstone_lamp");
    public static final ResourceKey<Block> COLORED_LAMP = of("colored_lamp");
    public static final ResourceKey<Block> INVERTED_COLORED_LAMP = of("inverted_colored_lamp");

    public static final ResourceKey<Block> CAGED_LAMP = of("caged_lamp");
    public static final ResourceKey<Block> INVERTED_CAGED_LAMP = of("inverted_caged_lamp");
    public static final ResourceKey<Block> FIXTURE_LAMP = of("fixture_lamp");
    public static final ResourceKey<Block> INVERTED_FIXTURE_LAMP = of("inverted_fixture_lamp");
    public static final ResourceKey<Block> STEEL_BUTTON = of("steel_button");
    public static final ResourceKey<Block> TINY_POTATO_SPRING = of("tiny_potato_spring");
    public static final ResourceKey<Block> GOLDEN_TINY_POTATO_SPRING = of("golden_tiny_potato_spring");

    public static final ResourceKey<Block> CHEESE_WHEEL = of("cheese_wheel");

    public static final ResourceKey<Block> ROTATION_DEBUG = of("rot_debug");
    public static final ResourceKey<Block> PIPE = of("pipe");
    public static final ResourceKey<Block> FILTERED_PIPE = of("filtered_pipe");
    public static final ResourceKey<Block> REDSTONE_VALVE_PIPE = of("redstone_valve_pipe");
    //public static final Map<Block, PipeInWallBlock> WALL_WITH_PIPE = PipeInWallBlock.MAP;
    public static final ResourceKey<Block> SMELTERY_CORE = of("smeltery_core");

    public static final ResourceKey<Block> SMELTERY = of("smeltery");
    public static final ResourceKey<Block> PRIMITIVE_SMELTERY = of("primitive_smeltery");
    public static final ResourceKey<Block> CASTING_TABLE = of("casting_table");
    public static final ResourceKey<Block> CASTING_CAULDRON = of("casting_cauldron");
    public static final ResourceKey<Block> FAUCET = of("faucet");
    public static final ResourceKey<Block> PUMP = of("pump");
    public static final ResourceKey<Block> NOZZLE = of("nozzle");
    public static final ResourceKey<Block> DRAIN = of("drain");
    public static final ResourceKey<Block> MECHANICAL_DRAIN = of("mechanical_drain");
    public static final ResourceKey<Block> MECHANICAL_SPOUT = of("mechanical_spout");
    public static final ResourceKey<Block> CREATIVE_DRAIN = of("creative_drain");
    public static final ResourceKey<Block> FLUID_TANK = of("fluid_tank");
    public static final ResourceKey<Block> PORTABLE_FLUID_TANK = of("portable_fluid_tank");

    public static final ResourceKey<Block> STEEL_BLOCK = of("steel_block");
    public static final ResourceKey<Block> TPS_PROVIDER = of("tps_provider");

    public static  ResourceKey<Block> of(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }
}
