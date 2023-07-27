package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class FactoryEntityTags {
    public static final TagKey<EntityType<?>> GRID_PASSABLE = of("grid_passable");

    private static TagKey<EntityType<?>> of(String path) {
        return TagKey.of(RegistryKeys.ENTITY_TYPE, ModInit.id(path));
    }}
