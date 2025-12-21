package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.AbstractRecipePolydexPage;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class PrioritizedRecipePage<T extends Recipe<?>> extends AbstractRecipePolydexPage<T> {
    private static final RegistryAccess GLOBAL = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final int priority;

    public PrioritizedRecipePage(RecipeHolder<T> recipe) {
        super(recipe);
        var priotity = 0;
        try {
            priotity = BuiltInRegistries.ITEM.getKey(getOutput(null, null).getItem())
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
