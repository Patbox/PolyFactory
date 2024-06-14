package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

public class ShapelessNbtCopyRecipe extends ShapelessRecipe implements PolymerRecipe {
    private final ItemStack result;
    private final Ingredient source;
    private final DefaultedList<Ingredient> ingredientsOg;
    public static MapCodec<ShapelessNbtCopyRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessNbtCopyRecipe::getGroup),
                        CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessNbtCopyRecipe::getCategory),
                        ItemStack.CODEC.fieldOf("result").forGetter(t -> t.result),
                        Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("source").forGetter(t -> t.source),
                        Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").flatXmap((ingredients) -> {
                            Ingredient[] ingredients2 = (Ingredient[]) ingredients.stream().filter((ingredient) -> {
                                return !ingredient.isEmpty();
                            }).toArray((i) -> {
                                return new Ingredient[i];
                            });

                            return ingredients2.length > 8 ? DataResult.error(() -> {
                                return "Too many ingredients for shapeless recipe";
                            }) : DataResult.success(DefaultedList.copyOf(Ingredient.EMPTY, ingredients2));
                        }, DataResult::success).forGetter(t -> t.ingredientsOg))
                .apply(instance, ShapelessNbtCopyRecipe::new);
    });
    private Recipe<?> vanilla;

    public ShapelessNbtCopyRecipe(String group, CraftingRecipeCategory category, ItemStack result, Ingredient source, DefaultedList<Ingredient> ingredients) {
        super(group, category, result, merge(ingredients, source));
        this.ingredientsOg = ingredients;
        this.result = result;
        this.source = source;

        this.vanilla = new ShapelessRecipe(group, category, result, this.getIngredients());
    }

    private static DefaultedList<Ingredient> merge(DefaultedList<Ingredient> ingredients, Ingredient source) {
        var list = DefaultedList.<Ingredient>of();
        list.add(source);
        list.addAll(ingredients);
        return list;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput recipeInputInventory, RegistryWrapper.WrapperLookup dynamicRegistryManager) {
        var stack = super.craft(recipeInputInventory, dynamicRegistryManager);
        for (var tmp : recipeInputInventory.getStacks()) {
            if (this.source.test(tmp)) {
                stack.applyComponentsFrom(tmp.getComponents());
                break;
            }
        }
        return stack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_SHAPELESS_NBT_COPY;
    }

    @Override
    public @Nullable Recipe<?> getPolymerReplacement(ServerPlayerEntity player) {
        return this.vanilla;
    }
}
