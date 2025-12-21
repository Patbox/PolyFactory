package eu.pb4.polyfactory.recipe.smeltery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import java.util.List;

public record SimpleSmelteryRecipe(String group, Ingredient ingredient, List<FluidStack<?>> output, int time) implements SmelteryRecipe {
    public static final MapCodec<SimpleSmelteryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(SimpleSmelteryRecipe::group),
            Ingredient.CODEC.fieldOf("input").forGetter(SimpleSmelteryRecipe::ingredient),
            ExtraCodecs.compactListCodec(FluidStack.CODEC).fieldOf("output").forGetter(SimpleSmelteryRecipe::output),
            Codec.INT.fieldOf("time").forGetter(SimpleSmelteryRecipe::time)
    ).apply(instance, SimpleSmelteryRecipe::new));

    public static RecipeHolder<SimpleSmelteryRecipe> of(String path, String group, Ingredient ingredient, List<FluidStack<?>> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe(group, ingredient, output, time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(String path, Ingredient ingredient, List<FluidStack<?>> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe("", ingredient, output, time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(String path, String group, Ingredient ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe(group, ingredient, List.of(output), time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(String path, Ingredient ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe("", ingredient, List.of(output), time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(String group, Item ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/from_" + BuiltInRegistries.ITEM.getKey(ingredient).getPath()), new SimpleSmelteryRecipe(group, Ingredient.of(ingredient), List.of(output), time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(Item ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/from_" + BuiltInRegistries.ITEM.getKey(ingredient).getPath()), new SimpleSmelteryRecipe("",  Ingredient.of(ingredient), List.of(output), time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(String group, TagKey<Item> ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/from_" + ingredient.location().getPath()), new SimpleSmelteryRecipe(group, Ingredient.of(FactoryUtil.fakeTagList(ingredient)), List.of(output), time));
    }

    public static RecipeHolder<SimpleSmelteryRecipe> of(TagKey<Item> ingredient, FluidStack<?> output, int time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("smeltery/from_" + ingredient.location().getPath()), new SimpleSmelteryRecipe("",  Ingredient.of(FactoryUtil.fakeTagList(ingredient)), List.of(output), time));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        return ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return FactoryRecipeSerializers.SMELTERY;
    }

    @Override
    public List<FluidStack<?>> output(SingleRecipeInput input, Level world) {
        return this.output;
    }

    @Override
    public int time(SingleRecipeInput input, Level world) {
        return this.time;
    }

    @Override
    public String group() {
        return this.group;
    }
}
