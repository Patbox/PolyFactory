package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @ModifyReturnValue(method = "craft", at = @At("RETURN"))
    private ItemStack modifyColors(ItemStack stack, RecipeInputInventory recipeInputInventory) {
        if (stack.hasNbt() && stack.getNbt().contains("color", NbtElement.STRING_TYPE)
                && stack.getNbt().getString("color").equals("dyn")) {
            for (var x : recipeInputInventory.getInputStacks()) {
                if (x.isIn(ConventionalItemTags.DYES)) {
                    stack.getNbt().putInt("color", DyeColorExtra.getColor(x));
                    break;
                }
            }
        }
        return stack;
    }
}
