package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public record CraftingWithLeftoverRecipe<T extends CraftingRecipe>(RecipeSerializer<CraftingWithLeftoverRecipe<T>> serializer, T backingRecipe, List<Ingredient> leftovers) implements CraftingRecipe {
    public static <T extends CraftingRecipe> MapCodec<CraftingWithLeftoverRecipe<T>> createCodec(MapCodec<T> recipeCodec, Supplier<RecipeSerializer<CraftingWithLeftoverRecipe<T>>> serializer) {
        return MapCodec.recursive("CraftingWithLeftovers", (c) -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                MapCodec.unit(serializer).forGetter(CraftingWithLeftoverRecipe::serializer),
                recipeCodec.forGetter(CraftingWithLeftoverRecipe::backingRecipe),
                Ingredient.CODEC.listOf().fieldOf("leftovers").forGetter(CraftingWithLeftoverRecipe::leftovers)
        ).apply(instance, CraftingWithLeftoverRecipe::new)));
    }
    public static CraftingWithLeftoverRecipe<ShapedRecipe> of(ShapedRecipe recipe, Ingredient... leftovers) {
        return new CraftingWithLeftoverRecipe<>(FactoryRecipeSerializers.CRAFTING_SHAPED_LEFTOVER, recipe, List.of(leftovers));
    }

    public static CraftingWithLeftoverRecipe<ShapelessRecipe> of(ShapelessRecipe recipe, Ingredient... leftovers) {
        return new CraftingWithLeftoverRecipe<>(FactoryRecipeSerializers.CRAFTING_SHAPELESS_LEFTOVER, recipe, List.of(leftovers));
    }

    public static RecipeOutput asExporter(RecipeOutput exporter, Ingredient... leftover) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
                if (recipe instanceof ShapedRecipe shapedRecipe) {
                    exporter.accept(key, of(shapedRecipe, leftover), advancement);
                } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                    exporter.accept(key, of(shapelessRecipe, leftover), advancement);
                }
            }

            @Override
            public Advancement.Builder advancement() {
                return exporter.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                exporter.includeRootAdvancement();
            }
        };
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        return this.backingRecipe.matches(input, world);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.backingRecipe.assemble(input, registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remainders = this.backingRecipe.getRemainingItems(input  );
        for (var i = 0; i < input.size(); i++) {
            for (var ing : this.leftovers) {
                if (ing.test(input.getItem(i))) {
                    remainders.set(i, input.getItem(i).copyWithCount(1));
                }
            }
        }

        return remainders;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return serializer;
    }

    @Override
    public PlacementInfo placementInfo() {
        return this.backingRecipe.placementInfo();
    }

    @Override
    public CraftingBookCategory category() {
        return this.backingRecipe.category();
    }

    @Override
    public String group() {
        return this.backingRecipe.group();
    }

    @Override
    public List<RecipeDisplay> display() {
        return this.backingRecipe.display();
    }
}
