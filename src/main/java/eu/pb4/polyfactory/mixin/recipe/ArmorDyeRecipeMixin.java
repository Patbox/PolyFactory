package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorDyeRecipe.class)
public class ArmorDyeRecipeMixin {
    @SuppressWarnings("MixinAnnotationTarget")
    @WrapOperation(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean matchArtificialDye(Object obj, Operation<Boolean> original) {
        return obj == FactoryItems.ARTIFICIAL_DYE || original.call(obj);
    }

    @WrapOperation(
            method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0)
    )
    private boolean captureArtificialDyes(ItemStack stack, Operation<Boolean> original) {
        if (stack.is(FactoryItems.ARTIFICIAL_DYE)) {
            ArtificialDyeItem.CURRENT_DYES.get().add(stack);
            return true;
        }

        return original.call(stack);
    }

    @ModifyExpressionValue(
            method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z")
    )
    private boolean markNotEmpty(boolean value) {
        if (!ArtificialDyeItem.CURRENT_DYES.get().isEmpty()) {
            return false;
        }

        return value;
    }

    @Inject(
            method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private void clearArtificialList(CraftingInput recipeInputInventory, HolderLookup.Provider wrapperLookup, CallbackInfoReturnable<ItemStack> cir) {
        ArtificialDyeItem.CURRENT_DYES.get().clear();
    }
}
