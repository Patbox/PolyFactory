package eu.pb4.polyfactory.mixin.wrench;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.decoration.ArmorStand;

@Mixin(ArmorStand.class)
public class ArmorStandMixin /*implements ConfigurableEntity<ArmorStandEntity>*/ {
    //@Override
    //public List<EntityConfig<?, ArmorStandEntity>> getEntityConfiguration(ServerPlayerEntity player, Vec3d targetPos) {
    //    return List.of(EntityConfig.YAW.cast(), EntityConfig.PITCH.cast());
    //}
}
