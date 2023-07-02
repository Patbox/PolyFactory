package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.tool.PressTemplateItem;
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

import java.util.List;

public record PressRecipe(Identifier identifier, Ingredient input, int inputCount, Identifier pressTemplate, List<Output> output, double minimumSpeed) implements Recipe<PressBlockEntity>, PolymerRecipe {
    public static final Codec<PressRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    CodecRecipeSerializer.INGREDIENT_CODEC.fieldOf("input").forGetter(PressRecipe::input),
                    Codec.INT.optionalFieldOf("input_count", 1).forGetter(PressRecipe::inputCount),
                    Identifier.CODEC.optionalFieldOf("press_template", PressTemplateItem.DEFAULT_TYPE).forGetter(PressRecipe::pressTemplate),
                    Output.LIST_CODEC.fieldOf("output").forGetter(PressRecipe::output),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 0d).forGetter(PressRecipe::minimumSpeed)
            ).apply(x, PressRecipe::new)
    );

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, Identifier pressTemplate, double minimumSpeed, Output... outputs) {
        return new PressRecipe(FactoryUtil.id("press/" + string), ingredient, inputCount, pressTemplate, List.of(outputs), minimumSpeed);
    }

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, Identifier pressTemplate,double minimumSpeed, ItemStack output) {
        return new PressRecipe(FactoryUtil.id("press/" + string), ingredient, inputCount, pressTemplate, List.of(new Output(output, 1)),  minimumSpeed);
    }

    public static PressRecipe of(String string, Ingredient ingredient, int inputCount, Identifier pressTemplate, double minimumSpeed, ItemConvertible output) {
        return new PressRecipe(FactoryUtil.id("press/" + string), ingredient, inputCount, pressTemplate, List.of(new Output(output.asItem().getDefaultStack(), 1)), minimumSpeed);
    }

    @Override
    public boolean matches(PressBlockEntity inventory, World world) {
        var input = inventory.getStack(PressBlockEntity.INPUT_SLOT);
        var template = inventory.getStack(PressBlockEntity.TEMPLATE_SLOT);
        return PressTemplateItem.getType(template).equals(this.pressTemplate) && this.input.test(input) && input.getCount() >= this.inputCount;
    }

    @Override
    public ItemStack craft(PressBlockEntity inventory, DynamicRegistryManager registryManager) {
        for (var out : output) {
            if (Math.random() <= out.chance) {
                return out.stack.copy();
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


    public record Output(ItemStack stack, float chance) {
        public static final Codec<Output> CODEC_SELF = RecordCodecBuilder.create(x -> x.group(
                        ItemStack.CODEC.fieldOf("item").forGetter(Output::stack),
                        Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(Output::chance)
                ).apply(x, Output::new)
        );

        public static final Codec<Output> CODEC = Codec.either(CODEC_SELF, ItemStack.CODEC).xmap(x -> x.map(y -> y, y -> new Output(y, 1f)), x -> Either.left(x));
        public static final Codec<List<Output>> LIST_CODEC = Codec.either(CODEC, Codec.list(CODEC)).xmap(x -> x.map(y -> List.of(y), y -> y), x -> Either.right(x));

        public static Output of(ItemConvertible stick, float chance) {
            return new Output(stick.asItem().getDefaultStack(), chance);
        }
    }
}
