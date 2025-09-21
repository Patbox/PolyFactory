package eu.pb4.polyfactory.recipe.press;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public record GenericPressRecipe(String group, CountedIngredient inputA, CountedIngredient inputB, List<OutputStack> output, double minimumSpeed, List<FluidStack<?>> outputFluids) implements PressRecipe {
    public static final MapCodec<GenericPressRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GenericPressRecipe::group),
                    CountedIngredient.CODEC.fieldOf("input_a").forGetter(GenericPressRecipe::inputA),
                    CountedIngredient.CODEC.optionalFieldOf("input_b", CountedIngredient.EMPTY).forGetter(GenericPressRecipe::inputB),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GenericPressRecipe::output),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GenericPressRecipe::minimumSpeed),
                    FluidStack.CODEC.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(GenericPressRecipe::outputFluids)
            ).apply(x, GenericPressRecipe::new)
    );

    public static RecipeEntry<GenericPressRecipe> of(String string, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", inputA, inputB, List.of(outputs), minimumSpeed, List.of()));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, String group, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe(group, inputA, inputB, List.of(outputs), minimumSpeed, List.of()));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY, List.of(outputs), minimumSpeed, List.of()));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, String group, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, List<OutputStack> outputs, List<FluidStack<?>> outputFluids) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe(group, inputA, inputB, outputs, minimumSpeed, outputFluids));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, List<OutputStack> outputs, List<FluidStack<?>> outputFluids) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY,outputs, minimumSpeed, outputFluids));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY),
                CountedIngredient.EMPTY, List.of(new OutputStack(output, 1, 1)),  minimumSpeed, List.of()));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemConvertible output) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("press/" + string), new GenericPressRecipe("", new CountedIngredient(Optional.of(ingredient), ItemComponentPredicate.EMPTY, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY,
                List.of(new OutputStack(output.asItem().getDefaultStack(), 1, 1)), minimumSpeed, List.of()));
    }

    @Override
    public boolean matches(PressInput inventory, World world) {
        var input = inventory.input();
        var template = inventory.pattern();
        return this.inputA.test(input) && this.inputB.test(template);
    }

    @Override
    public void applyRecipeUse(PressBlockEntity inventory, World world) {
        var stack = inventory.getStack(PressBlockEntity.INPUT_SLOT);
        stack.decrement(this.inputA.count());
        if (stack.isEmpty()) {
            inventory.setStack(PressBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
        }

        stack = inventory.getStack(PressBlockEntity.INPUT_2_SLOT);
        stack.decrement(this.inputB.count());
        if (stack.isEmpty()) {
            inventory.setStack(PressBlockEntity.INPUT_2_SLOT, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack craft(PressInput input, RegistryWrapper.WrapperLookup registryManager) {
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
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<GenericPressRecipe> getSerializer() {
        return FactoryRecipeSerializers.PRESS_GENERIC;
    }

}
