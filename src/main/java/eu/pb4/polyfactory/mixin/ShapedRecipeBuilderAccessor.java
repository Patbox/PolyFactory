package eu.pb4.polyfactory.mixin;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.data.recipes.ShapedRecipeBuilder.class)
public interface ShapedRecipeBuilderAccessor {
    @Invoker("<init>")
    static ShapedRecipeBuilder createShapedRecipeBuilder(final HolderGetter<Item> items, final RecipeCategory category, final ItemStackTemplate result) {
        throw new UnsupportedOperationException();
    }
}
