package eu.pb4.polyfactory.recipe.press;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public record GenericPressRecipe(String group, CountedIngredient inputA, CountedIngredient inputB, List<OutputStack> output, double minimumSpeed,
                                 List<FluidStack<?>> outputFluids, TriState requireFluidOutput) implements PressRecipe {
    public static final MapCodec<GenericPressRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GenericPressRecipe::group),
                    CountedIngredient.CODEC.fieldOf("input_a").forGetter(GenericPressRecipe::inputA),
                    CountedIngredient.CODEC.optionalFieldOf("input_b", CountedIngredient.EMPTY).forGetter(GenericPressRecipe::inputB),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GenericPressRecipe::output),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GenericPressRecipe::minimumSpeed),
                    FluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(GenericPressRecipe::outputFluids),
                    TriState.CODEC.optionalFieldOf("require_fluid_output", TriState.DEFAULT).forGetter(GenericPressRecipe::requireFluidOutput)
            ).apply(x, GenericPressRecipe::new)
    );

    public static RecipeHolder<GenericPressRecipe> of(String string, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", inputA, inputB, List.of(outputs), minimumSpeed, List.of(), TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, String group, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe(group, inputA, inputB, List.of(outputs), minimumSpeed, List.of(), TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY, List.of(outputs), minimumSpeed, List.of(), TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, String group, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, List<OutputStack> outputs, List<FluidStack<?>> outputFluids) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe(group, inputA, inputB, outputs, minimumSpeed, outputFluids, TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, List<OutputStack> outputs, List<FluidStack<?>> outputFluids) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY,outputs, minimumSpeed, outputFluids, TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemStack output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY, List.of(new OutputStack(output, 1, 1)),  minimumSpeed, List.of(), TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemStack output, FluidStack<?> fluidOutput) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY, List.of(new OutputStack(output, 1, 1)),  minimumSpeed, List.of(fluidOutput), TriState.DEFAULT));
    }

    public static RecipeHolder<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemLike output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY,
                List.of(new OutputStack(output.asItem().getDefaultInstance(), 1, 1)), minimumSpeed, List.of(), TriState.DEFAULT));
    }

    @Override
    public boolean matches(PressInput inventory, Level world) {
        var input = inventory.input();
        var template = inventory.pattern();
        return this.inputA.test(input) && this.inputB.test(template);
    }

    @Override
    public void applyRecipeUse(PressBlockEntity inventory, Level world) {
        var stack = inventory.getItem(PressBlockEntity.INPUT_SLOT);
        stack.shrink(this.inputA.count());
        if (stack.isEmpty()) {
            inventory.setItem(PressBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
        }

        stack = inventory.getItem(PressBlockEntity.INPUT_2_SLOT);
        stack.shrink(this.inputB.count());
        if (stack.isEmpty()) {
            inventory.setItem(PressBlockEntity.INPUT_2_SLOT, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack assemble(PressInput input, HolderLookup.Provider registryManager) {
        for (var out : output) {
            if (Math.random() <= out.chance()) {
                return out.stack().copy();
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public List<FluidStack<?>> outputFluids(PressInput input) {
        return this.outputFluids;
    }


    @Override
    public boolean fluidsRequired() {
        return requireFluidOutput.toBoolean(this.output().isEmpty());
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public RecipeSerializer<GenericPressRecipe> getSerializer() {
        return FactoryRecipeSerializers.PRESS_GENERIC;
    }

}
