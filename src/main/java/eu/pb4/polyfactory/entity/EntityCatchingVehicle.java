package eu.pb4.polyfactory.entity;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import eu.pb4.polyfactory.entity.configurable.EntityConfigValue;
import eu.pb4.polyfactory.entity.configurable.EntityValueFormatter;
import eu.pb4.polyfactory.entity.configurable.WrenchModifyEntityValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.screen.ScreenTexts;

import java.util.List;

public interface EntityCatchingVehicle {
    static EntityConfig<?, ?> CATCH_ENTITIES_CONFIG = EntityConfig.of("catch_entities", Codec.BOOL,
            EntityConfigValue.of(x -> ((EntityCatchingVehicle) x).polyfactory$canCatchEntities(), (x, y) -> ((EntityCatchingVehicle) x).polyfactory$setCatchEntities(y)),
            EntityValueFormatter.text(ScreenTexts::onOrOff),
            WrenchModifyEntityValue.iterate(List.of(false, true))
    );
    static List<EntityConfig<?, ?>> CONFIGS = List.of(
            CATCH_ENTITIES_CONFIG,
            EntityConfig.DISMOUNT
    );

    boolean polyfactory$canCatchEntities();
    void polyfactory$setCatchEntities(boolean value);

}
