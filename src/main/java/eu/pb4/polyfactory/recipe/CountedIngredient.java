package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

import java.util.List;

public record CountedIngredient(Ingredient ingredient, int count) {
    public static final Codec<CountedIngredient> CODEC_SELF = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.INGREDIENT_CODEC.fieldOf("base").forGetter(CountedIngredient::ingredient),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
            ).apply(x, CountedIngredient::new)
    );

    public static final Codec<CountedIngredient> CODEC = Codec.either(CODEC_SELF, CodecRecipeSerializer.INGREDIENT_CODEC)
            .xmap(x -> x.map(y -> y, y -> new CountedIngredient(y, 1)), Either::left);
    public static final Codec<List<CountedIngredient>> LIST_CODEC = Codec.either(CODEC, Codec.list(CODEC))
            .xmap(x -> x.map(y -> List.of(y), y -> y), x -> x.size() == 1 ? Either.left(x.get(0)) : Either.right(x));

    public static CountedIngredient ofItems(int count, ItemConvertible... items) {
        return new CountedIngredient(Ingredient.ofItems(items), count);
    }

    public static CountedIngredient ofStacks(int count, ItemStack... stacks) {
        return new CountedIngredient(Ingredient.ofStacks(stacks), count);
    }

    public static CountedIngredient fromTag(int count, TagKey<Item> tag) {
        return new CountedIngredient(Ingredient.fromTag(tag), count);
    }

    public boolean test(ItemStack stack) {
        return stack.getCount() >= this.count && this.ingredient.test(stack);
    }

}