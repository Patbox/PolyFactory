package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.factorytools.api.item.MultiBlockItem;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.block.*;
import eu.pb4.polyfactory.item.debug.BaseDebugItem;
import eu.pb4.polyfactory.item.tool.*;
import eu.pb4.polyfactory.item.util.*;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.wrench.WrenchItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class FactoryItems {
    public static final Item MOD_ICON = register("mod_icon", new ModeledItem(new Item.Settings()));
    public static final WrenchItem WRENCH = register("wrench", new WrenchItem());
    public static final Item CONVEYOR = register(FactoryBlocks.CONVEYOR);
    public static final Item STICKY_CONVEYOR = register(FactoryBlocks.STICKY_CONVEYOR);
    public static final Item FUNNEL = register(FactoryBlocks.FUNNEL);
    public static final Item SPLITTER = register(FactoryBlocks.SPLITTER);
    public static final Item FAN = register(FactoryBlocks.FAN);
    public static final Item HAND_CRANK = register(FactoryBlocks.HAND_CRANK);
    public static final Item STEAM_ENGINE = register(FactoryBlocks.STEAM_ENGINE);
    public static final Item GRINDER = register(FactoryBlocks.GRINDER);
    public static final Item PRESS = register(FactoryBlocks.PRESS);
    public static final Item CRAFTER = register(FactoryBlocks.CRAFTER);
    public static final Item MIXER = register(FactoryBlocks.MIXER);
    public static final Item MINER = register(FactoryBlocks.MINER);
    public static final Item PLACER = register(FactoryBlocks.PLACER);
    public static final Item PLANTER = register(FactoryBlocks.PLANTER);
    public static final FactoryBlockItem AXLE = register(FactoryBlocks.AXLE);
    public static final Item TURNTABLE = register(FactoryBlocks.TURNTABLE);
    public static final Item GEARBOX = register(FactoryBlocks.GEARBOX);
    public static final Item CLUTCH = register(FactoryBlocks.CLUTCH);
    public static final Item CONTAINER = register( FactoryBlocks.CONTAINER);
    public static final Item NIXIE_TUBE = register(FactoryBlocks.NIXIE_TUBE);
    public static final WindmillSailItem WINDMILL_SAIL = register("windmill_sail", new WindmillSailItem(new Item.Settings()));
    public static final Item METAL_GRID = register(FactoryBlocks.METAL_GRID);
    public static final Item SAW_DUST = register("saw_dust", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item COAL_DUST = register("coal_dust", new ModeledItem(Items.COAL, new Item.Settings()));
    public static final Item NETHERRACK_DUST = register("netherrack_dust", new ModeledItem(new Item.Settings()));
    public static final Item ENDER_DUST = register("ender_dust", new ModeledItem(new Item.Settings()));
    public static final Item ENDER_INFUSED_AMETHYST_SHARD = register("ender_infused_amethyst_shard", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_INGOT = register("steel_ingot", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_PLATE = register("steel_plate", new ModeledItem(new Item.Settings()));
    public static final Item COPPER_PLATE = register("copper_plate", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_GEAR = register("steel_gear", new GearItem(FactoryBlocks.AXLE_WITH_GEAR, new Item.Settings()));
    public static final Item LARGE_STEEL_GEAR = register("large_steel_gear", new GearItem(FactoryBlocks.AXLE_WITH_LARGE_GEAR, new Item.Settings()));
    public static final Item GENERIC_MACHINE_PART = register("generic_machine_part", new ModeledItem(new Item.Settings()));
    public static final Item WOODEN_PLATE = register("wooden_plate", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", new ModeledItem(new Item.Settings()));
    public static final Item INTEGRATED_CIRCUIT = register("integrated_circuit", new ModeledItem(new Item.Settings()));
    public static final Item REDSTONE_CHIP = register("redstone_chip", new ModeledItem(new Item.Settings()));

    public static final Item ITEM_FILTER = register("item_filter", new FilterItem(Items.PAPER, new Item.Settings()));

    public static final Item CREATIVE_MOTOR = register(FactoryBlocks.CREATIVE_MOTOR);
    public static final Item CREATIVE_CONTAINER = register(FactoryBlocks.CREATIVE_CONTAINER);
    public static final Item TACHOMETER = register(FactoryBlocks.TACHOMETER);
    public static final Item STRESSOMETER = register(FactoryBlocks.STRESSOMETER);
    public static final Item ITEM_COUNTER = register(FactoryBlocks.ITEM_COUNTER);
    public static final Item REDSTONE_INPUT = register(FactoryBlocks.REDSTONE_INPUT);
    public static final Item REDSTONE_OUTPUT = register(FactoryBlocks.REDSTONE_OUTPUT);
    public static final Item ITEM_READER = register(FactoryBlocks.ITEM_READER);
    public static final Item BLOCK_OBSERVER = register(FactoryBlocks.BLOCK_OBSERVER);
    public static final Item ARITHMETIC_OPERATOR = register(FactoryBlocks.ARITHMETIC_OPERATOR);
    public static final Item DATA_MEMORY = register("data_memory", new DataMemoryBlockItem(FactoryBlocks.DATA_MEMORY, new Item.Settings()));
    public static final Item NIXIE_TUBE_CONTROLLER = register(FactoryBlocks.NIXIE_TUBE_CONTROLLER);
    public static final Item HOLOGRAM_PROJECTOR = register(FactoryBlocks.HOLOGRAM_PROJECTOR);
    public static final Item WIRELESS_REDSTONE_RECEIVER = register(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
    public static final Item WIRELESS_REDSTONE_TRANSMITTER = register(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
    public static final Item PORTABLE_REDSTONE_TRANSMITTER = register("portable_redstone_transmitter",
            new WirelessRedstoneTransmitterItem(new Item.Settings().maxCount(1)
                    .component(FactoryDataComponents.REMOTE_KEYS, new Pair<>(ItemStack.EMPTY, ItemStack.EMPTY))));

    public static final CableItem CABLE = register("cable", new CableItem(FactoryBlocks.CABLE, AbstractCableBlock.DEFAULT_COLOR, new Item.Settings()));
    public static final ColoredDownsampledBlockItem LAMP = registerColored(FactoryBlocks.LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_LAMP = registerColored(FactoryBlocks.INVERTED_LAMP, -1);
    public static final ColoredDownsampledBlockItem CAGED_LAMP = registerColored(FactoryBlocks.CAGED_LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_CAGED_LAMP = registerColored(FactoryBlocks.INVERTED_CAGED_LAMP, -1);
    public static final Item STEEL_BUTTON = register(FactoryBlocks.STEEL_BUTTON);
    public static final Item ELECTRIC_MOTOR = register(FactoryBlocks.ELECTRIC_MOTOR);
    public static final Item ELECTRIC_GENERATOR = register(FactoryBlocks.ELECTRIC_GENERATOR);
    public static final Item WORKBENCH = register(FactoryBlocks.WORKBENCH);
    public static final Item ARTIFICIAL_DYE = register("artificial_dye", new ArtificialDyeItem(new Item.Settings()));
    public static final Item DYNAMITE = register("dynamite", new DynamiteItem(new Item.Settings().maxCount(16)));
    public static final Item INVERTED_REDSTONE_LAMP = register(FactoryBlocks.INVERTED_REDSTONE_LAMP);
    public static final Item TINY_POTATO_SPRING = register(FactoryBlocks.TINY_POTATO_SPRING);
    public static final Item EXPERIENCE_BUCKET = register("experience_bucket", new ModeledItem(new Item.Settings().maxCount(1)));
    public static final Item CRUSHED_RAW_IRON = register("crushed_raw_iron", new ModeledItem(new Item.Settings()));
    public static final Item CRUSHED_RAW_COPPER = register("crushed_raw_copper", new ModeledItem(new Item.Settings()));
    public static final Item CRUSHED_RAW_GOLD = register("crushed_raw_gold", new ModeledItem(new Item.Settings()));
    public static final Item SPRAY_CAN = register("spray_can", new DyeSprayItem(new Item.Settings().maxCount(1)));
    public static final Item PIPE = register(FactoryBlocks.PIPE);
    public static final Item PUMP = register(FactoryBlocks.PUMP);
    public static final Item DRAIN = register(FactoryBlocks.DRAIN);
    public static final Item MECHANICAL_DRAIN = register(FactoryBlocks.MECHANICAL_DRAIN);
    public static final Item MECHANICAL_SPOUT = register(FactoryBlocks.MECHANICAL_SPOUT);
    public static final Item CREATIVE_DRAIN = register(FactoryBlocks.CREATIVE_DRAIN);
    public static final UniversalFluidContainerItem STEEL_BUCKET = register("steel_bucket", new UniversalFluidContainerItem(FluidConstants.BUCKET * 2, new Item.Settings().maxCount(1)));
    public static final Item FLUID_TANK = register(FactoryBlocks.FLUID_TANK);
    public static void register() {
        FuelRegistry.INSTANCE.add(SAW_DUST, 60);
        FuelRegistry.INSTANCE.add(WOODEN_PLATE, 120);
        FuelRegistry.INSTANCE.add(COAL_DUST, 160);

        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "a_group"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(WINDMILL_SAIL::getDefaultStack)
                .displayName(Text.translatable("itemgroup." + ModInit.ID))
                .entries(((context, entries) -> {
                    entries.add(WRENCH);

                    // Rotational machines (tier 1)

                    // Rotation transmission
                    entries.add(AXLE);
                    entries.add(GEARBOX);
                    entries.add(CLUTCH);
                    entries.add(STEEL_GEAR);
                    entries.add(LARGE_STEEL_GEAR);

                    // Rotation Generation
                    entries.add(HAND_CRANK);
                    entries.add(WINDMILL_SAIL);
                    entries.add(STEAM_ENGINE);

                    // Item Movement/Storage
                    entries.add(CONVEYOR);
                    entries.add(STICKY_CONVEYOR);
                    entries.add(FAN);
                    entries.add(METAL_GRID);
                    entries.add(FUNNEL);
                    entries.add(SPLITTER);
                    entries.add(CONTAINER);
                    entries.add(ITEM_FILTER);

                    // Rotation other?
                    entries.add(TURNTABLE);

                    // Crafting/Machines
                    entries.add(WORKBENCH);
                    entries.add(GRINDER);
                    entries.add(PRESS);
                    entries.add(MIXER);
                    entries.add(CRAFTER);
                    entries.add(MINER);
                    entries.add(PLACER);
                    entries.add(PLANTER);

                    // Fluids
                    entries.add(PIPE);
                    entries.add(PUMP);
                    entries.add(DRAIN);
                    entries.add(MECHANICAL_DRAIN);
                    entries.add(MECHANICAL_SPOUT);
                    entries.add(FLUID_TANK);

                    // Data
                    entries.add(CABLE);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.RED)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.GREEN)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.BLUE)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(TACHOMETER);
                    entries.add(STRESSOMETER);
                    entries.add(REDSTONE_OUTPUT);
                    entries.add(REDSTONE_INPUT);
                    entries.add(ITEM_COUNTER);
                    entries.add(ITEM_READER);
                    entries.add(BLOCK_OBSERVER);
                    entries.add(NIXIE_TUBE_CONTROLLER);
                    entries.add(NIXIE_TUBE);
                    entries.add(HOLOGRAM_PROJECTOR);
                    entries.add(DATA_MEMORY);
                    entries.add(ARITHMETIC_OPERATOR);


                    // Redstone?
                    entries.add(WIRELESS_REDSTONE_RECEIVER);
                    entries.add(WIRELESS_REDSTONE_TRANSMITTER);
                    entries.add(PORTABLE_REDSTONE_TRANSMITTER);

                    // Electrical machines (tier 2)

                    // Rest
                    entries.add(INVERTED_REDSTONE_LAMP);
                    entries.add(ColoredItem.stack(LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(INVERTED_LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.add(STEEL_BUTTON);
                    entries.add(TINY_POTATO_SPRING);

                    // Other items
                    entries.add(DYNAMITE);
                    entries.add(SPRAY_CAN);
                    entries.add(EXPERIENCE_BUCKET);

                    // Generic Materials
                    entries.add(SAW_DUST);
                    entries.add(COAL_DUST);
                    entries.add(NETHERRACK_DUST);
                    entries.add(ENDER_DUST);
                    entries.add(CRUSHED_RAW_IRON);
                    entries.add(CRUSHED_RAW_COPPER);
                    entries.add(CRUSHED_RAW_GOLD);
                    entries.add(STEEL_ALLOY_MIXTURE);
                    entries.add(STEEL_INGOT);
                    entries.add(STEEL_PLATE);
                    entries.add(COPPER_PLATE);
                    entries.add(WOODEN_PLATE);
                    entries.add(TREATED_DRIED_KELP);
                    entries.add(ENDER_INFUSED_AMETHYST_SHARD);
                    entries.add(GENERIC_MACHINE_PART);
                    entries.add(REDSTONE_CHIP);
                    entries.add(INTEGRATED_CIRCUIT);

                    // Fancy dyes
                    entries.add(ArtificialDyeItem.of(0xFF0000));
                    entries.add(ArtificialDyeItem.of(0xFFFF00));
                    entries.add(ArtificialDyeItem.of(0x00FF00));
                    entries.add(ArtificialDyeItem.of(0x00FFFF));
                    entries.add(ArtificialDyeItem.of(0x0000FF));
                    entries.add(ArtificialDyeItem.of(0xFF00FF));

                    // Enchantments
                    //entries.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(, 1)));

                    // Creative
                    entries.add(CREATIVE_MOTOR);
                    entries.add(CREATIVE_CONTAINER);
                    entries.add(CREATIVE_DRAIN);
                })).build()
        );

        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "color"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(() -> ColoredItem.stack(CABLE, 1, DyeColor.RED))
                .displayName(Text.translatable("itemgroup." + ModInit.ID + ".color"))
                .entries(((context, entries) -> {

                    for (var dye : DyeColor.values()) {
                        var stack = WINDMILL_SAIL.getDefaultStack();
                        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(DyeColorExtra.getColor(dye), true));
                        entries.add(stack);
                    }

                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(INVERTED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var dye : DyeColor.values()) {
                        var x = ColoredItem.stack(SPRAY_CAN, 1, DyeColorExtra.getColor(dye));
                        x.set(FactoryDataComponents.USES_LEFT, 128);
                        entries.add(x);
                    }
                })).build()
        );

        if (ModInit.DEV_MODE) {
            PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "experimental"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                    .icon(FactoryDebugItems.DEBUG_PIPE_FLOW::getDefaultStack)
                    .displayName(Text.translatable("itemgroup." + ModInit.ID + ".experimental"))
                    .entries(((context, entries) -> {
                        FactoryDebugItems.addItemGroup(context, entries);
                        entries.add(ELECTRIC_GENERATOR, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                        entries.add(ELECTRIC_MOTOR, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                        entries.add(STEEL_BUCKET, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    })).build()
            );
        }

        AttackBlockCallback.EVENT.register(WRENCH::handleBlockAttack);
    }


    public static <T extends Item> T register(String path, T item) {
        Registry.register(Registries.ITEM, Identifier.of(ModInit.ID, path), item);
        return item;
    }

    public static <E extends Block & PolymerBlock> ColoredDownsampledBlockItem registerColored(E block, int color) {
        var id = Registries.BLOCK.getId(block);
        var item = new ColoredDownsampledBlockItem(block, color, new Item.Settings());
        Registry.register(Registries.ITEM, id, item);
        return item;
    }

    public static <E extends Block & PolymerBlock> FactoryBlockItem register(E block) {
        var id = Registries.BLOCK.getId(block);
        FactoryBlockItem item;
        if (block instanceof MultiBlock multiBlock) {
            item = new MultiBlockItem(multiBlock, new Item.Settings());
        } else if (block instanceof AbstractCableBlock cableBlock) {
            item = new CabledBlockItem((AbstractCableBlock & PolymerBlock) cableBlock, new Item.Settings());
        } else {
            item = new FactoryBlockItem(block, new Item.Settings());
        }

        Registry.register(Registries.ITEM, id, item);
        item.onRegistered(id);
        return item;
    }

    static {
        FactoryDebugItems.DEBUG_PIPE_FLOW.hasRecipeRemainder();
    }
}
