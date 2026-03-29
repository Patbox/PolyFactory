package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponentType;
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
    @WrapOperation(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z")
    )
    private boolean matchArtificialDye(ItemStack instance, DataComponentType dataComponentType, Operation<Boolean> original) {
        return instance.is(FactoryItems.ARTIFICIAL_DYE) || original.call(instance, dataComponentType);
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z", ordinal = 0)
    )
    private void addColor(CraftingInput recipeInputInventory, CallbackInfoReturnable<ItemStack> cir,
                          @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) IntList colors) {
        if (stack.is(FactoryItems.ARTIFICIAL_DYE)) {
            colors.add(ColoredItem.getColor(stack));
        }
    }
}
