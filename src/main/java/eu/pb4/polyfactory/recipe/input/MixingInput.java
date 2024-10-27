package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.world.World;

import java.util.List;

public record MixingInput(List<ItemStack> stacks, FluidContainerInput fluidContainer, World world) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    public CraftingRecipeInput asCraftingRecipeInput() {
        return CraftingRecipeInput.create(2, 3, this.stacks);
    }

    public long getFluid(FluidInstance<?> type) {
        return this.fluidContainer.get(type);
    }
    public List<FluidInstance<?>> fluids() {
        return this.fluidContainer.fluids();
    }

    @Override
    public boolean isEmpty() {
        return isSlotEmpty() && fluidContainer.isEmpty();
    }

    public boolean isSlotEmpty() {
        return RecipeInput.super.isEmpty();
    }
}
