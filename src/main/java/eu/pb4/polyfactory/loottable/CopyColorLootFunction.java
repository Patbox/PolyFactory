package eu.pb4.polyfactory.loottable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import eu.pb4.polyfactory.item.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.JsonSerializer;

public class CopyColorLootFunction implements LootFunction {
    public static final LootFunction INSTANCE = new CopyColorLootFunction();
    public static final LootFunctionType TYPE = new LootFunctionType(new JsonSerializer<LootFunction>() {
        @Override
        public void toJson(JsonObject json, LootFunction object, JsonSerializationContext context) {}

        @Override
        public LootFunction fromJson(JsonObject json, JsonDeserializationContext context) {
            return CopyColorLootFunction.INSTANCE;
        }
    });

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
