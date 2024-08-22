package eu.pb4.polyfactory.other;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryDamageTypes {
    RegistryKey<DamageType> EXPERIENCE_SPLASH = of("experience_splash");

    static RegistryKey<DamageType> of(String path) {
        return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id(path));
    }
}
