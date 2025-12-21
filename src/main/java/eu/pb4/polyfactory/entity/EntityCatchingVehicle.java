package eu.pb4.polyfactory.entity;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import eu.pb4.polyfactory.entity.configurable.EntityConfigValue;
import eu.pb4.polyfactory.entity.configurable.EntityValueFormatter;
import eu.pb4.polyfactory.entity.configurable.WrenchModifyEntityValue;
import java.util.List;
import net.minecraft.network.chat.CommonComponents;

public interface EntityCatchingVehicle {
    static EntityConfig<?, ?> CATCH_ENTITIES_CONFIG = EntityConfig.of("catch_entities", Codec.BOOL,
            EntityConfigValue.of(x -> ((EntityCatchingVehicle) x).polyfactory$canCatchEntities(), (x, y) -> ((EntityCatchingVehicle) x).polyfactory$setCatchEntities(y)),
            EntityValueFormatter.text(CommonComponents::optionStatus),
            WrenchModifyEntityValue.iterate(List.of(false, true))
    );
    static List<EntityConfig<?, ?>> CONFIGS = List.of(
            CATCH_ENTITIES_CONFIG,
            EntityConfig.DISMOUNT
    );

    boolean polyfactory$canCatchEntities();
    void polyfactory$setCatchEntities(boolean value);

}
