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
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public record ColoringCraftingRecipe(String group, Item input, Ingredient dye, int maxCount) implements CraftingRecipe {
    public static final MapCodec<ColoringCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ColoringCraftingRecipe::group),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(ColoringCraftingRecipe::input),
                    Ingredient.CODEC.fieldOf("dyes").forGetter(ColoringCraftingRecipe::dye),
                    Codec.INT.optionalFieldOf("max_count", 8).forGetter(ColoringCraftingRecipe::maxCount)
            ).apply(x, ColoringCraftingRecipe::new)
    );


    public static RecipeHolder<ColoringCraftingRecipe> of(HolderLookup.RegistryLookup<Item> itemWrap, String id, Item item) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("crafting/" + id), new ColoringCraftingRecipe("polyfactory:crafting/" + id, item, defaultDyes(itemWrap), 8));
    }

    public static RecipeHolder<ColoringCraftingRecipe> of(HolderLookup.RegistryLookup<Item> itemWrap, String id, Item item, int count) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("crafting/" + id), new ColoringCraftingRecipe("polyfactory:crafting/" + id, item, defaultDyes(itemWrap), count));
    }

    private static Ingredient defaultDyes(HolderGetter<Item> itemWrap) {
        return Ingredient.of(itemWrap.getOrThrow(ConventionalItemTags.DYES));
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        boolean hasDye = false;
        int count = 0;
        for (var stack : inventory.items()) {
            if (stack.is(ConventionalItemTags.DYES)) {
                if (hasDye) {
                    return false;
                }

                hasDye = true;
            } else if (stack.is(this.input)) {
                count++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return hasDye && count > 0 && count <= this.maxCount;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryManager) {
        int color = -1;
        int count = 0;
        ItemStack og = ItemStack.EMPTY;

        for (var stack : inventory.items()) {
            if (stack.is(ConventionalItemTags.DYES)) {
                color = DyeColorExtra.getColor(stack);
            } else if (stack.is(this.input)) {
                count++;
                og = stack;
            }
        }

        var out = ColoredItem.stackCrafting(this.input, count, color);
        if (this.maxCount == 1) {
            var c = out.get(FactoryDataComponents.COLOR);
            out.applyComponentsAndValidate(og.getComponentsPatch());
            out.set(FactoryDataComponents.COLOR, c);
        }
        return out;
    }

    @Override
    public RecipeSerializer<ColoringCraftingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_COLORING;
    }

    @Override
    public List<RecipeDisplay> display() {
        var list = new ArrayList<RecipeDisplay>();

        var anyColorBase = new ArrayList<SlotDisplay>();
        for (var dye : this.dye.items().toList()) {
            anyColorBase.add(new SlotDisplay.ItemStackSlotDisplay(ColoredItem.stackCrafting(this.input, 1, DyeColorExtra.getColor(dye.value().getDefaultInstance()))));
        }

        for (int count : this.maxCount != 1 ? new int[] {1, this.maxCount} : new int[] { 1 } ) {
            for (var dye : this.dye.items().toList()) {
                var t = new ArrayList<SlotDisplay>();
                t.add(new SlotDisplay.ItemSlotDisplay(dye));
                for (int i = 0; i < count; i++) {
                    t.add(new SlotDisplay.Composite(anyColorBase));
                }
                list.add(new ShapelessCraftingRecipeDisplay(t,
                        new SlotDisplay.ItemStackSlotDisplay(ColoredItem.stackCrafting(this.input, count, DyeColorExtra.getColor(dye.value().getDefaultInstance()))),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
            }
        }


        return list;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of(Ingredient.of(this.input), this.dye));
    }
}
