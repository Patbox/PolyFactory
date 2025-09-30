package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.CraftFailedResponseS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlueprintWorkbenchBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, BlockEntityExtraListener {
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(9, ItemStack.EMPTY);
    private final DefaultedList<FilterData> filters = DefaultedList.ofSize(9, FilterData.EMPTY_FALSE);
    private ItemStack outputPreview = ItemStack.EMPTY;
    private final ServerRecipeManager.MatchGetter<CraftingRecipeInput, CraftingRecipe> recipe = ServerRecipeManager.createCachedMatchGetter(RecipeType.CRAFTING);

    private BlueprintWorkbenchBlock.Model model;

    public BlueprintWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.BLUEPRINT_WORKBENCH, pos, state);
    }

    @Override
    protected void writeData(WriteView view) {
        Inventories.writeData(view, this.stacks);
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        Inventories.readData(view, this.stacks);
        for (int i = 0; i < 9; i++) {
            this.filters.set(i, FilterData.of(this.stacks.get(i), false));
        }
        super.readData(view);
        if (this.world != null) {
            this.updatePreview();
        }

        if (this.model != null) {
            for (int i = 0; i < 9; i++) {
                this.markSlotDirty(i);
            }
        }
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player, this);
    }

    private void updatePreview() {
        assert this.world != null;
        var stacks = new ArrayList<ItemStack>(9);
        for (var filter : this.filters) {
            stacks.add(filter.icon().isEmpty() ? ItemStack.EMPTY : filter.icon().getFirst());
        }
        var input = CraftingRecipeInput.create(3, 3, stacks);
        this.outputPreview = this.recipe.getFirstMatch(input, (ServerWorld) this.world).map(x -> x.value().craft(input, this.world.getRegistryManager())).orElse(ItemStack.EMPTY);
        if (this.model != null) {
            this.model.setResult(this.outputPreview.copy());
        }
    }

    public void clickForCrafting(ServerPlayerEntity player) {
        var list = DefaultedList.ofSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            var filter = this.filters.get(i);
            if (filter.isEmpty()) {
                continue;
            }

            for (var stack : player.getInventory().getMainStacks()) {
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
        var input = CraftingRecipeInput.create(3, 3, list);

        var optional = this.recipe.getFirstMatch(input, player.getEntityWorld());
        if (optional.isEmpty()) {
            return;
        }
        var recipe = optional.get().value();
        var result = recipe.craft(input, player.getEntityWorld().getRegistryManager());
        if (result.isEmpty()) {
            return;
        }
        player.giveOrDropStack(result);

        var remainders = recipe.getRecipeRemainders(input);
        for (var stack : list) {
            if (!stack.isEmpty()) {
                stack.decrement(1);
            }
        }
        for (var stack : remainders) {
            player.giveOrDropStack(stack);
        }

        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1);
        TriggerCriterion.trigger(player, FactoryTriggers.CRAFT_WITH_BLUEPRINT_WORKBENCH);
        player.swingHand(Hand.MAIN_HAND, true);
    }

    public void markSlotDirty(int index) {
        this.filters.set(index, FilterData.of(this.getStack(index), false));
        if (this.model != null) {
            var icon = this.filters.get(index).icon();
            this.model.setStack(index, icon.isEmpty() ? ItemStack.EMPTY : icon.getFirst());
        }
    }

    @Override
    public void markDirty() {
        this.updatePreview();
        super.markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockAwareAttachment.get(chunk, this.pos).holder() instanceof BlueprintWorkbenchBlock.Model m ? m : null;
        for (int i = 0; i < 9; i++) {
            this.markSlotDirty(i);
        }
        this.updatePreview();
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public static class Gui extends SimpleGui {
        private final BlueprintWorkbenchBlockEntity be;

        public Gui(ServerPlayerEntity player, BlueprintWorkbenchBlockEntity be) {
            super(ScreenHandlerType.CRAFTING, player, false);
            this.be = be;
            this.setTitle(Text.empty().append(Text.literal("" + GuiTextures.BLUEPRINT_WORKSTATION_EXTRA_OFFSET).setStyle(UiResourceCreator.STYLE))
                    .append(GuiTextures.BLUEPRINT_WORKBENCH.apply(be.getCachedState().getBlock().getName())));

            this.setSlot(0, () -> be.outputPreview.copy());

            for (int i = 0; i < 9; i++) {
                this.setSlotRedirect(i + 1, new Slot(be, i, 0, 0));
            }
            this.open();
        }

        @Override
        public void onCraftRequest(NetworkRecipeId recipeId, boolean shift) {
            var recipe = this.getPlayer().getEntityWorld().getServer().getRecipeManager().get(recipeId);
            if (recipe == null || !(recipe.parent().value() instanceof CraftingRecipe craftingRecipe) || craftingRecipe.getIngredientPlacement().hasNoPlacement()) {
                return;
            }

            //noinspection unchecked
            var post = InputSlotFiller.fill(new InputSlotFiller.Handler<>() {
                public void populateRecipeFinder(RecipeFinder finder) {
                    for (int i = 0; i < 9; i++) {
                        finder.addInput(be.getStack(i));
                    }
                }

                public void clear() {}

                public boolean matches(RecipeEntry<CraftingRecipe> entry) {
                    return entry.value().matches(CraftingRecipeInput.create(3, 3, be.stacks), player.getEntityWorld());
                }
            }, 3, 3, this.screenHandler.slots.subList(1, 10), this.screenHandler.slots.subList(1, 10), player.getInventory(), (RecipeEntry<CraftingRecipe>) recipe.parent(), false, false);

            if (post == AbstractRecipeScreenHandler.PostFillAction.PLACE_GHOST_RECIPE) {
                this.player.networkHandler.sendPacket(new CraftFailedResponseS2CPacket(this.player.currentScreenHandler.syncId, recipe.display().display()));
            }
        }

        @Override
        public void onTick() {
            if (this.be.isRemoved() || player.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(this.be.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
