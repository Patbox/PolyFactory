package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ModeledItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FilterItem extends ModeledItem {
    public FilterItem(Item item, Settings settings) {
        super(item, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity player) {
            new Gui(player, stack);
        }
        return TypedActionResult.success(stack, true);
    }

    @Override
    public Text getName(ItemStack stack) {
        var filter = getStack(stack);
        return Text.translatable(this.getTranslationKey() + (filter.isEmpty() ? ".empty" : ".with"), filter.getName());
    }

    public static Data createData(ItemStack stack, boolean defaultValue) {
        return new Data(createPredicate(stack, defaultValue), getStack(stack));
    }

    public static ItemStack getStack(ItemStack stack) {
        if (stack.hasNbt() && stack.getNbt().contains("item", NbtElement.COMPOUND_TYPE)) {
            return ItemStack.fromNbt(stack.getNbt().getCompound("item"));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        //tooltip.add(getStack(stack).getName());
        super.appendTooltip(stack, world, tooltip, context);
    }

    public static void setStack(ItemStack filter, ItemStack target) {
        if (target.isOf(FactoryItems.ITEM_FILTER)) {
            var alt = getStack(target);
            if (!alt.isEmpty()) {
                target = alt;
            }
        }

        filter.getOrCreateNbt().put("item", target.copyWithCount(1).writeNbt(new NbtCompound()));
    }

    public static Predicate<ItemStack> createPredicate(ItemStack stack, boolean defaultValue) {
        if (stack.isEmpty()) {
            return x -> defaultValue;
        } else if (stack.isOf(FactoryItems.ITEM_FILTER) && stack.hasNbt() && stack.getNbt().contains("item", NbtElement.COMPOUND_TYPE)) {
            var decodedStack = ItemStack.fromNbt(stack.getNbt().getCompound("item"));
            return x -> ItemStack.canCombine(x, decodedStack);
        } else {
            return x -> ItemStack.canCombine(x, stack);
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
            this.setTitle(Text.translatable("item.polyfactory.item_filter"));
            var x= new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).setName(Text.empty());
            this.setSlot(0, x);
            this.setSlot(1, x);
            this.setSlot(3, x);
            this.setSlot(4, x);
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
            if (this.screenHandler.getSlot(index).getStack() == stack) {
                GuiHelpers.sendSlotUpdate(player, this.screenHandler.syncId, index, stack, 0);
                return false;
            }

            return super.onAnyClick(index, type, action);
        }
    }

    public record Data(Predicate<ItemStack> predicate, ItemStack icon) {
        public boolean test(ItemStack stack) {
            return predicate.test(stack);
        }
    }
}
