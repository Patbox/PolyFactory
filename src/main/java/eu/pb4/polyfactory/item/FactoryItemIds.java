package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.FactoryBlockIds;
import eu.pb4.polyfactory.item.tool.SpoutMolds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryItemIds {
    // Util
    public static final ResourceKey<Item> FLUID_MODEL = of("fluid_model");
    // Actual items
    public static final ResourceKey<Item> WRENCH = of("wrench");
    public static final ResourceKey<Item> GUIDE_BOOK = of("guidebook");
    public static final ResourceKey<Item> CLIPBOARD = of("clipboard");
    public static final ResourceKey<Item> CONVEYOR = of(FactoryBlockIds.CONVEYOR);
    public static final ResourceKey<Item> STICKY_CONVEYOR = of(FactoryBlockIds.STICKY_CONVEYOR);
    public static final ResourceKey<Item> FUNNEL = of(FactoryBlockIds.FUNNEL);
    public static final ResourceKey<Item> SLOT_AWARE_FUNNEL = of(FactoryBlockIds.SLOT_AWARE_FUNNEL);
    public static final ResourceKey<Item> SPLITTER = of(FactoryBlockIds.SPLITTER);
    public static final ResourceKey<Item> FAN = of(FactoryBlockIds.FAN);
    public static final ResourceKey<Item> EJECTOR = of(FactoryBlockIds.EJECTOR);
    public static final ResourceKey<Item> HAND_CRANK = of(FactoryBlockIds.HAND_CRANK);
    public static final ResourceKey<Item> STEAM_ENGINE = of(FactoryBlockIds.STEAM_ENGINE);
    public static final ResourceKey<Item> SMELTERY_CORE = of(FactoryBlockIds.SMELTERY_CORE);
    public static final ResourceKey<Item> SMELTERY = of(FactoryBlockIds.SMELTERY);
    public static final ResourceKey<Item> PRIMITIVE_SMELTERY = of(FactoryBlockIds.PRIMITIVE_SMELTERY);
    public static final ResourceKey<Item> CASTING_TABLE = of(FactoryBlockIds.CASTING_TABLE);
    public static final ResourceKey<Item> SMELTERY_FAUCET = of(FactoryBlockIds.FAUCET);
    public static final ResourceKey<Item> GRINDER = of(FactoryBlockIds.GRINDER);
    public static final ResourceKey<Item> PRESS = of(FactoryBlockIds.PRESS);
    public static final ResourceKey<Item> CRAFTER = of(FactoryBlockIds.CRAFTER);
    public static final ResourceKey<Item> MIXER = of(FactoryBlockIds.MIXER);
    public static final ResourceKey<Item> FERMENTER = of(FactoryBlockIds.FERMENTER);
    public static final ResourceKey<Item> MINER = of(FactoryBlockIds.MINER);
    public static final ResourceKey<Item> PLACER = of(FactoryBlockIds.PLACER);
    public static final ResourceKey<Item> PLANTER = of(FactoryBlockIds.PLANTER);
    public static final ResourceKey<Item> AXLE = of(FactoryBlockIds.AXLE);
    public static final ResourceKey<Item> CHAIN_DRIVE = of(FactoryBlockIds.CHAIN_DRIVE);
    public static final ResourceKey<Item> TURNTABLE = of(FactoryBlockIds.TURNTABLE);
    public static final ResourceKey<Item> GEARBOX = of(FactoryBlockIds.GEARBOX);
    public static final ResourceKey<Item> CLUTCH = of(FactoryBlockIds.CLUTCH);
    public static final ResourceKey<Item> GEARSHIFT = of(FactoryBlockIds.GEARSHIFT);
    public static final ResourceKey<Item> CONTAINER = of( FactoryBlockIds.WOODEN_CONTAINER);
    public static final ResourceKey<Item> DEEP_STORAGE_CONTAINER = of( FactoryBlockIds.DEEP_STORAGE_CONTAINER);
    public static final ResourceKey<Item> ITEM_OUTPUT_BUFFER = of(FactoryBlockIds.ITEM_OUTPUT_BUFFER);
    public static final ResourceKey<Item> NIXIE_TUBE = of(FactoryBlockIds.NIXIE_TUBE);
    public static final ResourceKey<Item> WINDMILL_SAIL = of("windmill_sail");
    public static final ResourceKey<Item> METAL_GRID = of(FactoryBlockIds.METAL_GRID);
    public static final ResourceKey<Item> STRING_MESH = of("string_mesh");
    public static final ResourceKey<Item> SAW_DUST = of("saw_dust");
    public static final ResourceKey<Item> COAL_DUST = of("coal_dust");
    public static final ResourceKey<Item> NETHERRACK_DUST = of("netherrack_dust");
    public static final ResourceKey<Item> SULFUR_DUST = of("sulfur_dust");
    public static final ResourceKey<Item> ENDER_DUST = of("ender_dust");
    public static final ResourceKey<Item> ENDER_INFUSED_AMETHYST_SHARD = of("ender_infused_amethyst_shard");
    public static final ResourceKey<Item> STEEL_ALLOY_MIXTURE = of("steel_alloy_mixture");
    public static final ResourceKey<Item> STEEL_INGOT = of("steel_ingot");
    public static final ResourceKey<Item> STEEL_NUGGET = of("steel_nugget");
    public static final ResourceKey<Item> STEEL_BLOCK = of(FactoryBlockIds.STEEL_BLOCK);
    public static final ResourceKey<Item> STEEL_PLATE = of("steel_plate");
    public static final ResourceKey<Item> COPPER_PLATE = of("copper_plate");
    public static final ResourceKey<Item> BRITTLE_GLASS_BOTTLE = of("brittle_glass_bottle");
    public static final ResourceKey<Item> BRITTLE_POTION = of("brittle_potion");
    public static final ResourceKey<Item> THROWABLE_GLASS_BOTTLE = of("throwable_glass_bottle");
    public static final ResourceKey<Item> LINGERING_THROWABLE_GLASS_BOTTLE = of("lingering_throwable_glass_bottle");
    public static final ResourceKey<Item> STEEL_GEAR = of("steel_gear");
    public static final ResourceKey<Item> LARGE_STEEL_GEAR = of("large_steel_gear");
    public static final ResourceKey<Item> STEEL_MACHINE_GEARBOX = of("generic_machine_part");
    public static final ResourceKey<Item> WOODEN_PLATE = of("wooden_plate");
    public static final ResourceKey<Item> TREATED_DRIED_KELP = of("treated_dried_kelp");
    public static final ResourceKey<Item> INTEGRATED_CIRCUIT = of("integrated_circuit");
    public static final ResourceKey<Item> REDSTONE_CHIP = of("redstone_chip");
    public static final ResourceKey<Item> CHAIN_LIFT = of("chain_lift");

    public static final ResourceKey<Item> ITEM_FILTER = of("item_filter");

    public static final ResourceKey<Item> CREATIVE_MOTOR = of(FactoryBlockIds.CREATIVE_MOTOR);
    public static final ResourceKey<Item> CREATIVE_CONTAINER = of(FactoryBlockIds.CREATIVE_CONTAINER);
    public static final ResourceKey<Item> TACHOMETER = of(FactoryBlockIds.TACHOMETER);
    public static final ResourceKey<Item> STRESSOMETER = of(FactoryBlockIds.STRESSOMETER);
    public static final ResourceKey<Item> ITEM_COUNTER = of(FactoryBlockIds.ITEM_COUNTER);
    public static final ResourceKey<Item> REDSTONE_INPUT = of(FactoryBlockIds.REDSTONE_INPUT);
    public static final ResourceKey<Item> REDSTONE_OUTPUT = of(FactoryBlockIds.REDSTONE_OUTPUT);
    public static final ResourceKey<Item> SPEAKER = of(FactoryBlockIds.SPEAKER);
    public static final ResourceKey<Item> RECORD_PLAYER = of(FactoryBlockIds.RECORD_PLAYER);
    public static final ResourceKey<Item> ITEM_READER = of(FactoryBlockIds.ITEM_READER);
    public static final ResourceKey<Item> BLOCK_OBSERVER = of(FactoryBlockIds.BLOCK_OBSERVER);
    public static final ResourceKey<Item> TEXT_INPUT = of(FactoryBlockIds.TEXT_INPUT);
    public static final ResourceKey<Item> DIGITAL_CLOCK = of(FactoryBlockIds.DIGITAL_CLOCK);
    public static final ResourceKey<Item> ARITHMETIC_OPERATOR = of(FactoryBlockIds.ARITHMETIC_OPERATOR);
    public static final ResourceKey<Item> DATA_COMPARATOR = of(FactoryBlockIds.DATA_COMPARATOR);
    public static final ResourceKey<Item> DATA_EXTRACTOR = of(FactoryBlockIds.DATA_EXTRACTOR);
    public static final ResourceKey<Item> PROGRAMMABLE_DATA_EXTRACTOR = of(FactoryBlockIds.PROGRAMMABLE_DATA_EXTRACTOR);
    public static final ResourceKey<Item> DATA_MEMORY = of("data_memory");
    public static final ResourceKey<Item> NIXIE_TUBE_CONTROLLER = of(FactoryBlockIds.NIXIE_TUBE_CONTROLLER);
    public static final ResourceKey<Item> GAUGE = of(FactoryBlockIds.GAUGE);
    public static final ResourceKey<Item> HOLOGRAM_PROJECTOR = of(FactoryBlockIds.HOLOGRAM_PROJECTOR);
    public static final ResourceKey<Item> WIRELESS_REDSTONE_RECEIVER = of(FactoryBlockIds.WIRELESS_REDSTONE_RECEIVER);
    public static final ResourceKey<Item> WIRELESS_REDSTONE_TRANSMITTER = of(FactoryBlockIds.WIRELESS_REDSTONE_TRANSMITTER);
    public static final ResourceKey<Item> PORTABLE_REDSTONE_TRANSMITTER = of("portable_redstone_transmitter");

    public static final ResourceKey<Item> PUNCH_CARD = of("punch_card");

    public static final ResourceKey<Item> CABLE = of("cable");
    public static final ResourceKey<Item> GATED_CABLE = of(FactoryBlockIds.GATED_CABLE);
    public static final ResourceKey<Item> LAMP = of(FactoryBlockIds.COLORED_LAMP);
    public static final ResourceKey<Item> INVERTED_LAMP = of(FactoryBlockIds.INVERTED_COLORED_LAMP);
    public static final ResourceKey<Item> CAGED_LAMP = of(FactoryBlockIds.CAGED_LAMP);
    public static final ResourceKey<Item> INVERTED_CAGED_LAMP = of(FactoryBlockIds.INVERTED_CAGED_LAMP);
    public static final ResourceKey<Item> FIXTURE_LAMP = of(FactoryBlockIds.FIXTURE_LAMP);
    public static final ResourceKey<Item> INVERTED_FIXTURE_LAMP = of(FactoryBlockIds.INVERTED_FIXTURE_LAMP);
    public static final ResourceKey<Item> STEEL_BUTTON = of(FactoryBlockIds.STEEL_BUTTON);
    public static final ResourceKey<Item> ELECTRIC_MOTOR = of(FactoryBlockIds.ELECTRIC_MOTOR);
    public static final ResourceKey<Item> ELECTRIC_GENERATOR = of(FactoryBlockIds.ELECTRIC_GENERATOR);
    public static final ResourceKey<Item> WORKBENCH = of(FactoryBlockIds.WORKBENCH);
    public static final ResourceKey<Item> BLUEPRINT_WORKBENCH = of(FactoryBlockIds.BLUEPRINT_WORKBENCH);
    public static final ResourceKey<Item> MOLDMAKING_TABLE = of(FactoryBlockIds.MOLDMAKING_TABLE);
    public static final ResourceKey<Item> ARTIFICIAL_DYE = of("artificial_dye");
    public static final ResourceKey<Item> DYNAMITE = of("dynamite");
    public static final ResourceKey<Item> STICKY_DYNAMITE = of("sticky_dynamite");
    public static final ResourceKey<Item> INVERTED_REDSTONE_LAMP = of(FactoryBlockIds.INVERTED_REDSTONE_LAMP);
    public static final ResourceKey<Item> TINY_POTATO_SPRING = of(FactoryBlockIds.TINY_POTATO_SPRING);
    public static final ResourceKey<Item> GOLDEN_TINY_POTATO_SPRING = of(FactoryBlockIds.GOLDEN_TINY_POTATO_SPRING);
    public static final ResourceKey<Item> EXPERIENCE_BUCKET = of("experience_bucket");
    public static final ResourceKey<Item> SLIME_BUCKET = of("slime_bucket");
    public static final ResourceKey<Item> HONEY_BUCKET = of("honey_bucket");
    public static final ResourceKey<Item> PLANT_OIL_BUCKET = of("plant_oil_bucket");
    public static final ResourceKey<Item> ETHANOL_BUCKET = of("ethanol_bucket");
    public static final ResourceKey<Item> BIODIESEL_BUCKET = of("biodiesel_bucket");

    public static final ResourceKey<Item> BIOMASS = of("biomass");

    public static final ResourceKey<Item> CRISPY_HONEY = of("crispy_honey");
    public static final ResourceKey<Item> HONEYED_APPLE = of("honeyed_apple");

    public static final ResourceKey<Item> CRUSHED_RAW_IRON = of("crushed_raw_iron");
    public static final ResourceKey<Item> CRUSHED_RAW_COPPER = of("crushed_raw_copper");
    public static final ResourceKey<Item> CRUSHED_RAW_GOLD = of("crushed_raw_gold");

    public static final ResourceKey<Item> RAW_IRON_NUGGET = of("raw_iron_nugget");
    public static final ResourceKey<Item> RAW_COPPER_NUGGET = of("raw_copper_nugget");
    public static final ResourceKey<Item> RAW_GOLD_NUGGET = of("raw_gold_nugget");

    public static final ResourceKey<Item> SPRAY_CAN = of("spray_can");

    public static final ResourceKey<Item> PORTABLE_DRILL = of("portable_drill");
    public static final ResourceKey<Item> CREATIVE_PORTABLE_DRILL = of("creative_portable_drill");

    public static final ResourceKey<Item> COPPER_DRILL_HEAD = of("copper_drill_head");
    public static final ResourceKey<Item> IRON_DRILL_HEAD = of("iron_drill_head");
    public static final ResourceKey<Item> GOLDEN_DRILL_HEAD = of("golden_drill_head");
    public static final ResourceKey<Item> DIAMOND_DRILL_HEAD = of("diamond_drill_head");
    public static final ResourceKey<Item> NETHERITE_DRILL_HEAD = of("netherite_drill_head");

    public static final ResourceKey<Item> PIPE = of("pipe");
    public static final ResourceKey<Item> FILTERED_PIPE = of(FactoryBlockIds.FILTERED_PIPE);
    public static final ResourceKey<Item> REDSTONE_VALVE_PIPE = of(FactoryBlockIds.REDSTONE_VALVE_PIPE);
    public static final ResourceKey<Item> PUMP = of(FactoryBlockIds.PUMP);
    public static final ResourceKey<Item> NOZZLE = of(FactoryBlockIds.NOZZLE);
    public static final ResourceKey<Item> DRAIN = of(FactoryBlockIds.DRAIN);
    public static final ResourceKey<Item> MECHANICAL_DRAIN = of(FactoryBlockIds.MECHANICAL_DRAIN);
    public static final ResourceKey<Item> MECHANICAL_SPOUT = of(FactoryBlockIds.MECHANICAL_SPOUT);
    public static final ResourceKey<Item> CREATIVE_DRAIN = of(FactoryBlockIds.CREATIVE_DRAIN);
    public static final ResourceKey<Item> FLUID_TANK = of(FactoryBlockIds.FLUID_TANK);
    public static final ResourceKey<Item> PORTABLE_FLUID_TANK = of(FactoryBlockIds.PORTABLE_FLUID_TANK);

    public static final ResourceKey<Item> PRESSURE_FLUID_GUN = of("pressure_fluid_gun");

    public static final ResourceKey<Item> ITEM_PACKER = of(FactoryBlockIds.ITEM_PACKER);

    public static final SpoutMolds<ResourceKey<Item>> INGOT_MOLD = SpoutMolds.createIds("ingot");
    public static final SpoutMolds<ResourceKey<Item>> NUGGET_MOLD = SpoutMolds.createIds("nugget");
    public static final SpoutMolds<ResourceKey<Item>> PIPE_MOLD = SpoutMolds.createIds("pipe");
    public static final SpoutMolds<ResourceKey<Item>> BOTTLE_MOLD = SpoutMolds.createIds("bottle");
    public static final SpoutMolds<ResourceKey<Item>> THROWABLE_BOTTLE_MOLD = SpoutMolds.createIds("throwable_bottle");
    public static final SpoutMolds<ResourceKey<Item>> BRITTLE_BOTTLE_MOLD = SpoutMolds.createIds("brittle_bottle");
    public static final SpoutMolds<ResourceKey<Item>> CHAIN_MOLD = SpoutMolds.createIds("chain");
    public static final SpoutMolds<ResourceKey<Item>> DRILL_HEAD_MOLD = SpoutMolds.createIds("drill_head");

    public static final List<SpoutMolds<ResourceKey<Item>>> MOLDS = List.of(
        INGOT_MOLD, NUGGET_MOLD, PIPE_MOLD, BOTTLE_MOLD, THROWABLE_BOTTLE_MOLD, BRITTLE_BOTTLE_MOLD, CHAIN_MOLD, DRILL_HEAD_MOLD
    );


    public static  ResourceKey<Item> of(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    public static  ResourceKey<Item> of(Identifier id) {
        return ResourceKey.create(Registries.ITEM, id);
    }

    public static  ResourceKey<Item> of(ResourceKey<Block> key) {
        return key.dependent(Registries.ITEM, "");
    }
}
