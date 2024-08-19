package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.entity.splash.LavaSplashEntity;
import eu.pb4.polyfactory.entity.splash.WaterSplashEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryEntities {
    public static final EntityType<DynamiteEntity> DYNAMITE = register("dynamite", EntityType.Builder.create(DynamiteEntity::new, SpawnGroup.MISC)
            .dimensions(0.25f, 0.25f));

    public static final EntityType<WaterSplashEntity> WATER_SPLASH = register("water_splash",
            EntityType.Builder.create(WaterSplashEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(16).trackingTickInterval(4));

    public static final EntityType<LavaSplashEntity> LAVA_SPLASH = register("lava_splash",
            EntityType.Builder.create(LavaSplashEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(16).trackingTickInterval(1));

    public static void register() {

    }

    public static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> item) {
        var x = Registry.register(Registries.ENTITY_TYPE, Identifier.of(ModInit.ID, path), item.build());
        PolymerEntityUtils.registerType(x);
        return x;
    }
}
