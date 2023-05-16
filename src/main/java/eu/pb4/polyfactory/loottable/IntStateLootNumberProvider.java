package eu.pb4.polyfactory.loottable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.block.Block;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderType;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonSerializer;

public record IntStateLootNumberProvider(Block block, IntProperty property) implements LootNumberProvider {
    public static final LootNumberProviderType TYPE = new LootNumberProviderType(new JsonSerializer<IntStateLootNumberProvider>() {
        @Override
        public void toJson(JsonObject json, IntStateLootNumberProvider object, JsonSerializationContext context) {
            json.addProperty("property", object.property().getName());
            json.addProperty("block", Registries.BLOCK.getId(object.block).toString());
        }

        @Override
        public IntStateLootNumberProvider fromJson(JsonObject json, JsonDeserializationContext context) {
            var block = Registries.BLOCK.get(Identifier.tryParse(json.get("block").getAsString()));
            var property = (IntProperty) block.getStateManager().getProperty(json.get("property").getAsString());

            return new IntStateLootNumberProvider(block, property);
        }
    });

    @Override
    public float nextFloat(LootContext context) {
        var state = context.get(LootContextParameters.BLOCK_STATE);
        return state.isOf(block) ? state.get(property) : 0;
    }

    @Override
    public LootNumberProviderType getType() {
        return TYPE;
    }
}
