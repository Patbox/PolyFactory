package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.OptionalDirection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class CopyReadOnlyLootFunction implements LootItemFunction {
    public static final LootItemFunction INSTANCE = new CopyReadOnlyLootFunction();
    public static final LootItemFunctionType TYPE = new LootItemFunctionType(MapCodec.unit(INSTANCE));

    @Override
    public LootItemFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParams.BLOCK_STATE)) {
            var readOnly = lootContext.getOptionalParameter(LootContextParams.BLOCK_STATE).getOptionalValue(DataMemoryBlock.FACING_INPUT);
            readOnly.ifPresent(aBoolean -> stack.set(FactoryDataComponents.READ_ONLY, aBoolean == OptionalDirection.NONE));
        }

        return stack;
    }
}
