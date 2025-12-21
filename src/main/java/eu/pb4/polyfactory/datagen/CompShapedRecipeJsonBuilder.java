package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.mixin.ShapedRecipeAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

public class CompShapedRecipeJsonBuilder extends ShapedRecipeBuilder {
    private final DataComponentPatch.Builder builder = DataComponentPatch.builder();

    public CompShapedRecipeJsonBuilder(HolderGetter<Item> registryLookup, RecipeCategory category, ItemLike output, int count) {
        super(registryLookup, category, output, count);
    }

    public <T> CompShapedRecipeJsonBuilder setComponent(DataComponentType<T> type, T data) {
        if (data != null) {
            builder.set(type, data);
        }
        return this;
    }

    @Override
    public void save(RecipeOutput exporter, ResourceKey<Recipe<?>> recipeKey) {
        super.save(new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> recipeId, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
               var shaped = (ShapedRecipeAccessor) recipe;
               var stack = shaped.getResult();
               stack.applyComponentsAndValidate(builder.build());
               exporter.accept(recipeId, recipe, advancement);
            }

            @Override
            public Advancement.Builder advancement() {
                return exporter.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                exporter.includeRootAdvancement();
            }
        }, recipeKey);
    }

}