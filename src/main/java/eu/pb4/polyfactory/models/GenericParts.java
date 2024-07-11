package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class GenericParts {
    public static final ItemStack REGULAR_GEAR = create(id("block/gear"));
    public static final ItemStack SMALL_GEAR = create(id("block/decorative_small_gear"));
    public static final ItemStack FILTER_MESH = create(id("block/filter_mesh"));

    private static ItemStack create(Identifier id) {
        var stack = new ItemStack(BaseItemProvider.requestModel());
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(PolymerResourcePackUtils.requestModel(stack.getItem(), id).value()));
        return stack;
    }
}
