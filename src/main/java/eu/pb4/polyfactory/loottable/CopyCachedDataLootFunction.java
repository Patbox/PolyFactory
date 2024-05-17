package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class CopyCachedDataLootFunction implements LootFunction {
    public static final CopyCachedDataLootFunction INSTANCE = new CopyCachedDataLootFunction();
    public static final LootFunctionType<CopyCachedDataLootFunction> TYPE = new LootFunctionType<>(MapCodec.unit(INSTANCE));

    @Override
    public LootFunctionType<CopyCachedDataLootFunction> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParameters.BLOCK_ENTITY)) {
            if (lootContext.get(LootContextParameters.BLOCK_ENTITY) instanceof DataCache provider && provider.getCachedData() != null && !provider.getCachedData().isEmpty()) {
                stack.set(FactoryDataComponents.STORED_DATA, provider.getCachedData());
            }
        }

        return stack;
    }
}
