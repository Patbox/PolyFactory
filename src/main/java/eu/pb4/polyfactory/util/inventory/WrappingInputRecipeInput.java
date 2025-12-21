package eu.pb4.polyfactory.util.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public record WrappingInputRecipeInput(Container source, int start, int size, int width, int height) implements CraftingContainer {
    public static CraftingContainer of(Container source, int start, int size, int width, int height) {
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
    public List<ItemStack> getItems() {
        var stacks = new ArrayList<ItemStack>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(this.getItem(i));
        }

        return stacks;
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        for (var i = 0; i < size; i++) {
            if (!this.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < size ? this.source.getItem(slot + start) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot < size ? this.source.removeItem(slot + start, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot < size ? this.source.removeItemNoUpdate(slot + start) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if(slot < size) {
            this.source.setItem(slot + start, stack);
        };
    }

    @Override
    public void setChanged() {
        this.source.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.source.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.size; i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void fillStackedContents(StackedItemContents finder) {
        for (int i = 0; i < this.size; i++) {
            finder.accountStack(this.getItem(i));
        }
    }
}
