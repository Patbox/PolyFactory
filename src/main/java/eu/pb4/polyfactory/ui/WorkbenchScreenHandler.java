package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.block.other.WorkbenchBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WorkbenchScreenHandler extends RecipeBookMenu {
    private final CraftingContainer recipeInput;
    private final ResultContainer result;
    private final ContainerLevelAccess context;
    private final Player player;
    private final Block block;
    private boolean filling;

    public WorkbenchScreenHandler(int syncId, Block block, Inventory playerInventory, WorkbenchBlockEntity input, ResultContainer result, ContainerLevelAccess context) {
        super(MenuType.CRAFTING, syncId);
        this.result = result;
        this.block = block;
        this.context = context;
        this.player = playerInventory.player;
        this.recipeInput = input;
        this.addSlot(new ResultSlot(playerInventory.player, this.recipeInput, this.result, 0, 124, 35) {
            @Override
            protected void checkTakeAchievements(ItemStack stack) {
                super.checkTakeAchievements(stack);
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, input.currentRecipe().id(), input.getItems());
                }
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                var index = j + i * 3;
                this.addSlot(new Slot(this.recipeInput, index, 30 + j * 18, 17 + i * 18) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        input.markSlotDirty(index);
                    }
                });
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    protected static void updateResult(AbstractContainerMenu handler, ServerLevel world, Player player, CraftingContainer craftingInventory, ResultContainer resultInventory, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        CraftingInput craftingRecipeInput = craftingInventory.asCraftInput();
        ServerPlayer serverPlayerEntity = (ServerPlayer) player;
        ItemStack itemStack = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingRecipeInput, world, recipe);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeEntry = optional.get();
            CraftingRecipe craftingRecipe = recipeEntry.value();
            if (resultInventory.setRecipeUsed(serverPlayerEntity, recipeEntry)) {
                ItemStack itemStack2 = craftingRecipe.assemble(craftingRecipeInput, world.registryAccess());
                if (itemStack2.isItemEnabled(world.enabledFeatures())) {
                    itemStack = itemStack2;
                }
            }
        }

        resultInventory.setItem(0, itemStack);
        handler.setRemoteSlot(0, itemStack);
        serverPlayerEntity.connection.send(new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(), 0, itemStack));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        //this.context.run((world, pos) -> this.dropInventory(player, this.input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.context, player, this.block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2 != null && slot2.hasItem()) {
            ItemStack itemStack2 = slot2.getItem();
            itemStack = itemStack2.copy();
            if (slot == 0) {
                this.context.execute((world, pos) -> itemStack2.getItem().onCraftedBy(itemStack2, player));
                if (!this.moveItemStackTo(itemStack2, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot2.onQuickCraft(itemStack2, itemStack);
            } else if (slot >= 10 && slot < 46) {
                if (!this.moveItemStackTo(itemStack2, 1, 10, false)) {
                    if (slot < 37) {
                        if (!this.moveItemStackTo(itemStack2, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemStack2, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemStack2, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setByPlayer(ItemStack.EMPTY);
            } else {
                slot2.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot2.onTake(player, itemStack2);
            if (slot == 0) {
                player.drop(itemStack2, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public PostPlaceAction handlePlacement(boolean craftAll, boolean creative, RecipeHolder<?> recipe, ServerLevel world, Inventory inventory) {
        //noinspection unchecked
        RecipeHolder<CraftingRecipe> recipeEntry = (RecipeHolder<CraftingRecipe>) recipe;
        this.onInputSlotFillStart();

        RecipeBookMenu.PostPlaceAction var8;
        try {
            List<Slot> list = this.slots.subList(1, 10);
            var8 = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
                public void fillCraftSlotsStackedContents(StackedItemContents finder) {
                    WorkbenchScreenHandler.this.fillCraftSlotsStackedContents(finder);
                }

                public void clearCraftingContent() {

                }

                public boolean recipeMatches(RecipeHolder<CraftingRecipe> entry) {
                    return entry.value().matches(CraftingInput.of(3, 3, recipeInput.getItems()),
                            player.level());
                }
            }, 3, 3, list, list, inventory, recipeEntry, craftAll, creative);
        } finally {
            this.onInputSlotFillFinish(world, recipeEntry);
        }

        return var8;
    }

    public void onInputSlotFillStart() {
        this.filling = true;
    }

    public void onInputSlotFillFinish(ServerLevel world, RecipeHolder<CraftingRecipe> recipe) {
        this.filling = false;
        updateResult(this, world, this.player, this.recipeInput, this.result, recipe);
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (!this.filling) {
            this.context.execute((world, pos) -> {
                if (world instanceof ServerLevel serverWorld) {
                    updateResult(this, serverWorld, this.player, this.recipeInput, this.result, null);
                }
            });
        }

    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents finder) {
        this.recipeInput.fillStackedContents(finder);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }
}
