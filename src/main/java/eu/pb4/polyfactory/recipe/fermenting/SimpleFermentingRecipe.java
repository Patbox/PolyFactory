package eu.pb4.polyfactory.recipe.fermenting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SingleItemWithTemperature;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record SimpleFermentingRecipe(String group, Ingredient input,
                                     List<OutputStack> output, List<FluidStack<?>> fluidOutput,
                                     double time,
                                     float minimumTemperature, float maxTemperature) implements FermentingRecipe {
    public static final MapCodec<SimpleFermentingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(SimpleFermentingRecipe::group),
                    Ingredient.CODEC.fieldOf("input").forGetter(SimpleFermentingRecipe::input),
                    ExtraCodecs.compactListCodec(OutputStack.CODEC).optionalFieldOf("output", List.of()).forGetter(SimpleFermentingRecipe::output),
                    FluidStack.CODEC.listOf().fieldOf("output_fluid").forGetter(SimpleFermentingRecipe::fluidOutput),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleFermentingRecipe::time),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", Float.NEGATIVE_INFINITY).forGetter(SimpleFermentingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(SimpleFermentingRecipe::maxTemperature)
            ).apply(x, SimpleFermentingRecipe::new)
    );

    public static RecipeHolder<SimpleFermentingRecipe> of(String id, String group, Ingredient input, Item result, double time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("fermenting/" + id), new SimpleFermentingRecipe(group, input, List.of(OutputStack.of(result)),
                List.of(), time, 0, 1));
    }

    public static RecipeHolder<SimpleFermentingRecipe> of(String id, String group, Ingredient input, OutputStack outputItem, FluidStack<?> outputFluid, double time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("fermenting/" + id), new SimpleFermentingRecipe(group, input, List.of(outputItem),
                List.of(outputFluid), time, 0, 1));
    }

    public static RecipeHolder<SimpleFermentingRecipe> of(String id, String group, Ingredient input, FluidStack<?> outputFluid, double time) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("fermenting/" + id), new SimpleFermentingRecipe(group, input, List.of(),
                List.of(outputFluid), time, 0, 1));
    }


    @Override
    public String group() {
        return this.group;
    }

    @Override
    public boolean matches(SingleItemWithTemperature input, Level level) {
        return this.input.test(input.stack()) && input.temperature() >= this.minimumTemperature && input.temperature() <= this.maxTemperature;
    }

    @Override
    public List<ItemStack> assembleStacks(SingleItemWithTemperature input, RandomSource randomSource, boolean applyChance) {
        if (output.isEmpty()) {
            return List.of();
        }
        var list = new ArrayList<ItemStack>();

        for (var out : this.output) {
            for (int i = 0; i < out.roll(); i++) {
                if (!applyChance || out.roll() >= randomSource.nextFloat()) {
                    list.add(out.stack().create());
                }
            }
        }
        return list;
    }

    @Override
    public RecipeSerializer<SimpleFermentingRecipe> getSerializer() {
        return FactoryRecipeSerializers.FERMENTING_SIMPLE;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(SingleItemWithTemperature input) {
        return this.fluidOutput;
    }

    @Override
    public double time(SingleItemWithTemperature input) {
        return this.time;
    }
}
