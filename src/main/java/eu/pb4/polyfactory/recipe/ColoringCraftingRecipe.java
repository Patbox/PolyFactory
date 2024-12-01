package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.RegistryGetterCodec;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public record ColoringCraftingRecipe(String group, Item input, Ingredient dye, int maxCount) implements CraftingRecipe {
    public static final MapCodec<ColoringCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ColoringCraftingRecipe::group),
                    Registries.ITEM.getCodec().fieldOf("input").forGetter(ColoringCraftingRecipe::input),
                    Ingredient.CODEC.fieldOf("dyes").forGetter(ColoringCraftingRecipe::dye),
                    Codec.INT.optionalFieldOf("max_count", 8).forGetter(ColoringCraftingRecipe::maxCount)
            ).apply(x, ColoringCraftingRecipe::new)
    );


    public static RecipeEntry<ColoringCraftingRecipe> of(RegistryWrapper.Impl<Item> itemWrap, String id, Item item) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("crafting/" + id), new ColoringCraftingRecipe("polyfactory:crafting/" + id, item, defaultDyes(itemWrap), 8));
    }

    public static RecipeEntry<ColoringCraftingRecipe> of(RegistryWrapper.Impl<Item> itemWrap, String id, Item item, int count) {
        return new RecipeEntry<>(FactoryUtil.recipeKey("crafting/" + id), new ColoringCraftingRecipe("polyfactory:crafting/" + id, item, defaultDyes(itemWrap), count));
    }

    private static Ingredient defaultDyes(RegistryEntryLookup<Item> itemWrap) {
        return Ingredient.fromTag(itemWrap.getOrThrow(ConventionalItemTags.DYES));
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return CraftingRecipeCategory.MISC;
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        boolean hasDye = false;
        int count = 0;
        for (var stack : inventory.getStacks()) {
            if (stack.isIn(ConventionalItemTags.DYES)) {
                if (hasDye) {
                    return false;
                }

                hasDye = true;
            } else if (stack.isOf(this.input)) {
                count++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return hasDye && count > 0 && count <= this.maxCount;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        int color = -1;
        int count = 0;
        ItemStack og = ItemStack.EMPTY;

        for (var stack : inventory.getStacks()) {
            if (stack.isIn(ConventionalItemTags.DYES)) {
                color = DyeColorExtra.getColor(stack);
            } else if (stack.isOf(this.input)) {
                count++;
                og = stack;
            }
        }

        var out = ColoredItem.stackCrafting(this.input, count, color);
        if (this.maxCount == 1) {
            var c = out.get(FactoryDataComponents.COLOR);
            out.applyChanges(og.getComponentChanges());
            out.set(FactoryDataComponents.COLOR, c);
        }
        return out;
    }

    @Override
    public RecipeSerializer<ColoringCraftingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_COLORING;
    }

    @Override
    public List<RecipeDisplay> getDisplays() {
        var list = new ArrayList<RecipeDisplay>();

        var anyColorBase = new ArrayList<SlotDisplay>();
        for (var dye : this.dye.getMatchingItems().toList()) {
            anyColorBase.add(new SlotDisplay.StackSlotDisplay(ColoredItem.stackCrafting(this.input, 1, DyeColorExtra.getColor(dye.value().getDefaultStack()))));
        }

        for (int count : this.maxCount != 1 ? new int[] {1, this.maxCount} : new int[] { 1 } ) {
            for (var dye : this.dye.getMatchingItems().toList()) {
                var t = new ArrayList<SlotDisplay>();
                t.add(new SlotDisplay.ItemSlotDisplay(dye));
                for (int i = 0; i < count; i++) {
                    t.add(new SlotDisplay.CompositeSlotDisplay(anyColorBase));
                }
                list.add(new ShapelessCraftingRecipeDisplay(t,
                        new SlotDisplay.StackSlotDisplay(ColoredItem.stackCrafting(this.input, count, DyeColorExtra.getColor(dye.value().getDefaultStack()))),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
            }
        }


        return list;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forShapeless(List.of(Ingredient.ofItem(this.input), this.dye));
    }
}
