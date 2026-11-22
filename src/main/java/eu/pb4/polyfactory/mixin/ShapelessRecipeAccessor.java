package eu.pb4.polyfactory.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.crafting.ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {
    @Accessor
    List<Ingredient> getIngredients();

    @Accessor
    ItemStackTemplate getResult();
}
