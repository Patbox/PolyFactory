package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.input.GrindingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public record GrindingRecipe(String group, Ingredient input, List<OutputStack> output, double grindTime, double minimumSpeed, double optimalSpeed) implements Recipe<GrindingInput> {
    public static final MapCodec<GrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GrindingRecipe::group),
                    Ingredient.CODEC.fieldOf("input").forGetter(GrindingRecipe::input),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GrindingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(GrindingRecipe::grindTime),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GrindingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(GrindingRecipe::optimalSpeed)
            ).apply(x, GrindingRecipe::new)
    );

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( "", ingredient, List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( "", ingredient, List.of(outputs), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return of(string, "", ingredient, grindTime, optimalSpeed, outputs);
    }

    public static RecipeEntry<GrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new GrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public boolean matches(GrindingInput inventory, World world) {
        return this.input.test(inventory.getStackInSlot(GrinderBlockEntity.INPUT_SLOT));
    }

    @Deprecated
    @Override
    public ItemStack craft(GrindingInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        return this.output.isEmpty() ? ItemStack.EMPTY : this.output.get(0).stack();
    }

    @Override
    public RecipeSerializer<GrindingRecipe> getSerializer() {
        return FactoryRecipeSerializers.GRINDING;
    }

    @Override
    public RecipeType<GrindingRecipe> getType() {
        return FactoryRecipeTypes.GRINDING;
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }
}
