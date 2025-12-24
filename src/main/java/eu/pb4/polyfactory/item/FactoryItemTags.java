package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class FactoryItemTags {
    public static final TagKey<Item> ALLOWED_IN_MINER = of("allowed_in_miner");
    public static final TagKey<Item> ALLOWED_IN_PLANTER = of("allowed_in_planter");
    public static final TagKey<Item> ROOT_ADVANCEMENT = of("root_advancement");
    public static final TagKey<Item> PLACER_USABLE = of("placer_usable");
    public static final TagKey<Item> DRAIN_CATALYST = of("drain_catalyst");
    public static final TagKey<Item> XP_CONVERSION_CATALYST = of("xp_conversion_catalyst");
    public static final TagKey<Item> SPOUT_ITEM_HORIZONTAL = of("spout_item_horizontal");
    public static final TagKey<Item> CASTING_SMALL_FLUID = of("casting_small_fluid");
    public static final TagKey<Item> MOLDS = of("molds");
    public static final TagKey<Item> SHAPEABLE_CLAY_MOLDS = of("shapeable_clay_molds");
    public static final TagKey<Item> PIPES = of("pipes");
    public static final TagKey<Item> MOLD_SHAPE_BOTTLE = of("mold_shape/bottle");
    public static final TagKey<Item> MOLD_SHAPE_THROWABLE_BOTTLE = of("mold_shape/throwable_bottle");
    public static final TagKey<Item> MOLD_SHAPE_BRITTLE_BOTTLE = of("mold_shape/brittle_bottle");
    public static final TagKey<Item> GLASS_BOTTLE_MELTABLE = of("glass_bottle_meltable");

    private static TagKey<Item> of(String path) {
        return TagKey.create(Registries.ITEM, ModInit.id(path));
    }
}
