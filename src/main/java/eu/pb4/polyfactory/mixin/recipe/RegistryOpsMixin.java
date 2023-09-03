package eu.pb4.polyfactory.mixin.recipe;

import eu.pb4.polyfactory.util.Attachable;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(RegistryOps.class)
public class RegistryOpsMixin implements Attachable {
    @Unique
    private Map<String, Object> polyFactory$map;

    @Override
    public void polyFactory$set(String key, Object value) {
        if (polyFactory$map == null) {
            polyFactory$map = new Object2ObjectOpenHashMap<>();
        }
        polyFactory$map.put(key, value);
    }

    @Override
    public Object polyFactory$get(String key) {
        if (polyFactory$map == null) {
            return null;
        }
        return polyFactory$map.get(key);
    }
}
