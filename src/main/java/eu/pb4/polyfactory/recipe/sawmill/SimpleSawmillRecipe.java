package eu.pb4.polyfactory.recipe.sawmill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.SawmillBlock;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SawmillInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record SimpleSawmillRecipe(String group, Ingredient input, Optional<SawmillBlock.BladeAngle> angle, List<OutputStack> output, double time, double minimumSpeed, double optimalSpeed) implements SawmillRecipe {
    public static final MapCodec<SimpleSawmillRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(SimpleSawmillRecipe::group),
                    Ingredient.CODEC.fieldOf("input").forGetter(SimpleSawmillRecipe::input),
                    SawmillBlock.BladeAngle.CODEC.optionalFieldOf("angle").forGetter(SimpleSawmillRecipe::angle),
                    OutputStack.LIST_CODEC.fieldOf("output").forGetter(SimpleSawmillRecipe::output),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleSawmillRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(SimpleSawmillRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 0d).forGetter(SimpleSawmillRecipe::optimalSpeed)
            ).apply(x, SimpleSawmillRecipe::new)
    );

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( "", ingredient, Optional.empty(), List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, String group, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( group, ingredient, Optional.empty(), List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, Ingredient ingredient, double grindTime, double minimumSpeed, double optimalSpeed, ItemStackTemplate... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( "", ingredient, Optional.empty(), Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, Ingredient ingredient, SawmillBlock.BladeAngle angle, double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( "", ingredient, Optional.of(angle), List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, String group, Ingredient ingredient, SawmillBlock.BladeAngle angle,double grindTime, double minimumSpeed, double optimalSpeed, OutputStack... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( group, ingredient, Optional.of(angle), List.of(outputs), grindTime, minimumSpeed, optimalSpeed));
    }

    public static RecipeHolder<SimpleSawmillRecipe> of(String string, Ingredient ingredient, SawmillBlock.BladeAngle angle,double grindTime, double minimumSpeed, double optimalSpeed, ItemStackTemplate... outputs) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("sawmill/" + string), new SimpleSawmillRecipe( "", ingredient, Optional.of(angle), Arrays.stream(outputs).map(x -> new OutputStack(x, 1, 1)).toList(), grindTime, minimumSpeed, optimalSpeed));
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public boolean matches(SawmillInput inventory, Level world) {
        return (this.angle.isEmpty() || this.angle.orElseThrow().equals(inventory.angle())) && this.input.test(inventory.getItem(GrinderBlockEntity.INPUT_SLOT));
    }

    @Deprecated
    @Override
    public ItemStack assemble(SawmillInput inventory) {
        return this.output.isEmpty() ? ItemStack.EMPTY : this.output.getFirst().stack().create();
    }

    @Override
    public RecipeSerializer<SimpleSawmillRecipe> getSerializer() {
        return FactoryRecipeSerializers.SAWMILL_SIMPLE;
    }

    @Override
    public List<ItemStack> output(SawmillInput input, @Nullable RandomSource random) {
        var items = new ArrayList<ItemStack>();

        for (var out : this.output) {
            for (int a = 0; a < out.roll(); a++) {
                if (random == null || random.nextFloat() < out.chance()) {
                    items.add(out.stack().create());
                }
            }
        }

        return items;
    }

    @Override
    public double time(SawmillInput input) {
        return this.time;
    }

    @Override
    public double minimumSpeed(SawmillInput input) {
        return this.minimumSpeed;
    }

    @Override
    public double optimalSpeed(SawmillInput input) {
        return this.optimalSpeed;
    }
}
