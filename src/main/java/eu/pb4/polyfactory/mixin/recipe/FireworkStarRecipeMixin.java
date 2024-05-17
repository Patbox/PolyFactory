package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.FireworkStarRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FireworkStarRecipe.class)
public class FireworkStarRecipeMixin {
    @SuppressWarnings("MixinAnnotationTarget")
    @WrapOperation(method = "matches(Lnet/minecraft/inventory/RecipeInputInventory;Lnet/minecraft/world/World;)Z",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean matchArtificialDye(Object obj, Operation<Boolean> original) {
        return obj == FactoryItems.ARTIFICIAL_DYE || original.call(obj);
    }

    @Inject(method = "craft(Lnet/minecraft/inventory/RecipeInputInventory;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/Ingredient;test(Lnet/minecraft/item/ItemStack;)Z", ordinal = 0)
    )
    private void addColor(RecipeInputInventory recipeInputInventory, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfoReturnable<ItemStack> cir,
                          @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) IntList colors) {
        if (stack.isOf(FactoryItems.ARTIFICIAL_DYE)) {
            colors.add(ColoredItem.getColor(stack));
        }
    }
}
