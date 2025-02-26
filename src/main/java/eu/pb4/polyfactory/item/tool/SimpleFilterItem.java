package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.filter.ExactItemFilter;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;


public class SimpleFilterItem extends AbstractFilterItem {
    public SimpleFilterItem(Settings settings) {
        super(settings);
    }

    @Override
    public void openConfiguration(ServerPlayerEntity player, ItemStack stack) {
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
    public Text getName(ItemStack stack) {
        var filter = getStack(stack);
        return Text.translatable(this.getTranslationKey() + (filter.isEmpty() ? ".empty" : ".with"), filter.getName());
    }

    public static ItemStack getStack(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, List.of(ItemStack.EMPTY)).getFirst();
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
            filter.set(FactoryDataComponents.ITEM_FILTER, List.of(target.copyWithCount(1)));
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
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(Text.translatable(stack.getItem().getTranslationKey())));
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
