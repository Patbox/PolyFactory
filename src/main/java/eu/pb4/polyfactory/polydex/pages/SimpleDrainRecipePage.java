package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.drain.SimpleDrainRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SimpleDrainRecipePage extends DrainRecipePage<SimpleDrainRecipe> {
    public SimpleDrainRecipePage(RecipeHolder<SimpleDrainRecipe> recipe) {
        super(recipe);
    }

    @Override
    public boolean canDisplay(@Nullable PolydexEntry entry, ServerPlayer player) {
        return !this.recipe.requirePlayer();
    }

    @Override
    protected ItemStack getResultStack() {
        return this.recipe.output();
    }

    @Override
    protected List<FluidStack<?>> getBaseFluids() {
        return this.recipe.fluidInput();
    }

    @Override
    protected List<FluidStack<?>> getResultFluids() {
        return this.recipe.fluidOutput();
    }

    @Override
    protected List<CountedIngredient> getBaseIngredient() {
        if (this.recipe.catalyst().isPresent()) {
            return List.of(
                    this.recipe.item(),
                    new CountedIngredient(this.recipe.catalyst(), ItemComponentPredicate.EMPTY, 0, ItemStack.EMPTY)
            );
        }

        return List.of(this.recipe.item());
    }
}
