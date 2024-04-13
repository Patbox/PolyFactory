package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.block.data.util.DataCache;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class CopyReadOnlyLootFunction implements LootFunction {
    public static final LootFunction INSTANCE = new CopyReadOnlyLootFunction();
    public static final LootFunctionType TYPE = new LootFunctionType(Codec.unit(INSTANCE));

    @Override
    public LootFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParameters.BLOCK_STATE)) {
            var readOnly = lootContext.get(LootContextParameters.BLOCK_STATE).getOrEmpty(DataMemoryBlock.READ_ONLY);
            readOnly.ifPresent(aBoolean -> stack.getOrCreateNbt().putBoolean("read_only", aBoolean));
        }

        return stack;
    }
}
