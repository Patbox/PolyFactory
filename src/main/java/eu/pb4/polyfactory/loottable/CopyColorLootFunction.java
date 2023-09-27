package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class CopyColorLootFunction implements LootFunction {
    public static final LootFunction INSTANCE = new CopyColorLootFunction();
    public static final LootFunctionType TYPE = new LootFunctionType(Codec.unit(INSTANCE));

    @Override
    public LootFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (stack.getItem() instanceof ColoredItem && lootContext.hasParameter(LootContextParameters.BLOCK_ENTITY)) {
            if (lootContext.get(LootContextParameters.BLOCK_ENTITY) instanceof ColorProvider provider && !provider.isDefaultColor()) {
                ColoredItem.setColor(stack, provider.getColor());
            }
        }

        return stack;
    }
}
