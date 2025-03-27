package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.inventory.MinimalInventory;
import eu.pb4.polyfactory.util.storage.FilteredRedirectedSlottedStorage;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Unit;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Objects;

public class SlotAwareFunnelBlockEntity extends LockableBlockEntity {
    public static final int TARGET_SLOT_COUNT = 9;

    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.SLOT_AWARE_FUNNEL);
    }

    final DefaultedList<FilterData> filter = DefaultedList.ofSize(TARGET_SLOT_COUNT, FilterData.EMPTY_FALSE);
    final int[] slotTargets = new int[TARGET_SLOT_COUNT];
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TARGET_SLOT_COUNT, ItemStack.EMPTY);
    private final Storage<ItemVariant> storage = new FilteredRedirectedSlottedStorage<>(ItemStorage.SIDED,
            this::getWorld, this::getPos, () -> this.getCachedState().get(FunnelBlock.FACING), ItemVariant.blank(), this.slotTargets,
            (i, res) -> this.filter.get(i).test(res.toStack()));

    public SlotAwareFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SLOT_AWARE_FUNNEL, pos, state);
        Arrays.fill(this.slotTargets, -1);
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.writeNbt(nbt, items, lookup);
        nbt.putIntArray("targets", slotTargets.clone());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.readNbt(nbt, items, lookup);
        for (var i = 0; i < this.items.size(); i++) {
            this.filter.set(i, FilterData.of(this.items.get(i), false));
        }
        var targets = nbt.getIntArray("targets").orElseGet(() -> new int[this.slotTargets.length]);
        System.arraycopy(targets, 0, this.slotTargets, 0, Math.min(targets.length, this.slotTargets.length));
    }


    private void updateSlot(int i) {
        this.filter.set(i, FilterData.of(this.items.get(i), false));
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity, this);
    }

    public Inventory asInventory() {
        return MinimalInventory.createMaxOne(this.items, this::markDirty, this::updateSlot);
    }

    public static class Gui extends SimpleGui {
        private final SlotAwareFunnelBlockEntity be;

        public Gui(ServerPlayerEntity player, SlotAwareFunnelBlockEntity be) {
            super(ScreenHandlerType.GENERIC_9X2, player, false);
            this.be = be;
            var pseudoInv = be.asInventory();
            this.setTitle(GuiTextures.SLOT_AWARE_FUNNEL.apply(be.getCachedState().getBlock().getName()));
            for (int i = 0; i < 9; i++) {
                this.setSlotRedirect(i, new Slot(pseudoInv, i, 0, 0));
                final int index = i;
                this.setSlot(9 + i, new GuiElementInterface() {
                    @Override
                    public ItemStack getItemStack() {
                        return (be.slotTargets[index] < 0 ? GuiTextures.NUMBERED_BUTTONS_DISABLED : GuiTextures.NUMBERED_BUTTONS[Math.min(be.slotTargets[index], 99)])
                                .get().hideTooltip().asStack();
                    }

                    @Override
                    public ClickCallback getGuiCallback() {
                        return (i1, clickType, slotActionType, slotGuiInterface) -> {
                            new SetSlotGui(player, Gui.this, index);
                            player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                        };
                    }
                });
            }
            this.open();
        }

        @Override
        public void onTick() {
            if (this.be.isRemoved() || player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.be.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null) {
            ItemScatterer.spawn(world, pos, this.asInventory());
        }
    }

    private static class SetSlotGui extends AnvilInputGui {
        private final Gui gui;
        private final int slot;

        public SetSlotGui(ServerPlayerEntity player, Gui gui, int slot) {
            super(player, false);
            this.gui = gui;
            this.slot = slot;
            this.setTitle(GuiTextures.INPUT.apply(Text.translatable("block.polyfactory.slot_aware_funnel.set_slot_title", slot)));
            this.setDefaultInputValue(gui.be.slotTargets[slot] == -1 ? "" : String.valueOf(gui.be.slotTargets[slot]));
            this.updateDone();
            this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(ScreenTexts.BACK).setCallback(x -> {
                player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                this.close(true);
                this.gui.open();
            }));
            this.open();
        }

        @Override
        public void onInput(String input) {
            super.onInput(input);
            this.updateDone();
            if (this.screenHandler != null) {
                this.screenHandler.setReceivedStack(2, ItemStack.EMPTY);
            }
        }

        private void updateDone() {
            int targetSlot = -2;
            if (this.getInput().isEmpty()) {
                targetSlot = -1;
            }
            try {
                targetSlot = Integer.parseInt(this.getInput());
            } catch (Throwable ignored) {
            }


            if (targetSlot > -2 && targetSlot < 100) {
                int finalTargetSlot = targetSlot;
                this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(ScreenTexts.DONE).setCallback(x -> {
                    player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                    this.gui.be.slotTargets[this.slot] = finalTargetSlot;
                    this.close(true);
                    this.gui.open();
                }));
            } else {
                this.setSlot(1, GuiTextures.BUTTON_DONE_BLOCKED.get().setName(Text.empty().append(ScreenTexts.DONE).formatted(Formatting.GRAY)));
            }
        }

        @Override
        public void setDefaultInputValue(String input) {
            super.setDefaultInputValue(input);
            if (this.gui != null) {
                updateDone();
            }
            var itemStack = GuiTextures.EMPTY.getItemStack().copy();
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(input));
            itemStack.set(DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplayComponent(true, ReferenceSortedSets.emptySet()));
            this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
        }

        @Override
        public void onTick() {
            if (this.gui.be.isRemoved()
                    || player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.gui.be.getPos())) > (18 * 18)) {
                this.close();
                return;
            }
            super.onTick();
        }
    }
}
