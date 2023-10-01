package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.AbstractRecipePolydexPage;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.Identifier;

public abstract class PrioritizedRecipePage<T extends Recipe<?>> extends AbstractRecipePolydexPage<T> {
    private final int priority;

    public PrioritizedRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        this.priority = recipe.id().getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? 0 : 10;
    }

    @Override
    public int priority() {
        return this.priority;
    }
}
