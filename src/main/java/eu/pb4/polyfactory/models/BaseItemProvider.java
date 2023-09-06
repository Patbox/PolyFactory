package eu.pb4.polyfactory.models;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class BaseItemProvider {
    private static final Item[] SIMPLE = new Item[] {
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

    private static int currentSimple = 0;
    
    public static Item requestSimpleItem() {
        return SIMPLE[currentSimple++ % SIMPLE.length];
    }
}
