package eu.pb4.polyfactory.other;

import static eu.pb4.polyfactory.ModInit.id;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public interface FactoryDamageTypes {
    ResourceKey<DamageType> EXPERIENCE_SPLASH = of("experience_splash");

    static ResourceKey<DamageType> of(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, id(path));
    }
}
