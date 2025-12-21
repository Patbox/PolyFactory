package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.filter.*;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;


public class ImprovedFilterItem extends AbstractFilterItem {
    public ImprovedFilterItem(Properties settings) {
        super(settings.component(FactoryDataComponents.ITEM_FILTER_TYPE, Type.WHITELIST).component(FactoryDataComponents.ITEM_FILTER_MATCH, Match.STRICT));
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack stack) {
        new Gui(player, stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        var stacks = getStacks(stack);
        if (stacks.isEmpty()) {
            return Component.translatable(this.getDescriptionId() + ".empty" );
        }

        return Component.translatable(this.getDescriptionId() + ".with", stacks.size() == 1
                ? stacks.getFirst().getHoverName()
                : Component.empty().append(stacks.getFirst().getHoverName()).append(" ").append(Component.translatable("item.container.more_items", stacks.size() - 1))
                );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, tooltip, type);
        tooltip.accept(stack.getOrDefault(FactoryDataComponents.ITEM_FILTER_MATCH, Match.STRICT).asName().withStyle(ChatFormatting.YELLOW));
        tooltip.accept(stack.getOrDefault(FactoryDataComponents.ITEM_FILTER_TYPE, Type.WHITELIST).asTooltip());

        for (var filtered : getStacks(stack)) {
            tooltip.accept(Component.literal(" ").append(filtered.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    public static List<ItemStack> getStacks(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, List.<ItemStack>of());
    }

    public boolean isFilterSet(ItemStack stack) {
        return !stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, List.of()).isEmpty();
    }

    public FilterData createFilterData(ItemStack stack) {
        var data = stack.getOrDefault(FactoryDataComponents.ITEM_FILTER, List.<ItemStack>of());
        var list = new ArrayList<ItemFilter>(data.size());

        if (stack.get(FactoryDataComponents.ITEM_FILTER_MATCH) == Match.STRICT) {
            for (var filter : data) {
                list.add(new ExactItemFilter(filter));
            }
        } else {
            for (var filter : data) {
                list.add(new TypeItemFilter(filter.getItem()));
            }
        }

        var matchValue = stack.get(FactoryDataComponents.ITEM_FILTER_TYPE) == Type.WHITELIST;
        return new FilterData(new OneOfItemFilter(list, matchValue), data, !matchValue);
    }

    private static class Gui extends SimpleGui {
        private final ItemStack stack;
        public Gui(ServerPlayer player, ItemStack stack) {
            super(MenuType.GENERIC_9x3, player, false);
            this.stack = stack;
            this.setTitle(GuiTextures.ITEM_FILTER.apply(Component.translatable(stack.getItem().getDescriptionId())));

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    int i = x + y * 3;
                    this.setSlot(x + 2 + y * 9, new GuiElementInterface() {
                        @Override
                        public ItemStack getItemStack() {
                            var strict = stack.get(FactoryDataComponents.ITEM_FILTER_MATCH) == Match.STRICT;
                            var stacks = getStacks(stack);
                            if (i < stacks.size()) {
                                return strict ? stacks.get(i).copy() : stacks.get(i).getItem().getDefaultInstance();
                            }
                            return ItemStack.EMPTY;
                        }

                        @Override
                        public ClickCallback getGuiCallback() {
                            return (a, b, c, d) -> onSetSlot(i, b);
                        }
                    });
                }
            }

            this.setSlot(0 + 6, createButton(FactoryDataComponents.ITEM_FILTER_MATCH, Match.values(), Match::createButton));
            this.setSlot(18 + 6, createButton(FactoryDataComponents.ITEM_FILTER_TYPE, Type.values(), Type::createButton));

            this.open();
        }

        private <T extends Enum<T>> GuiElementInterface createButton(DataComponentType<T> type, T[] values, Function<T, GuiElementBuilder> builderFunction) {
            return new GuiElementInterface() {
                @Override
                public ItemStack getItemStack() {
                    return builderFunction.apply(stack.getOrDefault(type, values[0])).asStack();
                }

                @Override
                public ClickCallback getGuiCallback() {
                    return (i, clickType, slotActionType, slotGuiInterface) -> {
                        GuiUtils.playClickSound(player);
                        stack.set(type, values[(stack.getOrDefault(type, values[0]).ordinal() + 1) % values.length]);
                    };
                }
            };
        }

        private void onSetSlot(int i, ClickType type) {
            var stacks = new ArrayList<>(getStacks(stack));
            if (this.screenHandler.getCarried().isEmpty()) {
                if (i < stacks.size()) {
                    stacks.remove(i);
                } else {
                    return;
                }
            } else if (i < stacks.size()) {
                stacks.set(i, this.screenHandler.getCarried().copyWithCount(1));
            } else {
                stacks.add(this.screenHandler.getCarried().copyWithCount(1));
            }
            var dedupe = ItemStackLinkedSet.createTypeAndComponentsSet();
            dedupe.addAll(stacks);
            stacks.clear();
            stacks.addAll(dedupe);

            stack.set(FactoryDataComponents.ITEM_FILTER, stacks);
            GuiUtils.playClickSound(player);
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action) {
            if (index == -999 || index == -1) {
                return true;
            }
            var stacks = new ArrayList<>(getStacks(stack));
            if (stacks.size() < 9 && type.shift && index > this.getVirtualSize()) {
                stacks.add(this.screenHandler.getSlot(index).getItem().copyWithCount(1));
                var dedupe = ItemStackLinkedSet.createTypeAndComponentsSet();
                dedupe.addAll(stacks);
                stacks.clear();
                stacks.addAll(dedupe);
                stack.set(FactoryDataComponents.ITEM_FILTER, stacks);
                GuiUtils.playClickSound(player);
            }

            if (this.screenHandler.getSlot(index).getItem() == stack) {
                GuiHelpers.sendSlotUpdate(player, this.screenHandler.containerId, index, stack, 0);
                return false;
            }

            return super.onAnyClick(index, type, action);
        }
    }

    public enum Match implements StringRepresentable {
        STRICT("strict", GuiTextures.BUTTON_ITEM_FILTER_STRICT),
        TYPE_ONLY("type_only", GuiTextures.BUTTON_ITEM_FILTER_TYPE_ONLY);

        private final String name;
        private final Supplier<GuiElementBuilder> button;

        Match(String name, Supplier<GuiElementBuilder> button) {
            this.name = name;
            this.button = button;
        }

        public GuiElementBuilder createButton() {
            return this.button.get().setName(asName()).addLoreLine(this.asDescription().withStyle(ChatFormatting.GRAY));
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public MutableComponent asName() {
            return Component.translatable("item.polyfactory.item_filter.match." + this.name);
        }

        public MutableComponent asDescription() {
            return Component.translatable("item.polyfactory.item_filter.match." + this.name + ".desc");
        }
    }

    public enum Type implements StringRepresentable {
        WHITELIST("whitelist", ChatFormatting.GREEN, GuiTextures.BUTTON_DONE),
        BLACKLIST("blacklist", ChatFormatting.RED, GuiTextures.BUTTON_CLOSE);

        private final String name;
        private final ChatFormatting formatting;
        private final Supplier<GuiElementBuilder> button;

        Type(String name, ChatFormatting formatting, Supplier<GuiElementBuilder> button) {
            this.name = name;
            this.formatting = formatting;
            this.button = button;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }


        public GuiElementBuilder createButton() {
            return this.button.get().setName(asName()).addLoreLine(this.asDescription().withStyle(ChatFormatting.GRAY));
        }

        public MutableComponent asName() {
            return Component.translatable("item.polyfactory.item_filter.type." + this.name);
        }

        public MutableComponent asDescription() {
            return Component.translatable("item.polyfactory.item_filter.type." + this.name + ".desc");
        }

        public MutableComponent asTooltip() {
            return Component.translatable("item.polyfactory.item_filter.type." + this.name + ".tooltip").withStyle(this.formatting);
        }
    }
}
