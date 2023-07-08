package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public record GenericMixingRecipe(Identifier identifier, List<CountedIngredient> input, ItemStack output, double time,
                                  double minimumSpeed,
                                  double optimalSpeed) implements MixingRecipe {
    public static final Codec<GenericMixingRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    CountedIngredient.LIST_CODEC.fieldOf("input").forGetter(GenericMixingRecipe::input),
                    ItemStack.CODEC.fieldOf("output").forGetter(GenericMixingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(GenericMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(GenericMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(GenericMixingRecipe::optimalSpeed)
            ).apply(x, GenericMixingRecipe::new)
    );

    public static GenericMixingRecipe ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new GenericMixingRecipe(FactoryUtil.id("mixing/" + string), ingredient, output, mixingTime, minimumSpeed, optimalSpeed);
    }

    public Iterable<ItemStack> remainders() {
        return () -> Iterators.transform(this.input.iterator(), (a) -> a.leftOver().copy());
    }

    public static GenericMixingRecipe of(String string, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(x, 1, CountedIngredient.tryGettingLeftover(x));
            list.add(countedIngredient);
        }
        return new GenericMixingRecipe(FactoryUtil.id("mixing/" + string), list, output, mixingTime, minimumSpeed, optimalSpeed);
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
    public ItemStack craft(MixerBlockEntity inventory, DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.output;
    }

    @Override
    public Identifier getId() {
        return this.identifier;
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
