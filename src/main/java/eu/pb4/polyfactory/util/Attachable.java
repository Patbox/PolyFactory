package eu.pb4.polyfactory.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.registry.RegistryOps;

import javax.xml.crypto.Data;

public interface Attachable {
    void polyFactory$set(String key, Object value);
    Object polyFactory$get(String key);

    static <T> RegistryOps<T> set(RegistryOps<T> registryOps, String key, Object value) {
        ((Attachable) registryOps).polyFactory$set(key, value);
        return registryOps;
    }

    static <T> Codec<T> codec(String key, Class<T> tClass) {
        return new Codec<T>() {
            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                if (ops instanceof Attachable attachable) {
                    var x = attachable.polyFactory$get(key);

                    return x != null ? DataResult.success(new Pair<>(tClass.cast(x), input)) : DataResult.error(() -> key + " is not set! (Attachable)");
                }

                return DataResult.error(() -> "Ops isn't attachable!");
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                return DataResult.success(prefix);
            }
        };
    }
}
