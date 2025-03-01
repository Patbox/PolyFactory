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
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


public class ImprovedFilterItem extends AbstractFilterItem {
    public ImprovedFilterItem(Settings settings) {
        super(settings.component(FactoryDataComponents.ITEM_FILTER_TYPE, Type.WHITELIST).component(FactoryDataComponents.ITEM_FILTER_MATCH, Match.STRICT));
    }

    @Override
    public void openConfiguration(ServerPlayerEntity player, ItemStack stack) {
        new Gui(player, stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        var stacks = getStacks(stack);
        if (stacks.isEmpty()) {
            return Text.translatable(this.getTranslationKey() + ".empty" );
        }

        return Text.translatable(this.getTranslationKey() + ".with", stacks.size() == 1
                ? stacks.getFirst().getName()
                : Text.empty().append(stacks.getFirst().getName()).append(" ").append(Text.translatable("container.shulkerBox.more", stacks.size() - 1))
                );
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(stack.getOrDefault(FactoryDataComponents.ITEM_FILTER_MATCH, Match.STRICT).asName().formatted(Formatting.YELLOW));
        tooltip.add(stack.getOrDefault(FactoryDataComponents.ITEM_FILTER_TYPE, Type.WHITELIST).asTooltip());

        for (var filtered : getStacks(stack)) {
            tooltip.add(Text.literal(" ").append(filtered.getName()).formatted(Formatting.GRAY));
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
        public Gui(ServerPlayerEntity player, ItemStack stack) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.stack = stack;
            this.setTitle(GuiTextures.ITEM_FILTER.apply(Text.translatable(stack.getItem().getTranslationKey())));

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    int i = x + y * 3;
                    this.setSlot(x + 2 + y * 9, new GuiElementInterface() {
                        @Override
                        public ItemStack getItemStack() {
                            var strict = stack.get(FactoryDataComponents.ITEM_FILTER_MATCH) == Match.STRICT;
                            var stacks = getStacks(stack);
                            if (i < stacks.size()) {
                                return strict ? stacks.get(i).copy() : stacks.get(i).getItem().getDefaultStack();
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

        private <T extends Enum<T>> GuiElementInterface createButton(ComponentType<T> type, T[] values, Function<T, GuiElementBuilder> builderFunction) {
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
            if (this.screenHandler.getCursorStack().isEmpty()) {
                if (i < stacks.size()) {
                    stacks.remove(i);
                } else {
                    return;
                }
            } else if (i < stacks.size()) {
                stacks.set(i, this.screenHandler.getCursorStack().copyWithCount(1));
            } else {
                stacks.add(this.screenHandler.getCursorStack().copyWithCount(1));
            }
            var dedupe = ItemStackSet.create();
            dedupe.addAll(stacks);
            stacks.clear();
            stacks.addAll(dedupe);

            stack.set(FactoryDataComponents.ITEM_FILTER, stacks);
            GuiUtils.playClickSound(player);
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
            if (index == -999 || index == -1) {
                return true;
            }
            var stacks = new ArrayList<>(getStacks(stack));
            if (stacks.size() < 9 && type.shift && index > this.getVirtualSize()) {
                stacks.add(this.screenHandler.getSlot(index).getStack().copyWithCount(1));
                var dedupe = ItemStackSet.create();
                dedupe.addAll(stacks);
                stacks.clear();
                stacks.addAll(dedupe);
                stack.set(FactoryDataComponents.ITEM_FILTER, stacks);
                GuiUtils.playClickSound(player);
            }

            if (this.screenHandler.getSlot(index).getStack() == stack) {
                GuiHelpers.sendSlotUpdate(player, this.screenHandler.syncId, index, stack, 0);
                return false;
            }

            return super.onAnyClick(index, type, action);
        }
    }

    public enum Match implements StringIdentifiable {
        STRICT("strict", GuiTextures.BUTTON_ITEM_FILTER_STRICT),
        TYPE_ONLY("type_only", GuiTextures.BUTTON_ITEM_FILTER_TYPE_ONLY);

        private final String name;
        private final Supplier<GuiElementBuilder> button;

        Match(String name, Supplier<GuiElementBuilder> button) {
            this.name = name;
            this.button = button;
        }

        public GuiElementBuilder createButton() {
            return this.button.get().setName(asName()).addLoreLine(this.asDescription().formatted(Formatting.GRAY));
        }

        @Override
        public String asString() {
            return this.name;
        }

        public MutableText asName() {
            return Text.translatable("item.polyfactory.item_filter.match." + this.name);
        }

        public MutableText asDescription() {
            return Text.translatable("item.polyfactory.item_filter.match." + this.name + ".desc");
        }
    }

    public enum Type implements StringIdentifiable {
        WHITELIST("whitelist", Formatting.GREEN, GuiTextures.BUTTON_DONE),
        BLACKLIST("blacklist", Formatting.RED, GuiTextures.BUTTON_CLOSE);

        private final String name;
        private final Formatting formatting;
        private final Supplier<GuiElementBuilder> button;

        Type(String name, Formatting formatting, Supplier<GuiElementBuilder> button) {
            this.name = name;
            this.formatting = formatting;
            this.button = button;
        }

        @Override
        public String asString() {
            return this.name;
        }


        public GuiElementBuilder createButton() {
            return this.button.get().setName(asName()).addLoreLine(this.asDescription().formatted(Formatting.GRAY));
        }

        public MutableText asName() {
            return Text.translatable("item.polyfactory.item_filter.type." + this.name);
        }

        public MutableText asDescription() {
            return Text.translatable("item.polyfactory.item_filter.type." + this.name + ".desc");
        }

        public MutableText asTooltip() {
            return Text.translatable("item.polyfactory.item_filter.type." + this.name + ".tooltip").formatted(this.formatting);
        }
    }
}
