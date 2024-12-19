package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record GenericMixingRecipe(String group, List<CountedIngredient> input,
                                  Optional<List<FluidInputStack>> fluidInputs,
                                  ItemStack output, List<FluidStack<?>> fluidOutput, double time,
                                  double minimumSpeed,
                                  double optimalSpeed, float minimumTemperature, float maxTemperature) implements MixingRecipe {
    public static final MapCodec<GenericMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GenericMixingRecipe::group),
                    CountedIngredient.LIST_CODEC.fieldOf("input").forGetter(GenericMixingRecipe::input),
                    FluidInputStack.CODEC.listOf().optionalFieldOf("fluid_input").forGetter(GenericMixingRecipe::fluidInputs),
                    ItemStack.OPTIONAL_CODEC.fieldOf("output").forGetter(GenericMixingRecipe::output),
                    FluidStack.CODEC.listOf().fieldOf("output_fluid").forGetter(GenericMixingRecipe::fluidOutput),
                    Codec.DOUBLE.fieldOf("time").forGetter(GenericMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(GenericMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(GenericMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", Float.NEGATIVE_INFINITY).forGetter(GenericMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(GenericMixingRecipe::maxTemperature)
            ).apply(x, GenericMixingRecipe::new)
    );

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", ingredient,
                Optional.of(List.of()),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -0.1f, Float.POSITIVE_INFINITY));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", ingredient,
                Optional.of(List.of()),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, String group, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe(group, ingredient,
                Optional.of(List.of()),
                output,List.of(), mixingTime, minimumSpeed, optimalSpeed, -0.1f, Float.POSITIVE_INFINITY));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, String group, List<CountedIngredient> ingredient, List<FluidInputStack> fluids, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe(group, ingredient,
                Optional.ofNullable(fluids),
                output, List.of(), mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, String group, List<CountedIngredient> ingredient, List<FluidInputStack> fluidInput, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output, List<FluidStack<?>> fluidOutput) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe(group, ingredient,
                Optional.ofNullable(fluidInput),
                output, fluidOutput, mixingTime, minimumSpeed, optimalSpeed, minTemperature, Float.POSITIVE_INFINITY));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, String group, List<CountedIngredient> ingredient, List<FluidInputStack> fluidInput, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, float maxTemperature, ItemStack output, List<FluidStack<?>> fluidOutput) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe(group, ingredient,
                Optional.ofNullable(fluidInput),
                output, fluidOutput, mixingTime, minimumSpeed, optimalSpeed, minTemperature, maxTemperature));
    }

    public Iterable<ItemStack> remainders(MixingInput input) {
        return () -> Iterators.transform(this.input.iterator(), (a) -> a.leftOver().copy());
    }

    public static RecipeEntry<GenericMixingRecipe> of(String string, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(x, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", list, Optional.of(List.of()), output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    public static RecipeEntry<GenericMixingRecipe> of(String string, String group, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(Optional.of(x), ItemComponentPredicate.EMPTY, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new RecipeEntry<>(FactoryUtil.recipeKey("mixing/" + string), new GenericMixingRecipe(group, list, Optional.of(List.of()), output, List.of(), mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        var list = DefaultedList.<Ingredient>of();
        for (var i : input) {
            list.add(i.ingredient());
        }
        return list;
    }

    @Override
    public boolean matches(MixingInput inventory, World world) {
        if (this.fluidInputs.isPresent()) {
            for (var fluid : this.fluidInputs.get()) {
                if (inventory.getFluid(fluid.instance()) < fluid.required()) {
                    return false;
                }
            }
        }


        if (!this.input.isEmpty() && !this.input.getFirst().ingredient().isEmpty()) {
            var map = new Object2IntArrayMap<CountedIngredient>();

            for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                var notFound = true;
                for (var ig : this.input) {
                    if (ig.ingredient().test(stack)) {
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

    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        var list = new ArrayList<>(this.input);

        var container = inventory.getFluidContainer();
        if (this.fluidInputs.isPresent()) {
            for (var fluid : this.fluidInputs.get()) {
                container.extract(fluid.instance(), fluid.used(), false);
            }
        }

        for (var ig : list) {
            int count = ig.count();
            if (count == 0) {
                continue;
            }

            for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
                var stack = inventory.getStack(i);
                if (stack.isEmpty()) {
                    continue;
                }

                if (ig.ingredient().test(stack)) {
                    if (stack.getCount() >= count) {
                        stack.decrement(count);
                        inventory.setStack(i, stack);
                        break;
                    } else {
                        count -= stack.getCount();
                        stack.setCount(0);
                        inventory.setStack(i, ItemStack.EMPTY);
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
    public ItemStack craft(MixingInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        return this.output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return this.output;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.MIXING_GENERIC;
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
