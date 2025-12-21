package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BlueprintWorkbenchBlockEntity extends LockableBlockEntity implements MinimalSidedContainer, BlockEntityExtraListener {
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
    private final NonNullList<FilterData> filters = NonNullList.withSize(9, FilterData.EMPTY_FALSE);
    private ItemStack outputPreview = ItemStack.EMPTY;
    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> recipe = RecipeManager.createCheck(RecipeType.CRAFTING);

    private BlueprintWorkbenchBlock.Model model;

    public BlueprintWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.BLUEPRINT_WORKBENCH, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        for (int i = 0; i < 9; i++) {
            this.filters.set(i, FilterData.of(this.stacks.get(i), false));
        }
        super.loadAdditional(view);
        if (this.level != null) {
            this.updatePreview();
        }

        if (this.model != null) {
            for (int i = 0; i < 9; i++) {
                this.markSlotDirty(i);
            }
        }
    }

    public void createGui(ServerPlayer player) {
        new Gui(player, this);
    }

    private void updatePreview() {
        assert this.level != null;
        var stacks = new ArrayList<ItemStack>(9);
        for (var filter : this.filters) {
            stacks.add(filter.icon().isEmpty() ? ItemStack.EMPTY : filter.icon().getFirst());
        }
        var input = CraftingInput.of(3, 3, stacks);
        this.outputPreview = this.recipe.getRecipeFor(input, (ServerLevel) this.level).map(x -> x.value().assemble(input, this.level.registryAccess())).orElse(ItemStack.EMPTY);
        if (this.model != null) {
            this.model.setResult(this.outputPreview.copy());
        }
    }

    public void clickForCrafting(ServerPlayer player) {
        var list = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            var filter = this.filters.get(i);
            if (filter.isEmpty()) {
                continue;
            }

            for (var stack : player.getInventory().getNonEquipmentItems()) {
                if (filter.test(stack)) {
                    int count = 1;
                    for (int a = 0; a < i; a++) {
                        if (list.get(a) == stack) {
                            count++;
                        }
                    }
                    if (stack.getCount() >= count) {
                        list.set(i, stack);
                        break;
                    }
                }
            }
        }
        var input = CraftingInput.of(3, 3, list);

        var optional = this.recipe.getRecipeFor(input, player.level());
        if (optional.isEmpty()) {
            return;
        }
        var recipe = optional.get().value();
        var result = recipe.assemble(input, player.level().registryAccess());
        if (result.isEmpty()) {
            return;
        }
        player.handleExtraItemsCreatedOnUse(result);

        var remainders = recipe.getRemainingItems(input);
        for (var stack : list) {
            if (!stack.isEmpty()) {
                stack.shrink(1);
            }
        }
        for (var stack : remainders) {
            player.handleExtraItemsCreatedOnUse(stack);
        }

        FactoryUtil.playSoundToPlayer(player,SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1);
        TriggerCriterion.trigger(player, FactoryTriggers.CRAFT_WITH_BLUEPRINT_WORKBENCH);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    public void markSlotDirty(int index) {
        this.filters.set(index, FilterData.of(this.getItem(index), false));
        if (this.model != null) {
            var icon = this.filters.get(index).icon();
            this.model.setStack(index, icon.isEmpty() ? ItemStack.EMPTY : icon.getFirst());
        }
    }

    @Override
    public void setChanged() {
        this.updatePreview();
        super.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockAwareAttachment.get(chunk, this.worldPosition).holder() instanceof BlueprintWorkbenchBlock.Model m ? m : null;
        for (int i = 0; i < 9; i++) {
            this.markSlotDirty(i);
        }
        this.updatePreview();
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public static class Gui extends SimpleGui {
        private final BlueprintWorkbenchBlockEntity be;

        public Gui(ServerPlayer player, BlueprintWorkbenchBlockEntity be) {
            super(MenuType.CRAFTING, player, false);
            this.be = be;
            this.setTitle(Component.empty().append(Component.literal("" + GuiTextures.BLUEPRINT_WORKSTATION_EXTRA_OFFSET).setStyle(UiResourceCreator.STYLE))
                    .append(GuiTextures.BLUEPRINT_WORKBENCH.apply(be.getBlockState().getBlock().getName())));

            this.setSlot(0, () -> be.outputPreview.copy());

            for (int i = 0; i < 9; i++) {
                this.setSlotRedirect(i + 1, new Slot(be, i, 0, 0));
            }
            this.open();
        }

        @Override
        public void onCraftRequest(RecipeDisplayId recipeId, boolean shift) {
            var recipe = this.getPlayer().level().getServer().getRecipeManager().getRecipeFromDisplay(recipeId);
            if (recipe == null || !(recipe.parent().value() instanceof CraftingRecipe craftingRecipe) || craftingRecipe.placementInfo().isImpossibleToPlace()) {
                return;
            }

            //noinspection unchecked
            var post = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<>() {
                public void fillCraftSlotsStackedContents(StackedItemContents finder) {
                    for (int i = 0; i < 9; i++) {
                        finder.accountStack(be.getItem(i));
                    }
                }

                public void clearCraftingContent() {}

                public boolean recipeMatches(RecipeHolder<CraftingRecipe> entry) {
                    return entry.value().matches(CraftingInput.of(3, 3, be.stacks), player.level());
                }
            }, 3, 3, this.screenHandler.slots.subList(1, 10), this.screenHandler.slots.subList(1, 10), player.getInventory(), (RecipeHolder<CraftingRecipe>) recipe.parent(), false, false);

            if (post == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
                this.player.connection.send(new ClientboundPlaceGhostRecipePacket(this.player.containerMenu.containerId, recipe.display().display()));
            }
        }

        @Override
        public void onTick() {
            if (this.be.isRemoved() || player.position().distanceToSqr(Vec3.atCenterOf(this.be.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
