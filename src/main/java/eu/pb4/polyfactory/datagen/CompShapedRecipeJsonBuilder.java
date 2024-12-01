package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.mixin.ShapedRecipeAccessor;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementRequirements;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class CompShapedRecipeJsonBuilder extends ShapedRecipeJsonBuilder {
    private final ComponentChanges.Builder builder = ComponentChanges.builder();

    public CompShapedRecipeJsonBuilder(RegistryEntryLookup<Item> registryLookup, RecipeCategory category, ItemConvertible output, int count) {
        super(registryLookup, category, output, count);
    }

    public <T> CompShapedRecipeJsonBuilder setComponent(ComponentType<T> type, T data) {
        if (data != null) {
            builder.add(type, data);
        }
        return this;
    }

    @Override
    public void offerTo(RecipeExporter exporter, RegistryKey<Recipe<?>> recipeKey) {
        super.offerTo(new RecipeExporter() {
            @Override
            public void accept(RegistryKey<Recipe<?>> recipeId, Recipe<?> recipe, @Nullable AdvancementEntry advancement) {
               var shaped = (ShapedRecipeAccessor) recipe;
               var stack = shaped.getResult();
               stack.applyChanges(builder.build());
               exporter.accept(recipeId, recipe, advancement);
            }

            @Override
            public Advancement.Builder getAdvancementBuilder() {
                return exporter.getAdvancementBuilder();
            }

            @Override
            public void addRootAdvancement() {
                exporter.addRootAdvancement();
            }
        }, recipeKey);
    }

}