package eu.pb4.polyfactory.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CodecRecipeJsonProvider<T extends Recipe<?>>(Codec<T> codec, RecipeEntry<T> recipe) implements RecipeJsonProvider {
    @Override
    public void serialize(JsonObject json) {
        var obj = codec.encodeStart(JsonOps.INSTANCE, recipe.value()).result().get().getAsJsonObject();
        json.asMap().putAll(obj.asMap());
    }

    @Override
    public Identifier id() {
        return recipe.id();
    }

    @Override
    public RecipeSerializer<?> serializer() {
        return recipe.value().getSerializer();
    }

    @Nullable
    @Override
    public AdvancementEntry advancement() {
        return null;
    }
}
