package eu.pb4.polyfactory.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CodecRecipeJsonProvider<T extends Recipe<?>>(Codec<T> codec, T recipe) implements RecipeJsonProvider {
    @Override
    public void serialize(JsonObject json) {
        var obj = codec.encodeStart(JsonOps.INSTANCE, recipe).result().get().getAsJsonObject();
        json.asMap().putAll(obj.asMap());
    }

    @Override
    public Identifier getRecipeId() {
        return recipe.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return recipe.getSerializer();
    }

    @Nullable
    @Override
    public JsonObject toAdvancementJson() {
        return null;
    }

    @Nullable
    @Override
    public Identifier getAdvancementId() {
        return null;
    }
}
