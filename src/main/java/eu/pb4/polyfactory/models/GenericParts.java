package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.util.LazyItemStack;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class GenericParts {
    public static final LazyItemStack REGULAR_GEAR = create(id("block/gear"));
    public static final LazyItemStack SMALL_GEAR = create(id("block/decorative_small_gear"));
    public static final LazyItemStack FILTER_MESH = create(id("block/filter_mesh"));

    private static LazyItemStack create(Identifier id) {
        return ItemDisplayElementUtil.getModel(id);
    }
}
