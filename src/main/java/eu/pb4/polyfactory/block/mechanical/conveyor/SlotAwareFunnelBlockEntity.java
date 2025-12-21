package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.inventory.MinimalContainer;
import eu.pb4.polyfactory.util.storage.FilteredRedirectedSlottedStorage;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Objects;

public class SlotAwareFunnelBlockEntity extends LockableBlockEntity {
    public static final int TARGET_SLOT_COUNT = 9;

    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.SLOT_AWARE_FUNNEL);
    }

    final NonNullList<FilterData> filter = NonNullList.withSize(TARGET_SLOT_COUNT, FilterData.EMPTY_FALSE);
    final int[] slotTargets = new int[TARGET_SLOT_COUNT];
    private final NonNullList<ItemStack> items = NonNullList.withSize(TARGET_SLOT_COUNT, ItemStack.EMPTY);
    private final Storage<ItemVariant> storage = new FilteredRedirectedSlottedStorage<>(ItemStorage.SIDED,
            this::getLevel, this::getBlockPos, () -> this.getBlockState().getValue(FunnelBlock.FACING), ItemVariant.blank(), this.slotTargets,
            (i, res) -> this.filter.get(i).test(res.toStack()));

    public SlotAwareFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SLOT_AWARE_FUNNEL, pos, state);
        Arrays.fill(this.slotTargets, -1);
    }


    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, items);
        view.putIntArray("targets", slotTargets.clone());
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, items);
        for (var i = 0; i < this.items.size(); i++) {
            this.filter.set(i, FilterData.of(this.items.get(i), false));
        }
        var targets = view.getIntArray("targets").orElseGet(() -> new int[this.slotTargets.length]);
        System.arraycopy(targets, 0, this.slotTargets, 0, Math.min(targets.length, this.slotTargets.length));
    }


    private void updateSlot(int i) {
        this.filter.set(i, FilterData.of(this.items.get(i), false));
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity, this);
    }

    public Container asInventory() {
        return MinimalContainer.createMaxOne(this.items, this::setChanged, this::updateSlot);
    }

    public static class Gui extends SimpleGui {
        private final SlotAwareFunnelBlockEntity be;

        public Gui(ServerPlayer player, SlotAwareFunnelBlockEntity be) {
            super(MenuType.GENERIC_9x2, player, false);
            this.be = be;
            var pseudoInv = be.asInventory();
            this.setTitle(GuiTextures.SLOT_AWARE_FUNNEL.apply(be.getBlockState().getBlock().getName()));
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
                            FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
                        };
                    }
                });
            }
            this.open();
        }

        @Override
        public void onTick() {
            if (this.be.isRemoved() || player.position().distanceToSqr(Vec3.atCenterOf(this.be.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropContents(level, pos, this.asInventory());
        }
    }

    private static class SetSlotGui extends AnvilInputGui {
        private final Gui gui;
        private final int slot;

        public SetSlotGui(ServerPlayer player, Gui gui, int slot) {
            super(player, false);
            this.gui = gui;
            this.slot = slot;
            this.setTitle(GuiTextures.INPUT.apply(Component.translatable("block.polyfactory.slot_aware_funnel.set_slot_title", slot)));
            this.setDefaultInputValue(gui.be.slotTargets[slot] == -1 ? "" : String.valueOf(gui.be.slotTargets[slot]));
            this.updateDone();
            this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(CommonComponents.GUI_BACK).setCallback(x -> {
                FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
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
                this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
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
                this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(CommonComponents.GUI_DONE).setCallback(x -> {
                    FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
                    this.gui.be.slotTargets[this.slot] = finalTargetSlot;
                    this.close(true);
                    this.gui.open();
                }));
            } else {
                this.setSlot(1, GuiTextures.BUTTON_DONE_BLOCKED.get().setName(Component.empty().append(CommonComponents.GUI_DONE).withStyle(ChatFormatting.GRAY)));
            }
        }

        @Override
        public void setDefaultInputValue(String input) {
            super.setDefaultInputValue(input);
            if (this.gui != null) {
                updateDone();
            }
            var itemStack = GuiTextures.EMPTY.getItemStack().copy();
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(input));
            itemStack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
            this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
        }

        @Override
        public void onTick() {
            if (this.gui.be.isRemoved()
                    || player.position().distanceToSqr(Vec3.atCenterOf(this.gui.be.getBlockPos())) > (18 * 18)) {
                this.close();
                return;
            }
            super.onTick();
        }
    }
}
