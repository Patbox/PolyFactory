package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.grinding.StrippingGrindingRecipe;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class StrippingGrindingRecipePage extends GrindingRecipePage<StrippingGrindingRecipe> {
    private final PolydexStack<?>[] outputExtra;
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?>[] output;
    private final ItemStack[] inputDisplay;
    private final IdentityHashMap<Item, Item> input2output = new IdentityHashMap<>();
    private final IdentityHashMap<Item, Item> output2input = new IdentityHashMap<>();

    public StrippingGrindingRecipePage(RecipeEntry<StrippingGrindingRecipe> recipe) {
        super(recipe);
        var inputs = new ArrayList<Item>();
        var outputs = new ArrayList<PolydexStack<?>>();

        if (recipe.value().input().isPresent()) {
            recipe.value().input().get().getMatchingItems().forEach(ref -> {
                var stripped = ref.value() instanceof BlockItem blockItem ? StrippableBlockRegistry.getStrippedBlockState(blockItem.getBlock().getDefaultState()) : null;
                if (stripped != null) {
                    input2output.put(ref.value(), stripped.getBlock().asItem());
                    output2input.put(stripped.getBlock().asItem(), ref.value());
                    inputs.add(ref.value());
                    outputs.add(PolydexStack.of(stripped.getBlock().asItem()));
                }
            });
        }

        this.inputDisplay = inputs.stream().map(Item::getDefaultStack).toArray(ItemStack[]::new);
        this.output = outputs.toArray(PolydexStack[]::new);
        this.outputExtra = PolydexCompatImpl.createOutput(this.recipe.output());
        this.ingredients = List.of(PolydexIngredient.of(Ingredient.ofItems(inputs.toArray(ItemConvertible[]::new))));
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        if (polydexEntry != null && polydexEntry.stack().getBacking() instanceof ItemStack stack) {
            var x = input2output.get(stack.getItem());
            if (x != null) {
                return x.getDefaultStack();
            }
            x = output2input.get(stack.getItem());
            if (x != null) {
                return stack;
            }
        }

        return this.output[0].getBacking() instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        for (var i : this.output) {
            if (entry.isPartOf(i)) {
                return true;
            }
        }

        for (var i : this.outputExtra) {
            if (entry.isPartOf(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        if (entry != null && entry.stack().getBacking() instanceof ItemStack stack) {
            var x = input2output.get(stack.getItem());
            if (x != null) {
                return x.getDefaultStack();
            }
            x = output2input.get(stack.getItem());
            if (x != null) {
                return stack;
            }
        }

        return this.output[0].getBacking() instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        if (entry != null && entry.stack().getBacking() instanceof ItemStack stack) {
            var x = input2output.get(stack.getItem());
            var y = output2input.get(stack.getItem());

            if (x != null) {
                layer.setIngredient(4, 1, stack);
                layer.setOutput(3, 3, x.getDefaultStack());
            } else if (y != null) {
                layer.setIngredient(4, 1, y.getDefaultStack());
                layer.setOutput(3, 3, stack);
            } else {
                layer.setIngredient(4, 1, this.inputDisplay);
                layer.setOutput(3, 3, this.output);
            }
        } else {
            layer.setIngredient(4, 1, this.inputDisplay);
            layer.setOutput(3, 3, this.output);
        }


        var i = 1;
        for (; i < this.outputExtra.length + 1; i++) {
            layer.setOutput(3 + i, 3, this.outputExtra[i - 1]);
        }
        for (; i < 3; i++) {
            layer.setEmpty(3 + i, 3);
        }
    }
}
