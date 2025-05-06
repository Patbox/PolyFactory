package eu.pb4.polyfactory.recipe.smeltery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.Item;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;

import java.util.List;

public record SimpleSmelteryRecipe(String group, Ingredient ingredient, List<FluidStack<?>> output, int time) implements SmelteryRecipe {
    public static final MapCodec<SimpleSmelteryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(SimpleSmelteryRecipe::group),
            Ingredient.CODEC.fieldOf("input").forGetter(SimpleSmelteryRecipe::ingredient),
            Codecs.listOrSingle(FluidStack.CODEC).fieldOf("output").forGetter(SimpleSmelteryRecipe::output),
            Codec.INT.fieldOf("time").forGetter(SimpleSmelteryRecipe::time)
    ).apply(instance, SimpleSmelteryRecipe::new));

    public static RecipeEntry<SimpleSmelteryRecipe> of(String path, String group, Ingredient ingredient, List<FluidStack<?>> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe(group, ingredient, output, time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(String path, Ingredient ingredient, List<FluidStack<?>> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe("", ingredient, output, time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(String path, String group, Ingredient ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe(group, ingredient, List.of(output), time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(String path, Ingredient ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/" + path), new SimpleSmelteryRecipe("", ingredient, List.of(output), time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(String group, Item ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/from_" + Registries.ITEM.getId(ingredient).getPath()), new SimpleSmelteryRecipe(group, Ingredient.ofItems(ingredient), List.of(output), time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(Item ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/from_" + Registries.ITEM.getId(ingredient).getPath()), new SimpleSmelteryRecipe("",  Ingredient.ofItems(ingredient), List.of(output), time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(String group, TagKey<Item> ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/from_" + ingredient.id().getPath()), new SimpleSmelteryRecipe(group, Ingredient.fromTag(FactoryUtil.fakeTagList(ingredient)), List.of(output), time));
    }

    public static RecipeEntry<SimpleSmelteryRecipe> of(TagKey<Item> ingredient, FluidStack<?> output, int time) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("smeltery/from_" + ingredient.id().getPath()), new SimpleSmelteryRecipe("",  Ingredient.fromTag(FactoryUtil.fakeTagList(ingredient)), List.of(output), time));
    }

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        return ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleStackRecipeInput>> getSerializer() {
        return FactoryRecipeSerializers.SMELTERY;
    }

    @Override
    public List<FluidStack<?>> output(SingleStackRecipeInput input, World world) {
        return this.output;
    }

    @Override
    public int time(SingleStackRecipeInput input, World world) {
        return this.time;
    }

    @Override
    public String getGroup() {
        return this.group;
    }
}
