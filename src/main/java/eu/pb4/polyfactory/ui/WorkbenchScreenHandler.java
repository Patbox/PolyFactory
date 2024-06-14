package eu.pb4.polyfactory.ui;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;

public class WorkbenchScreenHandler extends AbstractRecipeScreenHandler<CraftingRecipeInput, Recipe<CraftingRecipeInput>> {
	private final RecipeInputInventory recipeInput;
	private final CraftingResultInventory result;
	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private final Block block;

	public WorkbenchScreenHandler(int syncId, Block block, PlayerInventory playerInventory, RecipeInputInventory input, CraftingResultInventory result, ScreenHandlerContext context) {
		super(ScreenHandlerType.CRAFTING, syncId);
		this.result = result;
		this.block = block;
		this.context = context;
		this.player = playerInventory.player;
		this.recipeInput = input;
		this.addSlot(new CraftingResultSlot(playerInventory.player, this.recipeInput, this.result, 0, 124, 35));

		for(int i = 0; i < 3; ++i) {
			for(int j = 0; j < 3; ++j) {
				this.addSlot(new Slot(this.recipeInput, j + i * 3, 30 + j * 18, 17 + i * 18));
			}
		}

		for(int i = 0; i < 3; ++i) {
			for(int j = 0; j < 9; ++j) {
				this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for(int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
		}
	}

	@Override
	public void populateRecipeFinder(RecipeMatcher finder) {
		this.recipeInput.provideRecipeInputs(finder);
	}

	@Override
	public void clearCraftingSlots() {

	}

	@Override
	public boolean matches(RecipeEntry recipe) {
		return recipe.value().matches(CraftingRecipeInput.create(3, 3, this.recipeInput.getHeldStacks()), this.player.getWorld());
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		//this.context.run((world, pos) -> this.dropInventory(player, this.input));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, this.block);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot2 = this.slots.get(slot);
		if (slot2 != null && slot2.hasStack()) {
			ItemStack itemStack2 = slot2.getStack();
			itemStack = itemStack2.copy();
			if (slot == 0) {
				this.context.run((world, pos) -> itemStack2.getItem().onCraftByPlayer(itemStack2, world, player));
				if (!this.insertItem(itemStack2, 10, 46, true)) {
					return ItemStack.EMPTY;
				}

				slot2.onQuickTransfer(itemStack2, itemStack);
			} else if (slot >= 10 && slot < 46) {
				if (!this.insertItem(itemStack2, 1, 10, false)) {
					if (slot < 37) {
						if (!this.insertItem(itemStack2, 37, 46, false)) {
							return ItemStack.EMPTY;
						}
					} else if (!this.insertItem(itemStack2, 10, 37, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (!this.insertItem(itemStack2, 10, 46, false)) {
				return ItemStack.EMPTY;
			}

			if (itemStack2.isEmpty()) {
				slot2.setStack(ItemStack.EMPTY);
			} else {
				slot2.markDirty();
			}

			if (itemStack2.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot2.onTakeItem(player, itemStack2);
			if (slot == 0) {
				player.dropItem(itemStack2, false);
			}
		}

		return itemStack;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != this.result && super.canInsertIntoSlot(stack, slot);
	}

	@Override
	public int getCraftingResultSlotIndex() {
		return 0;
	}

	@Override
	public int getCraftingWidth() {
		return this.recipeInput.getWidth();
	}

	@Override
	public int getCraftingHeight() {
		return this.recipeInput.getHeight();
	}

	@Override
	public int getCraftingSlotCount() {
		return 10;
	}

	@Override
	public RecipeBookCategory getCategory() {
		return RecipeBookCategory.CRAFTING;
	}

	@Override
	public boolean canInsertIntoSlot(int index) {
		return index != this.getCraftingResultSlotIndex();
	}
}
