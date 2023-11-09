package eu.pb4.factorytools.api.recipe;

import com.mojang.serialization.Codec;
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

public record LazyRecipeSerializer<T extends Recipe<?>>(Codec<T> codec) implements PolymerObject, RecipeSerializer<T> {
    public static final Codec<Ingredient> INGREDIENT_CODEC = Ingredient.ALLOW_EMPTY_CODEC;

    private static final DynamicRegistryManager STATIC_REGISTRIES = DynamicRegistryManager.of(Registries.REGISTRIES);

    @Override
    public T read(PacketByteBuf buf) {
        return codec.decode(NbtOps.INSTANCE, buf.readNbt()).result().get().getFirst();
    }

    @Override
    public void write(PacketByteBuf buf, T recipe) {
        buf.writeNbt((NbtCompound) codec.encodeStart(RegistryOps.of(NbtOps.INSTANCE, STATIC_REGISTRIES), recipe).result().get());
    }
}
