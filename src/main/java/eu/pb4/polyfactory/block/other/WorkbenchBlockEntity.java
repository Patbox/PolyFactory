package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlock;
import eu.pb4.polyfactory.ui.WorkbenchScreenHandler;
import eu.pb4.polyfactory.util.inventory.CrafterLikeInsertContainer;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class WorkbenchBlockEntity extends LockableBlockEntity implements MinimalSidedContainer, CrafterLikeInsertContainer, CraftingContainer, BlockEntityExtraListener {

    private static final int[] INPUT_SLOTS = IntStream.range(0, 9).toArray();
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
    private final ResultContainer result = new ResultContainer();
    @Nullable
    private RecipeHolder<CraftingRecipe> currentRecipe;

    private WorkbenchBlock.Model model;

    public WorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WORKBENCH, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        if (this.level != null) {
            updateResult();
        }
        super.loadAdditional(view);
        if (this.model != null) {
            for (int i = 0; i < 9; i++) {
                this.markSlotDirty(i);
            }
        }
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && (dir == Direction.UP || dir.getOpposite() == this.getBlockState().getValue(MCrafterBlock.FACING))) {
            return getLeastPopulatedInputSlot(stack) == slot;
        }

        return slot < 9;
    }

    @Override
    public int inputSize() {
        return 9;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public void createGui(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return WorkbenchBlockEntity.this.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new WorkbenchScreenHandler(syncId, getBlockState().getBlock(), playerInventory, WorkbenchBlockEntity.this, WorkbenchBlockEntity.this.result, ContainerLevelAccess.create(level, worldPosition));
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
    public List<ItemStack> getItems() {
        return List.copyOf(this.stacks.subList(0, 9));
    }

    @Override
    public void fillStackedContents(StackedItemContents finder) {
        for (int i = 0; i < 9; i++) {
            finder.accountStack(this.getItem(i));
        }
    }

    protected void updateResult() {
        ItemStack itemStack = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.asCraftInput(), level, this.currentRecipe);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeEntry = optional.get();
            CraftingRecipe craftingRecipe = recipeEntry.value();
            //if (result.shouldCraftRecipe(world, null, recipeEntry)) {
            ItemStack itemStack2 = craftingRecipe.assemble(this.asCraftInput(), level.registryAccess());
            if (itemStack2.isItemEnabled(level.enabledFeatures())) {
                itemStack = itemStack2;
            }
            //}
            this.currentRecipe = recipeEntry;
        } else {
            this.currentRecipe = null;
        }

        result.setItem(0, itemStack);
    }

    @Override
    public void markSlotDirty(int index) {
        if (this.model != null && this.stacks.size() > index) {
            this.model.setStack(index, this.getItem(index));
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateResult();
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        updateResult();
        this.model = BlockAwareAttachment.get(chunk, this.worldPosition).holder() instanceof WorkbenchBlock.Model m ? m : null;
        for (int i = 0; i < 9; i++) {
            this.markSlotDirty(i);
        }
    }

    @Nullable
    public RecipeHolder<CraftingRecipe> currentRecipe() {
        return this.currentRecipe;
    }
}
