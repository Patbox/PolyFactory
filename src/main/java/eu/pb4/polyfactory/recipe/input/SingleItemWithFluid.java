package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SingleItemWithFluid(ItemStack stack, FluidContainerInput fluidContainer, ServerWorld world) implements RecipeInput {
    public static SingleItemWithFluid of(ItemStack stack, @Nullable FluidContainer fluidContainer, ServerWorld world) {
        return new SingleItemWithFluid(stack, FluidContainerInput.of(fluidContainer), world);
    }
    @Override
    public ItemStack getStackInSlot(int slot) {
        return stack;
    }
    @Override
    public int size() {
        return 1;
    }
    public long getFluid(FluidInstance<?> type) {
        return this.fluidContainer.get(type);
    }

    @Override
    public boolean isEmpty() {
        return RecipeInput.super.isEmpty() && this.fluidContainer.isEmpty();
    }

    public List<FluidInstance<?>> fluids() {
        return this.fluidContainer.fluids();
    }}
