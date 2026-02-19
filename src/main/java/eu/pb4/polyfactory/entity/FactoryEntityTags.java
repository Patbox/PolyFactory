package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class FactoryEntityTags {
    public static final TagKey<EntityType<?>> GRID_PASSABLE = of("grid_passable");
    public static final TagKey<EntityType<?>> FLUID_SPLASH = of("fluid_splash");

    private static TagKey<EntityType<?>> of(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ModInit.id(path));
    }}
