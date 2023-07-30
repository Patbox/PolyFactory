package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
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
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public record GrindingRecipe(Identifier identifier, Ingredient input, List<OutputStack> output, double grindTime, double minimumSpeed, double optimalSpeed) implements Recipe<GrinderBlockEntity>, PolymerRecipe {
    public static final Codec<GrindingRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    CodecRecipeSerializer.INGREDIENT_CODEC.fieldOf("input").forGetter(GrindingRecipe::input),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(GrindingRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(GrindingRecipe::grindTime),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(GrindingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(GrindingRecipe::optimalSpeed)
            ).apply(x, GrindingRecipe::new)
    );

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, List.of(outputs), grindTime, minimumSpeed, optimalSpeed);
    }

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, OutputStack... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, List.of(outputs), grindTime, 0, optimalSpeed);
    }

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemStack... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, 0, optimalSpeed);
    }

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemStack... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed);
    }

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemConvertible... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed);
    }

    public static GrindingRecipe of(String string, Ingredient ingredient, double grindTime, double optimalSpeed, ItemConvertible... outputs) {
        return new GrindingRecipe(FactoryUtil.id("grinding/" + string), ingredient, Arrays.stream(outputs).map(x -> new OutputStack(x.asItem().getDefaultStack(), 1, 1)).toList(), grindTime, 0, optimalSpeed);
    }

    @Override
    public boolean matches(GrinderBlockEntity inventory, World world) {
        return this.input.test(inventory.getStack(GrinderBlockEntity.INPUT_SLOT));
    }

    @Deprecated
    @Override
    public ItemStack craft(GrinderBlockEntity inventory, DynamicRegistryManager registryManager) {
        return  ItemStack.EMPTY;
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
        return FactoryRecipeSerializers.GRINDING;
    }

    @Override
    public RecipeType<?> getType() {
        return FactoryRecipeTypes.GRINDING;
    }


}
