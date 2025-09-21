package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;

import java.util.List;

@Mixin(ArmorStandEntity.class)
public class ArmorStandEntityMixin /*implements ConfigurableEntity<ArmorStandEntity>*/ {
    //@Override
    //public List<EntityConfig<?, ArmorStandEntity>> getEntityConfiguration(ServerPlayerEntity player, Vec3d targetPos) {
    //    return List.of(EntityConfig.YAW.cast(), EntityConfig.PITCH.cast());
    //}
}
