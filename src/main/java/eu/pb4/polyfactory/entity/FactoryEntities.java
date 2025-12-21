package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.entity.splash.*;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class FactoryEntities {
    public static final EntityType<DynamiteEntity> DYNAMITE = register("dynamite", EntityType.Builder.<DynamiteEntity>of(DynamiteEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f));

    public static final EntityType<WaterSplashEntity> WATER_SPLASH = register("water_splash", createSplash(WaterSplashEntity::new));
    public static final EntityType<LavaSplashEntity> LAVA_SPLASH = register("lava_splash", createSplash(LavaSplashEntity::new));
    public static final EntityType<PotionSplashEntity> POTION_SPLASH = register("potion_splash", createSplash(PotionSplashEntity::new));
    public static final EntityType<MilkSplashEntity> MILK_SPLASH = register("milk_splash", createSplash(MilkSplashEntity::new));
    public static final EntityType<ExperienceSplashEntity> EXPERIENCE_SPLASH = register("experience_splash", createSplash(ExperienceSplashEntity::new));

    public static final EntityType<HoneySplashEntity> HONEY_SPLASH = register("honey_splash", createSplash(HoneySplashEntity::new));

    public static final EntityType<SlimeSplashEntity> SLIME_SPLASH = register("slime_splash", createSplash(SlimeSplashEntity::new));

    public static final EntityType<ChainLiftEntity> CHAIN_LIFT = register("chain_lift", EntityType.Builder.of(ChainLiftEntity::new, MobCategory.MISC)
            .noLootTable().sized(0.98f, 2.25f).passengerAttachments(0.05F).clientTrackingRange(8));
    public static final EntityType<MinecartWithBlocksEntity> MINECART_WITH_BLOCKS = register("minecart_with_blocks", EntityType.Builder.of(MinecartWithBlocksEntity::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8));
    public static void register() {

    }


    public static <T extends Entity> EntityType.Builder<T> createSplash(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.of(factory, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(2).noSave();
    }
    public static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> item) {
        var id = Identifier.fromNamespaceAndPath(ModInit.ID, path);
        var x = Registry.register(BuiltInRegistries.ENTITY_TYPE, id, item.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
        PolymerEntityUtils.registerType(x);
        return x;
    }
}
