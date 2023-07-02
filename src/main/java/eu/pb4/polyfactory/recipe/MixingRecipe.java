package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public record MixingRecipe(Identifier identifier, List<CountedIngredient> input, ItemStack output, double time, double minimumSpeed, double optimalSpeed) implements Recipe<MixerBlockEntity>, PolymerRecipe {
    public static final Codec<MixingRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    CountedIngredient.LIST_CODEC.fieldOf("input").forGetter(MixingRecipe::input),
                    ItemStack.CODEC.fieldOf("output").forGetter(MixingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(MixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(MixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(MixingRecipe::optimalSpeed)
            ).apply(x, MixingRecipe::new)
    );

    public static MixingRecipe ofCounted(String string, List<CountedIngredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        return new MixingRecipe(FactoryUtil.id("mixing/" + string), ingredient, output, mixingTime, minimumSpeed, optimalSpeed);
    }

    public static MixingRecipe of(String string, List<Ingredient> ingredient, double mixingTime, double minimumSpeed, double optimalSpeed, ItemStack output) {
        List<CountedIngredient> list = new ArrayList<>();
        for (Ingredient x : ingredient) {
            CountedIngredient countedIngredient = new CountedIngredient(x, 1);
            list.add(countedIngredient);
        }
        return new MixingRecipe(FactoryUtil.id("mixing/" + string), list, output, mixingTime, minimumSpeed, optimalSpeed);
    }

    @Override
    public boolean matches(MixerBlockEntity inventory, World world) {
        int count = this.input.size();
        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_SLOT; i++) {
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            var notFound = true;

            for (var entry : this.input) {
                if (entry.test(stack)) {
                    count--;
                    notFound = false;
                    break;
                }
            }

            if (notFound) {
                return false;
            }
        }

        return count == 0;
    }

    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        var list = new ArrayList<>(this.input);

        for (int i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_SLOT; i++) {
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            for (var ig : list) {
                if (ig.test(stack)) {
                    stack.decrement(ig.count());
                    break;
                }
            }
        }
    }

    @Override
    public ItemStack craft(MixerBlockEntity inventory, DynamicRegistryManager registryManager) {
        return this.output;
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
        return FactoryRecipeSerializers.MIXING;
    }

    @Override
    public RecipeType<?> getType() {
        return FactoryRecipeTypes.MIXER;
    }



}
