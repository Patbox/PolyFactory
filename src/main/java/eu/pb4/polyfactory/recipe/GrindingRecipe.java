package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.LazyRecipeSerializer;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public record GrindingRecipe(String group, Ingredient input, List<OutputStack> output, double grindTime, double minimumSpeed, double optimalSpeed) implements Recipe<GrinderBlockEntity>, PolymerRecipe {
    public static final MapCodec<GrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GrindingRecipe::group),
                    LazyRecipeSerializer.INGREDIENT_CODEC.fieldOf("input").forGetter(GrindingRecipe::input),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GrindingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(GrindingRecipe::grindTime),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GrindingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(GrindingRecipe::optimalSpeed)
            ).apply(x, GrindingRecipe::new)
    );

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( "", ingredient, List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( "", ingredient, List.of(outputs), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<GrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return of(string, "", ingredient, grindTime, optimalSpeed, outputs);
    }

    public static RecipeEntry<GrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("grinding/" + string), new GrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public boolean matches(GrinderBlockEntity inventory, World world) {
        return this.input.test(inventory.getStack(GrinderBlockEntity.INPUT_SLOT));
    }

    @Deprecated
    @Override
    public ItemStack craft(GrinderBlockEntity inventory, RegistryWrapper.WrapperLookup registryManager) {
        return this.output.isEmpty() ? ItemStack.EMPTY : this.output.get(0).stack();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Deprecated
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.GRINDING;
    }

    @Override
    public RecipeType<?> getType() {
        return FactoryRecipeTypes.GRINDING;
    }

    @Override
    public ItemStack createIcon() {
        return FactoryItems.GRINDER.getDefaultStack();
    }
}
