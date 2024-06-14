package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ArmorDyeRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorDyeRecipe.class)
public class ArmorDyeRecipeMixin {
    @SuppressWarnings("MixinAnnotationTarget")
    @WrapOperation(method = "matches(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/world/World;)Z",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean matchArtificialDye(Object obj, Operation<Boolean> original) {
        return obj == FactoryItems.ARTIFICIAL_DYE || original.call(obj);
    }

    @WrapOperation(
            method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0)
    )
    private boolean captureArtificialDyes(ItemStack stack, Operation<Boolean> original) {
        if (stack.isOf(FactoryItems.ARTIFICIAL_DYE)) {
            ArtificialDyeItem.CURRENT_DYES.get().add(stack);
            return true;
        }

        return original.call(stack);
    }

    @ModifyExpressionValue(
            method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z")
    )
    private boolean markNotEmpty(boolean value) {
        if (!ArtificialDyeItem.CURRENT_DYES.get().isEmpty()) {
            return false;
        }

        return value;
    }

    @Inject(
            method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
            at = @At("RETURN")
    )
    private void clearArtificialList(CraftingRecipeInput recipeInputInventory, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfoReturnable<ItemStack> cir) {
        ArtificialDyeItem.CURRENT_DYES.get().clear();
    }
}
