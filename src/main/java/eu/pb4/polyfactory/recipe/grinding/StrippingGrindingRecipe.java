package eu.pb4.polyfactory.recipe.grinding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.input.GrindingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StrippingGrindingRecipe(String group, Optional<Ingredient> input, List<OutputStack> output, double grindTime, double minimumSpeed, double optimalSpeed) implements GrindingRecipe {
    public static final MapCodec<StrippingGrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(StrippingGrindingRecipe::group),
                    Ingredient.CODEC.optionalFieldOf("input").forGetter(StrippingGrindingRecipe::input),
                    OutputStack.LIST_CODEC.optionalFieldOf("output", List.of()).forGetter(StrippingGrindingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(StrippingGrindingRecipe::grindTime),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(StrippingGrindingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(StrippingGrindingRecipe::optimalSpeed)
            ).apply(x, StrippingGrindingRecipe::new)
    );

    public static RecipeEntry< StrippingGrindingRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("grinding/" + string), new StrippingGrindingRecipe( "", Optional.ofNullable(ingredient), List.of(outputs),  grindTime, minimumSpeed, optimalSpeed));
    }
    
    @Override
    public List<ItemStack> output(GrindingInput input, RegistryWrapper.WrapperLookup registryManager, @Nullable Random random) {
        var items = new ArrayList<ItemStack>();

        var stripped = input.stack().getItem() instanceof BlockItem blockItem ? StrippableBlockRegistry.getStrippedBlockState(blockItem.getBlock().getDefaultState()) : null;
        if (stripped != null)  {
            items.add(stripped.getBlock().asItem().getDefaultStack());
        }

        for (var out : this.output) {
            for (int a = 0; a < out.roll(); a++) {
                if (random == null || random.nextFloat() < out.chance()) {
                    items.add(out.stack().copy());
                }
            }
        }

        return items;



    }

    @Override
    public double grindTime(GrindingInput input) {
        return grindTime;
    }

    @Override
    public double minimumSpeed(GrindingInput input) {
        return minimumSpeed;
    }

    @Override
    public double optimalSpeed(GrindingInput input) {
        return optimalSpeed;
    }

    @Override
    public boolean matches(GrindingInput input, World world) {
        return (this.input.isEmpty() || this.input.get().test(input.stack()))
                && input.stack().getItem() instanceof BlockItem blockItem
                && StrippableBlockRegistry.getStrippedBlockState(blockItem.getBlock().getDefaultState()) != null;
    }

    @Override
    public RecipeSerializer<StrippingGrindingRecipe> getSerializer() {
        return FactoryRecipeSerializers.GRINDING_STRIPPING;
    }
}
