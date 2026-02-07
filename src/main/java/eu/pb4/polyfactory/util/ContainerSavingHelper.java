package eu.pb4.polyfactory.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ContainerSavingHelper {
    public static final String TAG_ITEMS = "Items";
    public static final Codec<ItemStackWithSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("Slot").orElse(0).forGetter(ItemStackWithSlot::slot),
                    ItemStack.MAP_CODEC.forGetter(ItemStackWithSlot::stack))
            .apply(instance, ItemStackWithSlot::new));


    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items) {
        saveAllItems(TAG_ITEMS, output, items);
    }

    public static void saveAllItems(String tag, ValueOutput output, NonNullList<ItemStack> items) {
        var list = output.list(tag, CODEC);

        for (int i = 0; i < items.size(); ++i) {
            var itemStack = items.get(i);
            if (!itemStack.isEmpty()) {
                list.add(new ItemStackWithSlot(i, itemStack));
            }
        }

        //if (typedOutputList.isEmpty() && !allowEmpty) {
        //    output.discard(TAG_ITEMS);
        //}
    }

    public static void loadAllItems(ValueInput input, NonNullList<ItemStack> items) {
        loadAllItems(TAG_ITEMS, input, items);
    }

    public static void loadAllItems(String tag, ValueInput input, NonNullList<ItemStack> items) {
        for (var stack : input.listOrEmpty(tag, CODEC)) {
            if (stack.isValidInContainer(items.size())) {
                items.set(stack.slot(), stack.stack());
            }
        }
    }
}
