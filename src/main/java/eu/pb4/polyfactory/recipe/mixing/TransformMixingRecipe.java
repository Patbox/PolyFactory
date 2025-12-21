package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record TransformMixingRecipe(String group, Ingredient base, List<CountedIngredient> input,
                                    Optional<List<FluidInputStack>> fluidInputs,
                                    ItemStack output, List<FluidStack<?>> fluidOutput, double time,
                                    double minimumSpeed,
                                    double optimalSpeed, float minimumTemperature, float maxTemperature) implements MixingRecipe {
    public static final MapCodec<TransformMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(TransformMixingRecipe::group),
                    Ingredient.CODEC.fieldOf("base").forGetter(TransformMixingRecipe::base),
                    CountedIngredient.LIST_CODEC.fieldOf("input").forGetter(TransformMixingRecipe::input),
                    FluidInputStack.CODEC.listOf().optionalFieldOf("fluid_input").forGetter(TransformMixingRecipe::fluidInputs),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("output").forGetter(TransformMixingRecipe::output),
                    FluidStack.CODEC.listOf().fieldOf("output_fluid").forGetter(TransformMixingRecipe::fluidOutput),
                    Codec.DOUBLE.fieldOf("time").forGetter(TransformMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(TransformMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(TransformMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", Float.NEGATIVE_INFINITY).forGetter(TransformMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(TransformMixingRecipe::maxTemperature)
            ).apply(x, TransformMixingRecipe::new)
    );

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, Ingredient base, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe("", base, ingredient,
                Optional.of(List.of()),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -0.1f, Float.POSITIVE_INFINITY));
    }

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, Ingredient base, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe("", base, ingredient,
                Optional.of(List.of()),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, String group, Ingredient base, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe(group, base, ingredient,
                Optional.of(List.of()),
                output,List.of(), mixingTime, minimumSpeed, optimalSpeed, -0.1f, Float.POSITIVE_INFINITY));
    }

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, String group, Ingredient base, List<CountedIngredient> ingredient, List<FluidInputStack> fluids, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe(group, base, ingredient,
                Optional.ofNullable(fluids),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, String group, Ingredient base, List<CountedIngredient> ingredient, List<FluidInputStack> fluidInput, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output, List<FluidStack<?>> fluidOutput) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe(group, base, ingredient,
                Optional.ofNullable(fluidInput),
                output, fluidOutput, mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeHolder<TransformMixingRecipe> ofCounted(String string, String group, Ingredient base, List<CountedIngredient> ingredient, List<FluidInputStack> fluidInput, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, float maxTemperature, ItemStack output, List<FluidStack<?>> fluidOutput) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe(group, base, ingredient,
                Optional.ofNullable(fluidInput),
                output, fluidOutput, mixingTime, minimumSpeed, optimalSpeed, minTemperature, maxTemperature));
    }

    public Iterable<ItemStack> remainders(MixingInput input) {
        return () -> Iterators.transform(this.input.iterator(), (a) -> a.leftOver().copy());
    }

    public static RecipeHolder<TransformMixingRecipe> of(String string, Ingredient base, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(Optional.of(x), ItemComponentPredicate.EMPTY, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe("", base, list, Optional.of(List.of()), output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    public static RecipeHolder<TransformMixingRecipe> of(String string, String group, Ingredient base, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(Optional.of(x), ItemComponentPredicate.EMPTY, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new TransformMixingRecipe(group, base, list, Optional.of(List.of()), output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public boolean matches(MixingInput inventory, Level world) {
        if (this.fluidInputs.isPresent()) {
            for (var fluid : this.fluidInputs.get()) {
                if (inventory.getFluid(fluid.instance()) < fluid.required()) {
                    return false;
                }
            }
        }

        var noBase = true;
        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (this.base.test(stack) && stack.getItem() != this.output.getItem()) {
                noBase = false;
                break;
            }
        }

        if (noBase) {
            return false;
        }

        if (!this.input.isEmpty() && !this.input.getFirst().ingredient().isEmpty()) {
            var map = new Object2IntArrayMap<CountedIngredient>();

            for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
                var stack = inventory.getItem(i);
                if (stack.isEmpty() || this.base.test(stack)) {
                    continue;
                }
                var notFound = true;
                for (var ig : this.input) {
                    if (ig.test(stack)) {
                        map.put(ig, map.getInt(ig) + stack.getCount());
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    return false;
                }
            }

            if (this.input.size() != map.size()) {
                return false;
            }

            for (var entry : map.object2IntEntrySet()) {
                if (entry.getKey().count() > entry.getIntValue()) {
                    return false;
                }
            }
        } else if (!inventory.isSlotEmpty()) {
            return false;
        }

        return true;
    }

    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        var list = new ArrayList<>(this.input);

        var container = inventory.getFluidContainer();
        if (this.fluidInputs.isPresent()) {
            for (var fluid : this.fluidInputs.get()) {
                container.extract(fluid.instance(), fluid.used(), false);
            }
        }

        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (this.base.test(stack) && stack.getItem() != this.output.getItem()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }

        for (var ig : list) {
            int count = ig.count();
            if (count == 0) {
                continue;
            }

            for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
                var stack = inventory.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }

                if (ig.test(stack)) {
                    if (stack.getCount() >= count) {
                        stack.shrink(count);
                        inventory.setItem(i, stack);
                        break;
                    } else {
                        count -= stack.getCount();
                        stack.setCount(0);
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    public List<FluidInputStack> fluidInput() {
        if (this.fluidInputs.isPresent()) {
            return this.fluidInputs.get();
        }
        return List.of();
    }

    @Override
    public ItemStack assemble(MixingInput inventory, HolderLookup.Provider registryManager) {
        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (this.base.test(stack) && stack.getItem() != this.output.getItem()) {
                var output = this.output.copyWithCount(1);
                output.applyComponentsAndValidate(stack.getComponentsPatch());
                return output;
            }
        }

        return this.output.copy();
    }
    @Override
    public RecipeSerializer<TransformMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_TRANSFORM;
    }

    @Override
    public double optimalSpeed(MixingInput input) {
        return this.optimalSpeed;
    }

    @Override
    public double minimumSpeed(MixingInput input) {
        return this.minimumSpeed;
    }

    @Override
    public float minimumTemperature(MixingInput input) {
        return this.minimumTemperature;
    }

    @Override
    public float maxTemperature(MixingInput input) {
        return this.maxTemperature;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(MixingInput input) {
        return this.fluidOutput;
    }

    @Override
    public List<FluidInputStack> fluidInput(MixingInput input) {
        return this.fluidInputs.isPresent() ? this.fluidInputs.get() : List.of();
    }

    @Override
    public double time(MixingInput input) {
        return this.time;
    }
}
