package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.mixin.ShapedRecipeAccessor;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementRequirements;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class CompShapedRecipeJsonBuilder extends ShapedRecipeJsonBuilder {
    private final ComponentChanges.Builder builder = ComponentChanges.builder();

    public CompShapedRecipeJsonBuilder(RecipeCategory category, ItemConvertible output, int count) {
        super(category, output, count);
    }

    public <T> CompShapedRecipeJsonBuilder setComponent(ComponentType<T> type, T data) {
        if (data != null) {
            builder.add(type, data);
        }
        return this;
    }

    @Override
    public void offerTo(RecipeExporter exporter, Identifier recipeId) {
        super.offerTo(new RecipeExporter() {
            @Override
            public void accept(Identifier recipeId, Recipe<?> recipe, @Nullable AdvancementEntry advancement) {
               var shaped = (ShapedRecipe) recipe;
               var stack = shaped.getResult(null);
               stack.applyChanges(builder.build());
               exporter.accept(recipeId,
                       new ShapedRecipe(shaped.getGroup(), shaped.getCategory(),
                               ((ShapedRecipeAccessor) shaped).getRaw(),
                               stack,
                               shaped.showNotification())
                       , advancement);
            }

            @Override
            public Advancement.Builder getAdvancementBuilder() {
                return exporter.getAdvancementBuilder();
            }
        }, recipeId);
    }

}