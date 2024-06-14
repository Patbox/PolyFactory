package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @ModifyReturnValue(method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    private ItemStack modifyColors(ItemStack stack, CraftingRecipeInput recipeInputInventory) {
        if (stack.contains(FactoryDataComponents.COLOR)
                && stack.getOrDefault(FactoryDataComponents.COLOR, -1) == -2) {
            for (var x : recipeInputInventory.getStacks()) {
                if (x.isIn(ConventionalItemTags.DYES)) {
                    stack.set(FactoryDataComponents.COLOR, DyeColorExtra.getColor(x));
                    break;
                }
            }
        }
        return stack;
    }
}