package eu.pb4.polyfactory.recipe.grinding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.input.GrindingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record SimpleGrindingRecipe(String group, Ingredient input, List<OutputStack> output, double grindTime, double minimumSpeed, double optimalSpeed) implements GrindingRecipe {
    public static final MapCodec<SimpleGrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(SimpleGrindingRecipe::group),
                    Ingredient.CODEC.fieldOf("input").forGetter(SimpleGrindingRecipe::input),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(simpleGrindingRecipe -> simpleGrindingRecipe.output()),
                    Codec.DOUBLE.fieldOf("time").forGetter(simpleGrindingRecipe -> simpleGrindingRecipe.grindTime()),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(simpleGrindingRecipe -> simpleGrindingRecipe.minimumSpeed()),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(simpleGrindingRecipe -> simpleGrindingRecipe.optimalSpeed())
            ).apply(x, SimpleGrindingRecipe::new)
    );

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( "", ingredient, List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( "", ingredient, List.of(outputs), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( group, ingredient, List.of(outputs), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( group, ingredient, List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( "", ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return of(string, "", ingredient, grindTime, optimalSpeed, outputs);
    }

    public static RecipeEntry<SimpleGrindingRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new SimpleGrindingRecipe( group, ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, 0, optimalSpeed));
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
        return this.output.isEmpty() ? ItemStack.EMPTY : this.output.getFirst().stack();
    }

    @Override
    public RecipeSerializer<SimpleGrindingRecipe> getSerializer() {
        return FactoryRecipeSerializers.GRINDING_SIMPLE;
    }

    @Override
    public List<ItemStack> output(GrindingInput input, RegistryWrapper.WrapperLookup registryManager, @Nullable Random random) {
        var items = new ArrayList<ItemStack>();

        for (var out : this.output) {
            for (int a = 0; a < out.roll(); a++) {
                if (random == null || random.nextFloat() < out.chance()) {
                    items.add(out.stack().copy());
                }
            }
        }

        return items;
    }

    @Override
    public double grindTime(GrindingInput input) {
        return this.grindTime;
    }

    @Override
    public double minimumSpeed(GrindingInput input) {
        return this.minimumSpeed;
    }

    @Override
    public double optimalSpeed(GrindingInput input) {
        return this.optimalSpeed;
    }
}
