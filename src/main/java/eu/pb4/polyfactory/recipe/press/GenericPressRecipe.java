package eu.pb4.polyfactory.recipe.press;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
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

public record GenericPressRecipe(CountedIngredient inputA, CountedIngredient inputB, List<OutputStack> output, double minimumSpeed) implements PressRecipe {
    public static final MapCodec<GenericPressRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    CountedIngredient.CODEC.fieldOf("input_a").forGetter(GenericPressRecipe::inputA),
                    CountedIngredient.CODEC.optionalFieldOf("input_b", CountedIngredient.EMPTY).forGetter(GenericPressRecipe::inputB),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GenericPressRecipe::output),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GenericPressRecipe::minimumSpeed)
            ).apply(x, GenericPressRecipe::new)
    );

    public static RecipeEntry<GenericPressRecipe> of(String string, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("press/" + string), new GenericPressRecipe(inputA, inputB, List.of(outputs), minimumSpeed));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.id("press/" + string), new GenericPressRecipe(new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(outputs), minimumSpeed));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("press/" + string), new GenericPressRecipe(new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(new OutputStack(output, 1, 1)),  minimumSpeed));
    }

    public static RecipeEntry<GenericPressRecipe> of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemConvertible output) {
        return new RecipeEntry<>(FactoryUtil.id("press/" + string), new GenericPressRecipe(new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(new OutputStack(output.asItem().getDefaultStack(), 1, 1)), minimumSpeed));
    }

    @Override
    public boolean matches(PressInput inventory, World world) {
        var input = inventory.pattern();
        var template = inventory.input();
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
    public boolean fits(int width, int height) {
        return true;
    }

    @Deprecated
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return this.output.isEmpty() ? ItemStack.EMPTY : this.output.get(0).stack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.PRESS_GENERIC;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(null, this.inputA.ingredient(), this.inputB.ingredient());
    }

    @Override
    public ItemStack createIcon() {
        return FactoryItems.PRESS.getDefaultStack();
    }
}
