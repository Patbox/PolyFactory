package eu.pb4.polyfactory.other;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryBiomeTags {

    TagKey<Biome> WINDMILL_HIGH_SPEED_BONUS = of("windmill/high_speed_bonus");
    TagKey<Biome> WINDMILL_MIDDLE_SPEED_BONUS = of("windmill/middle_speed_bonus");
    TagKey<Biome> WINDMILL_LOW_SPEED_BONUS = of("windmill/low_speed_bonus");
    TagKey<Biome> TEMPERATURE_REALLY_HOT = of("temperature/really_hot");
    TagKey<Biome> TEMPERATURE_HOT = of("temperature/hot");
    TagKey<Biome> TEMPERATURE_COLD = of("temperature/cold");
    TagKey<Biome> TEMPERATURE_REALLY_COLD = of("temperature/really_cold");


    static TagKey<Biome> of(String path) {
        return TagKey.of(RegistryKeys.BIOME, id(path));
    }
}
