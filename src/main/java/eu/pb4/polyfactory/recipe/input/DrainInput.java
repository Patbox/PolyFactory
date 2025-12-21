package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record DrainInput(ItemStack stack, ItemStack catalyst, FluidContainerInput fluidContainer, boolean isPlayer) implements RecipeInput {
    public static DrainInput of(ItemStack stack, ItemStack catalyst, FluidContainer fluidContainer, boolean isPlayer) {
        return new DrainInput(stack, catalyst, FluidContainerInput.of(fluidContainer), isPlayer);
    }
    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? stack : catalyst;
    }
    @Override
    public int size() {
        return 2;
    }

    public long getFluid(FluidInstance<?> type) {
        return this.fluidContainer.get(type);
    }
    public List<FluidInstance<?>> fluids() {
        return this.fluidContainer.fluids();
    }
}
