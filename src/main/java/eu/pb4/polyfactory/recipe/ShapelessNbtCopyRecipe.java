package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;

public class ShapelessNbtCopyRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final Ingredient source;
    private final List<Ingredient> ingredientsOg;
    public static MapCodec<ShapelessNbtCopyRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessNbtCopyRecipe::getGroup),
                        CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessNbtCopyRecipe::getCategory),
                        ItemStack.CODEC.fieldOf("result").forGetter(t -> t.result),
                        Ingredient.CODEC.fieldOf("source").forGetter(t -> t.source),
                        Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap((ingredients) -> {
                            return ingredients.size() > 8 ? DataResult.error(() -> {
                                return "Too many ingredients for shapeless recipe";
                            }) : DataResult.success(ingredients);
                        }, DataResult::success).forGetter(t -> t.ingredientsOg))
                .apply(instance, ShapelessNbtCopyRecipe::new);
    });

    public ShapelessNbtCopyRecipe(String group, CraftingRecipeCategory category, ItemStack result, Ingredient source, List<Ingredient> ingredients) {
        super(group, category, result, merge(ingredients, source));
        this.ingredientsOg = ingredients;
        this.result = result;
        this.source = source;
    }

    private static List<Ingredient> merge(List<Ingredient> ingredients, Ingredient source) {
        var list = new ArrayList<Ingredient>();
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
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        //noinspection unchecked
        return (RecipeSerializer<ShapelessRecipe>) (Object) FactoryRecipeSerializers.CRAFTING_SHAPELESS_NBT_COPY;
    }
}
