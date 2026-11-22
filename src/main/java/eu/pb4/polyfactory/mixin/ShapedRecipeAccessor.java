package eu.pb4.polyfactory.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.crafting.ShapedRecipe.class)
public interface ShapedRecipeAccessor {
    @Accessor
    ItemStackTemplate getResult();

    @Accessor
    ShapedRecipePattern getPattern();
}
