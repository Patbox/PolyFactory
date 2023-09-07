package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.item.block.CableItem;
import eu.pb4.polyfactory.item.block.WindmillSailItem;
import eu.pb4.polyfactory.item.tool.FilterItem;
import eu.pb4.polyfactory.item.util.ModeledItem;
import eu.pb4.polyfactory.item.util.MultiBlockItem;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.tool.WrenchItem;
import eu.pb4.polyfactory.item.util.AutoModeledPolymerItem;
import eu.pb4.polyfactory.item.util.ModeledBlockItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FactoryItems {
    public static final WrenchItem WRENCH = register("wrench", new WrenchItem());
    public static final Item CONVEYOR = register("conveyor", new ModeledBlockItem(FactoryBlocks.CONVEYOR, new Item.Settings()));
    public static final Item STICKY_CONVEYOR = register("sticky_conveyor", new ModeledBlockItem(FactoryBlocks.STICKY_CONVEYOR, new Item.Settings()));
    public static final Item ELECTRIC_MOTOR = register("electric_motor", new ModeledBlockItem(FactoryBlocks.ELECTRIC_MOTOR, new Item.Settings()));
    public static final Item FUNNEL = register("funnel", new ModeledBlockItem(FactoryBlocks.FUNNEL, new Item.Settings()));
    public static final Item SPLITTER = register("splitter", new ModeledBlockItem(FactoryBlocks.SPLITTER, new Item.Settings()));
    public static final Item FAN = register("fan", new ModeledBlockItem(FactoryBlocks.FAN, new Item.Settings()));
    //public static final Item CABLE_PLATE_BLOCK = register("cable_plate", new ModeledBlockItem(FactoryBlocks.CABLE_PLATE, new Item.Settings()));
    public static final Item HAND_CRANK = register("hand_crank", new ModeledBlockItem(FactoryBlocks.HAND_CRANK, new Item.Settings()));
    public static final Item STEAM_ENGINE = register("steam_engine", new MultiBlockItem(FactoryBlocks.STEAM_ENGINE, new Item.Settings()));
    public static final Item GRINDER = register("grinder", new ModeledBlockItem(FactoryBlocks.GRINDER, new Item.Settings()));
    public static final Item PRESS = register("press", new ModeledBlockItem(FactoryBlocks.PRESS, new Item.Settings()));
    public static final Item MIXER = register("mixer", new ModeledBlockItem(FactoryBlocks.MIXER, new Item.Settings()));
    public static final Item MINER = register("miner", new ModeledBlockItem(FactoryBlocks.MINER, new Item.Settings()));
    public static final ModeledBlockItem AXLE = register("axle", new ModeledBlockItem(FactoryBlocks.AXLE, new Item.Settings()));
    public static final Item GEARBOX = register("gearbox", new ModeledBlockItem(FactoryBlocks.GEARBOX, new Item.Settings()));
    public static final Item CONTAINER = register("wooden_container", new ModeledBlockItem(FactoryBlocks.CONTAINER, new Item.Settings()));
    public static final Item NIXIE_TUBE = register("nixie_tube", new ModeledBlockItem(FactoryBlocks.NIXIE_TUBE, new Item.Settings()));
    public static final Item WINDMILL_SAIL = register("windmill_sail", new WindmillSailItem(new Item.Settings()));
    public static final Item METAL_GRID = register("metal_grid", new ModeledBlockItem(FactoryBlocks.METAL_GRID, new Item.Settings()));

    public static final Item SAW_DUST = register("saw_dust", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item COAL_DUST = register("coal_dust", new ModeledItem(Items.COAL, new Item.Settings()));
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_INGOT = register("steel_ingot", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_PLATE = register("steel_plate", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_GEAR = register("steel_gear", new GearItem(new Item.Settings()));
    public static final Item GENERIC_MACHINE_PART = register("generic_machine_part", new ModeledItem(new Item.Settings()));

    public static final Item WOODEN_PLATE = register("wooden_plate", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", new ModeledItem(new Item.Settings()));

    public static final Item ITEM_FILTER = register("item_filter", new FilterItem(Items.PAPER, new Item.Settings()));

    public static final Item CREATIVE_MOTOR = register("creative_motor", new ModeledBlockItem(FactoryBlocks.CREATIVE_MOTOR, new Item.Settings()));
    public static final Item CREATIVE_CONTAINER = register("creative_container", new ModeledBlockItem(FactoryBlocks.CREATIVE_CONTAINER, new Item.Settings()));

    public static final Item ROTATION_DEBUG = register("rot_debug", new ModeledBlockItem(FactoryBlocks.ROTATION_DEBUG, new Item.Settings()));
    public static final Item GREEN_SCREEN = register("green_screen", new ModeledBlockItem(FactoryBlocks.GREEN_SCREEN, new Item.Settings()));
    public static final Item ITEM_COUNTER = register("item_counter", new ModeledBlockItem(FactoryBlocks.ITEM_COUNTER, new Item.Settings()));
    public static final Item REDSTONE_INPUT = register("redstone_input", new ModeledBlockItem(FactoryBlocks.REDSTONE_INPUT, new Item.Settings()));
    public static final Item REDSTONE_OUTPUT = register("redstone_output", new ModeledBlockItem(FactoryBlocks.REDSTONE_OUTPUT, new Item.Settings()));
    public static final CableItem CABLE = register("cable", new CableItem(new Item.Settings()));


    public static final Item ARTIFICIAL_DYE = register("artificial_dye", new ArtificialDyeItem(new Item.Settings()));
    public static final Item INVERTED_REDSTONE_LAMP = register("inverted_redstone_lamp", new ModeledBlockItem(FactoryBlocks.INVERTED_REDSTONE_LAMP, new Item.Settings()));


    public static void register() {
        FuelRegistry.INSTANCE.add(SAW_DUST, 60);
        FuelRegistry.INSTANCE.add(COAL_DUST, 160);

        PolymerItemGroupUtils.registerPolymerItemGroup(new Identifier(ModInit.ID, "group"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(WINDMILL_SAIL::getDefaultStack)
                .displayName(Text.translatable("itemgroup." + ModInit.ID))
                .entries(((context, entries) -> {
                    //entries.add(WRENCH);

                    // Rotational machines (tier 1)

                    // Rotation transmision
                    entries.add(AXLE);
                    entries.add(GEARBOX);

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

                    // Crafting/Machines
                    entries.add(GRINDER);
                    entries.add(PRESS);
                    entries.add(MIXER);
                    entries.add(MINER);

                    // Data
                    entries.add(CABLE);
                    entries.add(NIXIE_TUBE);
                    entries.add(REDSTONE_OUTPUT);
                    entries.add(REDSTONE_INPUT);
                    entries.add(ITEM_COUNTER);


                    // Rest

                    entries.add(INVERTED_REDSTONE_LAMP);

                    // Electrical machines (tier 2)
                    //entries.add(ELECTRIC_MOTOR_BLOCK);
                    //entries.add(CABLE_PLATE_BLOCK);

                    // Generic Materials
                    entries.add(SAW_DUST);
                    entries.add(COAL_DUST);
                    entries.add(STEEL_ALLOY_MIXTURE);
                    entries.add(STEEL_INGOT);
                    entries.add(STEEL_PLATE);
                    entries.add(STEEL_GEAR);
                    entries.add(WOODEN_PLATE);
                    entries.add(TREATED_DRIED_KELP);
                    entries.add(GENERIC_MACHINE_PART);

                    // Fancy dyes
                    entries.add(ArtificialDyeItem.of(0xFF0000));
                    entries.add(ArtificialDyeItem.of(0xFFFF00));
                    entries.add(ArtificialDyeItem.of(0x00FF00));
                    entries.add(ArtificialDyeItem.of(0x00FFFF));
                    entries.add(ArtificialDyeItem.of(0x0000FF));
                    entries.add(ArtificialDyeItem.of(0xFF00FF));

                    // Enchantments
                    entries.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(FactoryEnchantments.IGNORE_MOVEMENT, 1)));

                    // Creative
                    entries.add(CREATIVE_MOTOR);
                    entries.add(CREATIVE_CONTAINER);


                    // Remove this
                    if (ModInit.DEV) {
                        entries.add(ROTATION_DEBUG);
                        entries.add(GREEN_SCREEN);
                    }
                })).build()
        );


        AttackBlockCallback.EVENT.register(WRENCH::handleBlockAttack);
    }


    public static <T extends Item> T register(String path, T item) {
        Registry.register(Registries.ITEM, new Identifier(ModInit.ID, path), item);
        if (item instanceof AutoModeledPolymerItem modeledPolymerItem) {
            modeledPolymerItem.defineModels(new Identifier(ModInit.ID, path));
        }
        return item;
    }
}
