package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.grinding.SimpleGrindingRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SimpleGrindingRecipePage extends GrindingRecipePage<SimpleGrindingRecipe> {
    private final PolydexStack<?>[] output;
    private final List<PolydexIngredient<?>> ingredients;

    public SimpleGrindingRecipePage(RecipeHolder<SimpleGrindingRecipe> recipe) {
        super(recipe);
        this.output = PolydexCompatImpl.createOutput(this.recipe.output());
        this.ingredients = List.of(PolydexIngredient.of(recipe.value().input()));
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return this.recipe.output().getFirst().stack().copy();
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        for (var i : this.output) {
            if (entry.isPartOf(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return this.recipe.output().getFirst().stack();
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder layer) {
        layer.setIngredient(4, 1, this.recipe.input());

        var i = 0;
        for (; i < this.output.length; i++) {
            layer.setOutput(3 + i, 3, this.output[i]);
        }
        for (; i < 3; i++) {
            layer.setEmpty(3 + i, 3);
        }
    }
}
