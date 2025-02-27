package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlock;
import eu.pb4.polyfactory.ui.WorkbenchScreenHandler;
import eu.pb4.polyfactory.util.inventory.CustomInsertInventory;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class WorkbenchBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, CustomInsertInventory, RecipeInputInventory, BlockEntityExtraListener {
    private static final int[] INPUT_SLOTS = IntStream.range(0, 9).toArray();
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(9, ItemStack.EMPTY);
    private final CraftingResultInventory result = new CraftingResultInventory();
    @Nullable
    private RecipeEntry<CraftingRecipe> currentRecipe;

    private WorkbenchBlock.Model model;

    public WorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WORKBENCH, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.writeNbt(nbt, stacks, lookup);
        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.readNbt(nbt, this.stacks, lookup);
        if (this.world != null) {
            updateResult();
        }
        super.readNbt(nbt, lookup);
        if (this.model != null) {
            for (int i = 0; i < 9; i++) {
                this.markSlotDirty(i);
            }
        }
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && (dir == Direction.UP || dir.getOpposite() == this.getCachedState().get(MCrafterBlock.FACING))) {
            return getLeastPopulatedInputSlot(stack) == slot;
        }

        return slot < 9;
    }

    private int getLeastPopulatedInputSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        int slot = -1;
        int count = 9999;

        for (int i = 0; i < 9; i++) {
            var cur = this.getStack(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.areItemsAndComponentsEqual(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxCount()) {
                    count = cur.getCount();
                    slot = i;
                }
            }
        }

        return slot;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public void createGui(ServerPlayerEntity player) {
        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return WorkbenchBlockEntity.this.getDisplayName();
            }

            @Nullable
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new WorkbenchScreenHandler(syncId, getCachedState().getBlock(), playerInventory, WorkbenchBlockEntity.this, WorkbenchBlockEntity.this.result, ScreenHandlerContext.create(world, pos));
            }
        });
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public List<ItemStack> getHeldStacks() {
        return List.copyOf(this.stacks.subList(0, 9));
    }

    @Override
    public void provideRecipeInputs(RecipeFinder finder) {
        for (int i = 0; i < 9; i++) {
            finder.addInput(this.getStack(i));
        }
    }

    protected void updateResult() {
        ItemStack itemStack = ItemStack.EMPTY;
        Optional<RecipeEntry<CraftingRecipe>> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, this.createRecipeInput(), world, this.currentRecipe);
        if (optional.isPresent()) {
            RecipeEntry<CraftingRecipe> recipeEntry = optional.get();
            CraftingRecipe craftingRecipe = recipeEntry.value();
            //if (result.shouldCraftRecipe(world, null, recipeEntry)) {
                ItemStack itemStack2 = craftingRecipe.craft(this.createRecipeInput(), world.getRegistryManager());
                if (itemStack2.isItemEnabled(world.getEnabledFeatures())) {
                    itemStack = itemStack2;
                }
            //}
            this.currentRecipe = recipeEntry;
        } else {
            this.currentRecipe = null;
        }

        result.setStack(0, itemStack);
    }

    @Override
    public void markSlotDirty(int index) {
        if (this.model != null) {
            this.model.setStack(index, this.getStack(index));
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        updateResult();
    }

    @Override
    public int insertStack(ItemStack itemStack, Direction direction) {
        var init = itemStack.getCount();
        while (true) {
            if (itemStack.isEmpty()) {
                return init;
            }
            var slot = this.getLeastPopulatedInputSlot(itemStack);
            if (slot == -1) {
                return init - itemStack.getCount();
            }

            var current = this.getStack(slot);
            if (current.isEmpty()) {
                this.setStack(slot, itemStack.copyWithCount(1));
                itemStack.decrement(1);
            } else {
                current.increment(1);
                itemStack.decrement(1);
            }
        }
    }


    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        updateResult();
        this.model = BlockAwareAttachment.get(chunk, this.pos).holder() instanceof WorkbenchBlock.Model m ? m : null;
        for (int i = 0; i < 9; i++) {
            this.markSlotDirty(i);
        }
    }

    @Nullable
    public RecipeEntry<CraftingRecipe> currentRecipe() {
        return this.currentRecipe;
    }
}
