package eu.pb4.polyfactory.models;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class GenericParts {
    public static final ItemStack SMALL_GEAR = create(id("block/decorative_small_gear"));
    public static final ItemStack FILTER_MESH = create(id("block/filter_mesh"));

    private static ItemStack create(Identifier id) {
        var stack = new ItemStack(Items.MAGENTA_CANDLE);
        stack.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(Items.MAGENTA_CANDLE, id).value());
        return stack;
    }
}
