package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.AbstractRecipePolydexPage;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public abstract class PrioritizedRecipePage<T extends Recipe<?>> extends AbstractRecipePolydexPage<T> {
    private static final DynamicRegistryManager GLOBAL = DynamicRegistryManager.of(Registries.REGISTRIES);
    private final int priority;

    public PrioritizedRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        var priotity = 0;
        try {
            priotity = Registries.ITEM.getId(recipe.value().getResult(GLOBAL).getItem())
                    .getNamespace().startsWith(Identifier.DEFAULT_NAMESPACE) ? 0 : 10;
        } catch (Throwable e) {
            // No op
        }
        this.priority = priotity;
    }

    @Override
    public int priority() {
        return this.priority;
    }
}
