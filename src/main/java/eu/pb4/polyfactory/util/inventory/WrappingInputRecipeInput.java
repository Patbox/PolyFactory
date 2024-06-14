package eu.pb4.polyfactory.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

public record WrappingInputRecipeInput(Inventory source, int start, int size, int width, int height) implements RecipeInputInventory {
    public static RecipeInputInventory of(Inventory source, int start, int size, int width, int height) {
        return new WrappingInputRecipeInput(source, start, size, width, height);
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public List<ItemStack> getHeldStacks() {
        var stacks = new ArrayList<ItemStack>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(this.getStack(i));
        }

        return stacks;
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (int i = 0; i < size; i++) {
            finder.addInput(this.getStack(i));
        }
    }

    @Override
    public boolean isEmpty() {
        for (var i = 0; i < size; i++) {
            if (!this.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot < size ? this.source.getStack(slot + start) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return slot < size ? this.source.removeStack(slot + start, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return slot < size ? this.source.removeStack(slot + start) : ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(slot < size) {
            this.source.setStack(slot + start, stack);
        };
    }

    @Override
    public void markDirty() {
        this.source.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.source.canPlayerUse(player);
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.size; i++) {
            this.setStack(i, ItemStack.EMPTY);
        }
    }
}
