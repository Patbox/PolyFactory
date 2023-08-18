package eu.pb4.polyfactory.item;

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
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FactoryItems {
    public static final WrenchItem WRENCH = register("wrench", new WrenchItem());
    public static final Item CONVEYOR_BLOCK = register("conveyor", new ModeledBlockItem(FactoryBlocks.CONVEYOR, new Item.Settings()));
    public static final Item STICKY_CONVEYOR_BLOCK = register("sticky_conveyor", new ModeledBlockItem(FactoryBlocks.STICKY_CONVEYOR, new Item.Settings()));
    public static final Item ELECTRIC_MOTOR_BLOCK = register("electric_motor", new ModeledBlockItem(FactoryBlocks.ELECTRIC_MOTOR, new Item.Settings()));
    public static final Item FUNNEL_BLOCK = register("funnel", new ModeledBlockItem(FactoryBlocks.FUNNEL, new Item.Settings()));
    public static final Item SPLITTER_BLOCK = register("splitter", new ModeledBlockItem(FactoryBlocks.SPLITTER, new Item.Settings()));
    public static final Item FAN_BLOCK = register("fan", new ModeledBlockItem(FactoryBlocks.FAN, new Item.Settings()));
    //public static final Item CABLE_PLATE_BLOCK = register("cable_plate", new ModeledBlockItem(FactoryBlocks.CABLE_PLATE, new Item.Settings()));
    public static final Item HAND_CRANK_BLOCK = register("hand_crank", new ModeledBlockItem(FactoryBlocks.HAND_CRANK, new Item.Settings()));
    public static final Item STEAM_ENGINE_BLOCK = register("steam_engine", new MultiBlockItem(FactoryBlocks.STEAM_ENGINE, new Item.Settings()));
    public static final Item GRINDER_BLOCK = register("grinder", new ModeledBlockItem(FactoryBlocks.GRINDER, new Item.Settings()));
    public static final Item PRESS_BLOCK = register("press", new ModeledBlockItem(FactoryBlocks.PRESS, new Item.Settings()));
    public static final Item MIXER_BLOCK = register("mixer", new ModeledBlockItem(FactoryBlocks.MIXER, new Item.Settings()));
    public static final Item MINER_BLOCK = register("miner", new ModeledBlockItem(FactoryBlocks.MINER, new Item.Settings()));
    public static final Item AXLE_BLOCK = register("axle", new ModeledBlockItem(FactoryBlocks.AXLE, new Item.Settings()));
    public static final Item GEARBOX_BLOCK = register("gearbox", new ModeledBlockItem(FactoryBlocks.GEARBOX, new Item.Settings()));
    public static final Item CONTAINER_BLOCK = register("wooden_container", new ModeledBlockItem(FactoryBlocks.CONTAINER, new Item.Settings()));
    public static final Item NIXIE_TUBE = register("nixie_tube", new ModeledBlockItem(FactoryBlocks.NIXIE_TUBE, new Item.Settings()));
    public static final Item WINDMILL_SAIL = register("windmill_sail", new WindmillSailItem(new Item.Settings()));
    public static final Item METAL_GRID_BLOCK = register("metal_grid", new ModeledBlockItem(FactoryBlocks.METAL_GRID, new Item.Settings()));

    public static final Item SAW_DUST = register("saw_dust", new ModeledItem(Items.STICK, new Item.Settings()));
    public static final Item COAL_DUST = register("coal_dust", new ModeledItem(Items.COAL, new Item.Settings()));
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", new ModeledItem(Items.IRON_INGOT, new Item.Settings()));
    public static final Item STEEL_INGOT = register("steel_ingot", new ModeledItem(Items.IRON_INGOT, new Item.Settings()));
    public static final Item STEEL_PLATE = register("steel_plate", new ModeledItem(Items.IRON_INGOT, new Item.Settings()));
    public static final Item STEEL_GEAR = register("steel_gear", new GearItem(new Item.Settings()));
    public static final Item GENERIC_MACHINE_PART = register("generic_machine_part", new ModeledItem(Items.IRON_NUGGET, new Item.Settings()));

    public static final Item WOODEN_PLATE = register("wooden_plate", new ModeledItem(Items.PAPER, new Item.Settings()));
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", new ModeledItem(Items.PAPER, new Item.Settings()));

    public static final Item ITEM_FILTER = register("item_filter", new FilterItem(Items.PAPER, new Item.Settings()));

    public static final Item CREATIVE_MOTOR_BLOCK = register("creative_motor", new ModeledBlockItem(FactoryBlocks.CREATIVE_MOTOR, new Item.Settings()));
    public static final Item CREATIVE_CONTAINER_BLOCK = register("creative_container", new ModeledBlockItem(FactoryBlocks.CREATIVE_CONTAINER, new Item.Settings()));

    public static final Item ROTATION_DEBUG_BLOCK = register("rot_debug", new ModeledBlockItem(FactoryBlocks.ROTATION_DEBUG, new Item.Settings()));
    public static final Item GREEN_SCREEN_BLOCK = register("green_screen", new ModeledBlockItem(FactoryBlocks.GREEN_SCREEN, new Item.Settings()));
    public static final Item BLOCK_DATA_PROVIDER = register("block_data_provider", new ModeledBlockItem(FactoryBlocks.BLOCK_DATA_PROVIDER, new Item.Settings()));
    public static final Item CABLE_BLOCK = register("cable", new CableItem(new Item.Settings()));


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
                    entries.add(AXLE_BLOCK);
                    entries.add(GEARBOX_BLOCK);

                    // Rotation Generation
                    entries.add(HAND_CRANK_BLOCK);
                    entries.add(WINDMILL_SAIL);
                    entries.add(STEAM_ENGINE_BLOCK);

                    // Item Movement/Storage
                    entries.add(CONVEYOR_BLOCK);
                    entries.add(STICKY_CONVEYOR_BLOCK);
                    entries.add(FAN_BLOCK);
                    entries.add(METAL_GRID_BLOCK);
                    entries.add(FUNNEL_BLOCK);
                    entries.add(SPLITTER_BLOCK);
                    entries.add(CONTAINER_BLOCK);
                    entries.add(ITEM_FILTER);

                    // Crafting/Machines
                    entries.add(GRINDER_BLOCK);
                    entries.add(PRESS_BLOCK);
                    entries.add(MIXER_BLOCK);
                    entries.add(MINER_BLOCK);

                    // Data
                    entries.add(CABLE_BLOCK);
                    entries.add(NIXIE_TUBE);
                    entries.add(BLOCK_DATA_PROVIDER);

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

                    // Creative
                    entries.add(CREATIVE_MOTOR_BLOCK);
                    entries.add(CREATIVE_CONTAINER_BLOCK);


                    // Remove this
                    if (ModInit.DEV) {
                        entries.add(ROTATION_DEBUG_BLOCK);
                        entries.add(GREEN_SCREEN_BLOCK);
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
