package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.entity.splash.*;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryEntities {
    public static final EntityType<DynamiteEntity> DYNAMITE = register("dynamite", EntityType.Builder.create(DynamiteEntity::new, SpawnGroup.MISC)
            .dimensions(0.25f, 0.25f));

    public static final EntityType<WaterSplashEntity> WATER_SPLASH = register("water_splash", createSplash(WaterSplashEntity::new));
    public static final EntityType<LavaSplashEntity> LAVA_SPLASH = register("lava_splash", createSplash(LavaSplashEntity::new));
    public static final EntityType<PotionSplashEntity> POTION_SPLASH = register("potion_splash", createSplash(PotionSplashEntity::new));
    public static final EntityType<MilkSplashEntity> MILK_SPLASH = register("milk_splash", createSplash(MilkSplashEntity::new));
    public static final EntityType<ExperienceSplashEntity> EXPERIENCE_SPLASH = register("experience_splash", createSplash(ExperienceSplashEntity::new));

    public static final EntityType<HoneySplashEntity> HONEY_SPLASH = register("honey_splash", createSplash(HoneySplashEntity::new));

    public static final EntityType<SlimeSplashEntity> SLIME_SPLASH = register("slime_splash", createSplash(SlimeSplashEntity::new));

    public static void register() {

    }


    public static <T extends Entity> EntityType.Builder<T> createSplash(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.create(factory, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(6).trackingTickInterval(2).disableSaving();
    }
    public static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> item) {
        var x = Registry.register(Registries.ENTITY_TYPE, Identifier.of(ModInit.ID, path), item.build());
        PolymerEntityUtils.registerType(x);
        return x;
    }
}
