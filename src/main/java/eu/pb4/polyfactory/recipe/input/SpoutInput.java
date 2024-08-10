package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SpoutInput(ItemStack stack, Object2LongMap<FluidInstance<?>> fluidsAmount, List<FluidInstance<?>> fluids, ServerWorld world) implements RecipeInput {
    public static SpoutInput of(ItemStack stack, @Nullable FluidContainer fluidContainer, ServerWorld world) {
        return new SpoutInput(stack, fluidContainer != null ? fluidContainer.asMap() : Object2LongMaps.emptyMap(),
                fluidContainer != null ? fluidContainer.orderList() : List.of(), world);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stack;
    }
    @Override
    public int getSize() {
        return 1;
    }
    public long getFluid(FluidInstance<?> type) {
        return this.fluidsAmount.getOrDefault(type, 0);
    }
}
