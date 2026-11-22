package eu.pb4.polyfactory.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.crafting.Ingredient.class)
public interface IngredientAccessor {
    @Accessor
    HolderSet<Item> getValues();
}
