package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

public record MixingInput(List<ItemStack> stacks, FluidContainerInput fluidContainer, Level world) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    public CraftingInput asCraftingRecipeInput() {
        return CraftingInput.of(2, 3, this.stacks);
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
