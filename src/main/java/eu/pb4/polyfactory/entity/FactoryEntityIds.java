package eu.pb4.polyfactory.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryEntityIds {
    public static final ResourceKey<EntityType<?>> DYNAMITE = of("dynamite");

    public static final ResourceKey<EntityType<?>> WATER_SPLASH = of("water_splash");
    public static final ResourceKey<EntityType<?>> LAVA_SPLASH = of("lava_splash");
    public static final ResourceKey<EntityType<?>> POTION_SPLASH = of("potion_splash");
    public static final ResourceKey<EntityType<?>> MILK_SPLASH = of("milk_splash");
    public static final ResourceKey<EntityType<?>> EXPERIENCE_SPLASH = of("experience_splash");

    public static final ResourceKey<EntityType<?>> HONEY_SPLASH = of("honey_splash");

    public static final ResourceKey<EntityType<?>> SLIME_SPLASH = of("slime_splash");

    public static final ResourceKey<EntityType<?>> CHAIN_LIFT = of("chain_lift");
    public static final ResourceKey<EntityType<?>> MINECART_WITH_BLOCKS = of("minecart_with_blocks");

    public static  ResourceKey<EntityType<?>> of(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id(path));
    }
}
