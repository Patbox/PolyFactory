package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public record CraftingWithLeftoverRecipe<T extends CraftingRecipe>(RecipeSerializer<CraftingWithLeftoverRecipe<T>> serializer, T backingRecipe, List<Ingredient> leftovers) implements CraftingRecipe {
    public static <T extends CraftingRecipe> MapCodec<CraftingWithLeftoverRecipe<T>> createCodec(MapCodec<T> recipeCodec, Supplier<RecipeSerializer<CraftingWithLeftoverRecipe<T>>> serializer) {
        return MapCodec.recursive("CraftingWithLeftovers", (c) -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                MapCodec.unit(serializer).forGetter(CraftingWithLeftoverRecipe::serializer),
                recipeCodec.forGetter(CraftingWithLeftoverRecipe::backingRecipe),
                Ingredient.CODEC.listOf().fieldOf("leftovers").forGetter(CraftingWithLeftoverRecipe::leftovers)
        ).apply(instance, CraftingWithLeftoverRecipe::new)));
    }
    public static CraftingWithLeftoverRecipe<ShapedRecipe> of(ShapedRecipe recipe, Ingredient... leftovers) {
        return new CraftingWithLeftoverRecipe<>(FactoryRecipeSerializers.CRAFTING_SHAPED_LEFTOVER, recipe, List.of(leftovers));
    }

    public static CraftingWithLeftoverRecipe<ShapelessRecipe> of(ShapelessRecipe recipe, Ingredient... leftovers) {
        return new CraftingWithLeftoverRecipe<>(FactoryRecipeSerializers.CRAFTING_SHAPELESS_LEFTOVER, recipe, List.of(leftovers));
    }

    public static RecipeExporter asExporter(RecipeExporter exporter, Ingredient... leftover) {
        return new RecipeExporter() {
            @Override
            public void accept(RegistryKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementEntry advancement) {
                if (recipe instanceof ShapedRecipe shapedRecipe) {
                    exporter.accept(key, of(shapedRecipe, leftover), advancement);
                } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                    exporter.accept(key, of(shapelessRecipe, leftover), advancement);
                }
            }

            @Override
            public Advancement.Builder getAdvancementBuilder() {
                return exporter.getAdvancementBuilder();
            }

            @Override
            public void addRootAdvancement() {
                exporter.addRootAdvancement();
            }
        };
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return this.backingRecipe.matches(input, world);
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return this.backingRecipe.craft(input, registries);
    }

    @Override
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
        var remainders = this.backingRecipe.getRecipeRemainders(input  );
        for (var i = 0; i < input.size(); i++) {
            for (var ing : this.leftovers) {
                if (ing.test(input.getStackInSlot(i))) {
                    remainders.set(i, input.getStackInSlot(i).copyWithCount(1));
                }
            }
        }

        return remainders;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return serializer;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return this.backingRecipe.getIngredientPlacement();
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return this.backingRecipe.getCategory();
    }

    @Override
    public String getGroup() {
        return this.backingRecipe.getGroup();
    }

    @Override
    public List<RecipeDisplay> getDisplays() {
        return this.backingRecipe.getDisplays();
    }
}
