package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import it.unimi.dsi.fastutil.ints.IntList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.FireworkStarRecipe;

@Mixin(FireworkStarRecipe.class)
public class FireworkStarRecipeMixin {
    @SuppressWarnings("MixinAnnotationTarget")
    @WrapOperation(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            constant = @Constant(classValue = DyeItem.class)
    )
    private boolean matchArtificialDye(Object obj, Operation<Boolean> original) {
        return obj == FactoryItems.ARTIFICIAL_DYE || original.call(obj);
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z", ordinal = 0)
    )
    private void addColor(CraftingInput recipeInputInventory, HolderLookup.Provider wrapperLookup, CallbackInfoReturnable<ItemStack> cir,
                          @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) IntList colors) {
        if (stack.is(FactoryItems.ARTIFICIAL_DYE)) {
            colors.add(ColoredItem.getColor(stack));
        }
    }
}
