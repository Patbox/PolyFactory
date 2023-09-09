package eu.pb4.polyfactory.models;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class BaseItemProvider {
    private static final Item[] ITEMS = new Item[] {
            Items.PAPER,
            Items.IRON_NUGGET,
            Items.GOLD_NUGGET,
            Items.IRON_INGOT,
            Items.GOLD_INGOT,
            Items.CLAY_BALL,
            Items.SCUTE,
            Items.LEATHER,
            Items.RABBIT_HIDE,
            Items.RABBIT_FOOT,
            Items.ECHO_SHARD,
            Items.PHANTOM_MEMBRANE,
            Items.GUNPOWDER,
            Items.GLOWSTONE_DUST,
            Items.GHAST_TEAR
    };

    private static int currentItem = 0;

    private static final Item[] MODELS = new Item[] {
            Items.WHITE_WOOL,
            Items.ORANGE_WOOL,
            Items.MAGENTA_WOOL,
            Items.LIGHT_BLUE_WOOL,
            Items.YELLOW_WOOL,
            Items.LIME_WOOL,
            Items.PINK_WOOL,
            Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL,
            Items.CYAN_WOOL,
            Items.PURPLE_WOOL,
            Items.BLUE_WOOL,
            Items.BROWN_WOOL,
            Items.GREEN_WOOL,
            Items.RED_WOOL,
            Items.BLACK_WOOL
    };

    private static int currentModels = 0;


    public static Item requestItem() {
        return ITEMS[currentItem++ % ITEMS.length];
    }

    public static Item requestModel() {
        return MODELS[currentModels++ % MODELS.length];
    }
}
