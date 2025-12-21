package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class CopyCachedDataLootFunction implements LootItemFunction {
    public static final CopyCachedDataLootFunction INSTANCE = new CopyCachedDataLootFunction();
    public static final LootItemFunctionType<CopyCachedDataLootFunction> TYPE = new LootItemFunctionType<>(MapCodec.unit(INSTANCE));

    @Override
    public LootItemFunctionType<CopyCachedDataLootFunction> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParams.BLOCK_ENTITY)) {
            if (lootContext.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DataCache provider && provider.getCachedData() != null && !provider.getCachedData().isEmpty()) {
                stack.set(FactoryDataComponents.STORED_DATA, provider.getCachedData());
            }
        }

        return stack;
    }
}
