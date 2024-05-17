package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public record GenericMixingRecipe(String group, List<CountedIngredient> input, ItemStack output, double time,
                                  double minimumSpeed,
                                  double optimalSpeed, float minimumTemperature, float maxTemperature) implements MixingRecipe {
    public static final MapCodec<GenericMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(GenericMixingRecipe::group),
                    CountedIngredient.LIST_CODEC.fieldOf("input").forGetter(GenericMixingRecipe::input),
                    ItemStack.CODEC.fieldOf("output").forGetter(GenericMixingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(GenericMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(GenericMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(GenericMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", -1f).forGetter(GenericMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", 2f).forGetter(GenericMixingRecipe::maxTemperature)
            ).apply(x, GenericMixingRecipe::new)
    );

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", ingredient, output, mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, float minTemperature, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", ingredient, output, mixingTime, minimumSpeed, optimalSpeed, minTemperature, 2f));
    }

    public static RecipeEntry<GenericMixingRecipe> ofCounted(String string, String group, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe(group, ingredient, output, mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
    }

    public Iterable<ItemStack> remainders() {
        return () -> Iterators.transform(this.input.iterator(), (a) -> a.leftOver().copy());
    }

    public static RecipeEntry<GenericMixingRecipe> of(String string, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(x, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + string), new GenericMixingRecipe("", list, output, mixingTime, minimumSpeed, optimalSpeed, -1f, 2f));
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
    public boolean matches(MixerBlockEntity inventory, World world) {
        var map = new Object2IntArrayMap<CountedIngredient>();
        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);
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

        return true;
    }

    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        var list = new ArrayList<>(this.input);
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

    @Override
    public ItemStack craft(MixerBlockEntity inventory, RegistryWrapper.WrapperLookup registryManager) {
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
    public RecipeType<?> getType() {
        return FactoryRecipeTypes.MIXER;
    }


}
