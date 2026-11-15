package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import java.util.ArrayList;
import java.util.List;

public class ShapelessComponentCopyRecipe extends ShapelessRecipe {
    private final ItemStackTemplate result;
    private final Ingredient source;
    private final List<Ingredient> ingredientsOg;
    private final List<DataComponentType<?>> copiedComponents;

    public static MapCodec<ShapelessComponentCopyRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
                        CommonInfo.MAP_CODEC.forGetter(t -> t.commonInfo),
                        CraftingBookInfo.MAP_CODEC.forGetter(t -> t.bookInfo),
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(t -> t.result),
                        Ingredient.CODEC.fieldOf("source").forGetter(t -> t.source),
                        Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap((ingredients) -> {
                            return ingredients.size() > 8 ? DataResult.error(() -> {
                                return "Too many ingredients for shapeless recipe";
                            }) : DataResult.success(ingredients);
                        }, DataResult::success).forGetter(t -> t.ingredientsOg),
                        ExtraCodecs.compactListCodec(BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec()).fieldOf("components").forGetter(t -> t.copiedComponents))
                .apply(instance, ShapelessComponentCopyRecipe::new);
    });

    public ShapelessComponentCopyRecipe(CommonInfo info, CraftingBookInfo craftingBookInfo, ItemStackTemplate result, Ingredient source, List<Ingredient> ingredients,
                                        List<DataComponentType<?>> copiedComponents) {
        super(info, craftingBookInfo, result, merge(ingredients, source));
        this.ingredientsOg = ingredients;
        this.result = result;
        this.source = source;
        this.copiedComponents = copiedComponents;
    }

    private static List<Ingredient> merge(List<Ingredient> ingredients, Ingredient source) {
        var list = new ArrayList<Ingredient>();
        list.add(source);
        list.addAll(ingredients);
        return list;
    }

    @Override
    public ItemStack assemble(CraftingInput recipeInputInventory) {
        var stack = super.assemble(recipeInputInventory);
        for (var tmp : recipeInputInventory.items()) {
            if (this.source.test(tmp)) {
                //noinspection rawtypes
                for (DataComponentType type : this.copiedComponents) {
                    stack.set(type, tmp.get(type));
                }
                break;
            }
        }
        return stack;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        //noinspection unchecked
        return (RecipeSerializer<ShapelessRecipe>) (Object) FactoryRecipeSerializers.CRAFTING_SHAPELESS_COMPONENT_COPY;
    }
}
