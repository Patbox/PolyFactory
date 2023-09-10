package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.multiblock.MultiBlock;
import eu.pb4.polyfactory.item.block.CableItem;
import eu.pb4.polyfactory.item.block.WindmillSailItem;
import eu.pb4.polyfactory.item.tool.FilterItem;
import eu.pb4.polyfactory.item.util.ModeledItem;
import eu.pb4.polyfactory.item.util.MultiBlockItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.tool.WrenchItem;
import eu.pb4.polyfactory.item.util.AutoModeledPolymerItem;
import eu.pb4.polyfactory.item.util.ModeledBlockItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class FactoryItems {
    public static final WrenchItem WRENCH = register("wrench", new WrenchItem());
    public static final Item CONVEYOR = register(FactoryBlocks.CONVEYOR);
    public static final Item STICKY_CONVEYOR = register(FactoryBlocks.STICKY_CONVEYOR);
    public static final Item ELECTRIC_MOTOR = register(FactoryBlocks.ELECTRIC_MOTOR);
    public static final Item FUNNEL = register(FactoryBlocks.FUNNEL);
    public static final Item SPLITTER = register(FactoryBlocks.SPLITTER);
    public static final Item FAN = register(FactoryBlocks.FAN);
    //public static final Item CABLE_PLATE_BLOCK = register("cable_plate", FactoryBlocks.CABLE_PLATE);
    public static final Item HAND_CRANK = register(FactoryBlocks.HAND_CRANK);
    public static final Item STEAM_ENGINE = register(FactoryBlocks.STEAM_ENGINE);
    public static final Item GRINDER = register(FactoryBlocks.GRINDER);
    public static final Item PRESS = register(FactoryBlocks.PRESS);
    public static final Item MIXER = register(FactoryBlocks.MIXER);
    public static final Item MINER = register(FactoryBlocks.MINER);
    public static final ModeledBlockItem AXLE = register(FactoryBlocks.AXLE);
    public static final Item GEARBOX = register(FactoryBlocks.GEARBOX);
    public static final Item CONTAINER = register( FactoryBlocks.CONTAINER);
    public static final Item NIXIE_TUBE = register(FactoryBlocks.NIXIE_TUBE);
    public static final Item WINDMILL_SAIL = register("windmill_sail", new WindmillSailItem(new Item.Settings()));
    public static final Item METAL_GRID = register(FactoryBlocks.METAL_GRID);

    public static final Item SAW_DUST = register("saw_dust", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item COAL_DUST = register("coal_dust", new ModeledItem(Items.COAL, new Item.Settings()));
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_INGOT = register("steel_ingot", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_PLATE = register("steel_plate", new ModeledItem(new Item.Settings()));
    public static final Item STEEL_GEAR = register("steel_gear", new GearItem(new Item.Settings()));
    public static final Item GENERIC_MACHINE_PART = register("generic_machine_part", new ModeledItem(new Item.Settings()));
    public static final Item WOODEN_PLATE = register("wooden_plate", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", new ModeledItem(new Item.Settings()));
    public static final Item INTEGRATED_CIRCUIT = register("integrated_circuit", new ModeledItem(new Item.Settings()));

    public static final Item ITEM_FILTER = register("item_filter", new FilterItem(Items.PAPER, new Item.Settings()));

    public static final Item CREATIVE_MOTOR = register(FactoryBlocks.CREATIVE_MOTOR);
    public static final Item CREATIVE_CONTAINER = register(FactoryBlocks.CREATIVE_CONTAINER);

    public static final Item ROTATION_DEBUG = register(FactoryBlocks.ROTATION_DEBUG);
    public static final Item GREEN_SCREEN = register(FactoryBlocks.GREEN_SCREEN);
    public static final Item ITEM_COUNTER = register(FactoryBlocks.ITEM_COUNTER);
    public static final Item REDSTONE_INPUT = register(FactoryBlocks.REDSTONE_INPUT);
    public static final Item REDSTONE_OUTPUT = register(FactoryBlocks.REDSTONE_OUTPUT);
    public static final Item BOOK_READER = register(FactoryBlocks.BOOK_READER);
    public static final Item NIXIE_TUBE_CONTROLLER = register(FactoryBlocks.NIXIE_TUBE_CONTROLLER);
    public static final CableItem CABLE = register("cable", new CableItem(new Item.Settings()));


    public static final Item ARTIFICIAL_DYE = register("artificial_dye", new ArtificialDyeItem(new Item.Settings()));
    public static final Item INVERTED_REDSTONE_LAMP = register(FactoryBlocks.INVERTED_REDSTONE_LAMP);


    public static void register() {
        FuelRegistry.INSTANCE.add(SAW_DUST, 60);
        FuelRegistry.INSTANCE.add(COAL_DUST, 160);

        PolymerItemGroupUtils.registerPolymerItemGroup(new Identifier(ModInit.ID, "group"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(WINDMILL_SAIL::getDefaultStack)
                .displayName(Text.translatable("itemgroup." + ModInit.ID))
                .entries(((context, entries) -> {
                    //entries.add(WRENCH);

                    // Rotational machines (tier 1)

                    // Rotation transmission
                    entries.add(AXLE);
                    entries.add(GEARBOX);
                    entries.add(STEEL_GEAR);

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
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.RED)));
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.GREEN)));
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.BLUE)));
                    entries.add(REDSTONE_OUTPUT);
                    entries.add(REDSTONE_INPUT);
                    entries.add(ITEM_COUNTER);
                    entries.add(BOOK_READER);
                    entries.add(NIXIE_TUBE_CONTROLLER);
                    entries.add(NIXIE_TUBE);


                    // Rest

                    entries.add(INVERTED_REDSTONE_LAMP);

                    // Electrical machines (tier 2)
                    //entries.add(ELECTRIC_MOTOR_BLOCK);

                    // Generic Materials
                    entries.add(SAW_DUST);
                    entries.add(COAL_DUST);
                    entries.add(STEEL_ALLOY_MIXTURE);
                    entries.add(STEEL_INGOT);
                    entries.add(STEEL_PLATE);
                    //entries.add(STEEL_GEAR);
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

    public static <E extends Block & PolymerBlock> ModeledBlockItem register(E block) {
        var id = Registries.BLOCK.getId(block);
        ModeledBlockItem item;
        if (block instanceof MultiBlock multiBlock) {
            item = new MultiBlockItem(multiBlock, new Item.Settings());
        } else {
            item = new ModeledBlockItem(block, new Item.Settings());
        }

        Registry.register(Registries.ITEM, id, item);
        item.defineModels(id);
        return item;
    }
}
