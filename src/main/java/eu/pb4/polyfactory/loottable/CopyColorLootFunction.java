package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class CopyColorLootFunction implements LootItemFunction {
    public static final LootItemFunction INSTANCE = new CopyColorLootFunction();
    public static final LootItemFunctionType TYPE = new LootItemFunctionType(MapCodec.unit(INSTANCE));

    @Override
    public LootItemFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (stack.getItem() instanceof ColoredItem && lootContext.hasParameter(LootContextParams.BLOCK_ENTITY)) {
            if (lootContext.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ColorProvider provider && !provider.isDefaultColor()) {
                ColoredItem.setColor(stack, provider.getColor());
            }
        }

        return stack;
    }
}
