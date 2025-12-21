package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.filter.ExactItemFilter;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


public class SimpleFilterItem extends AbstractFilterItem {
    public SimpleFilterItem(Properties settings) {
        super(settings);
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack stack) {
        new Gui(player, stack);
    }

    @Override
    public boolean isFilterSet(ItemStack stack) {
        return !getStack(stack).isEmpty();
    }

    @Override
    public FilterData createFilterData(ItemStack stack) {
        var filter = getStack(stack).copy();
        return new FilterData(new ExactItemFilter(filter), List.of(filter), false);
    }

    @Override
    public Component getName(ItemStack stack) {
        var filter = getStack(stack);
        return Component.translatable(this.getDescriptionId() + (filter.isEmpty() ? ".empty" : ".with"), filter.getHoverName());
    }

    public static ItemStack getStack(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, List.of(ItemStack.EMPTY)).getFirst();
    }

    public static void setStack(ItemStack filter, ItemStack target) {
        if (target.is(FactoryItems.ITEM_FILTER)) {
            var alt = getStack(target);
            if (!alt.isEmpty()) {
                target = alt;
            }
        }

        if (target.isEmpty()) {
            filter.remove(FactoryDataComponents.ITEM_FILTER);
        } else {
            filter.set(FactoryDataComponents.ITEM_FILTER, List.of(target.copyWithCount(1)));
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, net.minecraft.world.inventory.ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        if (otherStack.isEmpty() || clickType == net.minecraft.world.inventory.ClickAction.PRIMARY) {
            return false;
        }

        setStack(stack, otherStack);
        return true;
    }

    private static class Gui extends SimpleGui {
        private final ItemStack stack;

        public Gui(ServerPlayer player, ItemStack stack) {
            super(MenuType.HOPPER, player, false);
            this.stack = stack;
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(Component.translatable(stack.getItem().getDescriptionId())));
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

        private void onSetSlot(int i, ClickType type, net.minecraft.world.inventory.ClickType slotActionType, SlotGuiInterface guiInterface) {
            setStack(stack, this.screenHandler.getCarried());
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action) {
            if (index == -999) {
                return true;
            }

            if (this.screenHandler.getSlot(index).getItem() == stack) {
                GuiHelpers.sendSlotUpdate(player, this.screenHandler.containerId, index, stack, 0);
                return false;
            }

            return super.onAnyClick(index, type, action);
        }
    }
}
