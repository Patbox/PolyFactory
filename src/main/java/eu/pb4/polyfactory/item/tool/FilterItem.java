package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;


public class FilterItem extends SimplePolymerItem {
    public FilterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity player) {
            new Gui(player, stack);
        }
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public Text getName(ItemStack stack) {
        var filter = getStack(stack);
        return Text.translatable(this.getTranslationKey() + (filter.isEmpty() ? ".empty" : ".with"), filter.getName());
    }

    public static ItemStack getStack(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, ItemStack.EMPTY);
    }

    public static void setStack(ItemStack filter, ItemStack target) {
        if (target.isOf(FactoryItems.ITEM_FILTER)) {
            var alt = getStack(target);
            if (!alt.isEmpty()) {
                target = alt;
            }
        }

        if (target.isEmpty()) {
            filter.remove(FactoryDataComponents.ITEM_FILTER);
        } else {
            filter.set(FactoryDataComponents.ITEM_FILTER, target.copyWithCount(1));
        }
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, net.minecraft.util.ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (otherStack.isEmpty() || clickType == net.minecraft.util.ClickType.LEFT) {
            return false;
        }

        setStack(stack, otherStack);
        return true;
    }

    private static class Gui extends SimpleGui {
        private final ItemStack stack;

        public Gui(ServerPlayerEntity player, ItemStack stack) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.stack = stack;
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(Text.translatable("item.polyfactory.item_filter")));
            this.setSlot(2, new GuiElementInterface() {
                @Override
                public ItemStack getItemStack() {
                    return getStack(stack);
                }

                @Override
                public ClickCallback getGuiCallback() {
                    return Gui.this::onSetSlot;
                }
            });
            this.open();
        }

        private void onSetSlot(int i, ClickType type, SlotActionType slotActionType, SlotGuiInterface guiInterface) {
            setStack(stack, this.screenHandler.getCursorStack());
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
            if (index == -999) {
                return true;
            }

            if (this.screenHandler.getSlot(index).getStack() == stack) {
                GuiHelpers.sendSlotUpdate(player, this.screenHandler.syncId, index, stack, 0);
                return false;
            }

            return super.onAnyClick(index, type, action);
        }
    }
}
