package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack modifyColors(ItemStack stack, CraftingInput recipeInputInventory) {
        if (stack.has(FactoryDataComponents.COLOR)
                && stack.getOrDefault(FactoryDataComponents.COLOR, -1) == -2) {
            for (var x : recipeInputInventory.items()) {
                if (x.is(ConventionalItemTags.DYES)) {
                    stack.set(FactoryDataComponents.COLOR, DyeColorExtra.getColor(x));
                    break;
                }
            }
        }
        return stack;
    }
}