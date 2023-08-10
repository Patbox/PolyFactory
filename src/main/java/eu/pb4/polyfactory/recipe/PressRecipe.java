package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;

public record PressRecipe(Identifier identifier, CountedIngredient inputA, CountedIngredient inputB, List<OutputStack> output, double minimumSpeed) implements Recipe<PressBlockEntity>, PolymerRecipe {
    public static final Codec<PressRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    CountedIngredient.CODEC.fieldOf("input_a").forGetter(PressRecipe::inputA),
                    CountedIngredient.CODEC.optionalFieldOf("input_b", CountedIngredient.EMPTY).forGetter(PressRecipe::inputB),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(PressRecipe::output),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(PressRecipe::minimumSpeed)
            ).apply(x, PressRecipe::new)
    );

    public static PressRecipe of(String string, CountedIngredient inputA, CountedIngredient inputB, double minimumSpeed, OutputStack... outputs) {
        return new PressRecipe(FactoryUtil.id("press/" + string), inputA, inputB, List.of(outputs), minimumSpeed);
    }

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, OutputStack... outputs) {
        return new PressRecipe(FactoryUtil.id("press/" + string), new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(outputs), minimumSpeed);
    }

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemStack output) {
        return new PressRecipe(FactoryUtil.id("press/" + string), new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(new OutputStack(output, 1, 1)),  minimumSpeed);
    }

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, double minimumSpeed, ItemConvertible output) {
        return new PressRecipe(FactoryUtil.id("press/" + string), new CountedIngredient(ingredient, inputCount, ItemStack.EMPTY), CountedIngredient.EMPTY, List.of(new OutputStack(output.asItem().getDefaultStack(), 1, 1)), minimumSpeed);
    }

    @Override
    public boolean matches(PressBlockEntity inventory, World world) {
        var input = inventory.getStack(PressBlockEntity.INPUT_SLOT);
        var template = inventory.getStack(PressBlockEntity.INPUT_2_SLOT);
        return this.inputA.test(input) && this.inputB.test(template);
    }

    @Override
    public ItemStack craft(PressBlockEntity inventory, DynamicRegistryManager registryManager) {
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
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public Identifier getId() {
        return this.identifier;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.PRESS;
    }

    @Override
    public RecipeType<?> getType() {
        return FactoryRecipeTypes.PRESS;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(null, this.inputA.ingredient(), this.inputB.ingredient());
    }

    @Override
    public ItemStack createIcon() {
        return FactoryItems.PRESS_BLOCK.getDefaultStack();
    }
}
