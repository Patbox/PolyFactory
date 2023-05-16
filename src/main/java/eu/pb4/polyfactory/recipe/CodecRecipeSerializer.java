package eu.pb4.polyfactory.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.datagen.CodecRecipeJsonProvider;
import eu.pb4.polyfactory.util.Attachable;
import eu.pb4.polymer.core.api.utils.PolymerObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public record CodecRecipeSerializer<T extends Recipe<?>>(Codec<T> codec) implements PolymerObject, RecipeSerializer<T> {
    public static final Codec<Identifier> ID_CODEC = Attachable.codec("id", Identifier.class);
    public static final Codec<Ingredient> INGREDIENT_CODEC = Codecs.JSON_ELEMENT.flatXmap((element) -> {
        try {
            return DataResult.success(Ingredient.fromJson(element));
        } catch (JsonParseException var2) {
            return DataResult.error(var2::getMessage);
        }
    }, (text) -> {
        try {
            return DataResult.success(text.toJson());
        } catch (IllegalArgumentException var2) {
            return DataResult.error(var2::getMessage);
        }
    });

    public static <T> RecordCodecBuilder<T, Identifier> idCodec() {
        return ID_CODEC.optionalFieldOf("type", new Identifier("empty")).forGetter(x -> new Identifier("empty"));
    }

    private static final DynamicRegistryManager STATIC_REGISTRIES = DynamicRegistryManager.of(Registries.REGISTRIES);
    @Override
    public T read(Identifier id, JsonObject json) {
        var ops = RegistryOps.of(JsonOps.INSTANCE, STATIC_REGISTRIES);
        Attachable.set(ops, "id", id);
        return codec.decode(ops, json).getOrThrow(false, ModInit.LOGGER::error).getFirst();
    }

    @Override
    public T read(Identifier id, PacketByteBuf buf) {
        var ops = RegistryOps.of(NbtOps.INSTANCE, STATIC_REGISTRIES);
        Attachable.set(ops, "id", id);
        return codec.decode(ops, buf.readNbt()).result().get().getFirst();
    }

    @Override
    public void write(PacketByteBuf buf, T recipe) {
        buf.writeNbt((NbtCompound) codec.encodeStart(RegistryOps.of(NbtOps.INSTANCE, STATIC_REGISTRIES), recipe).result().get());
    }
}
