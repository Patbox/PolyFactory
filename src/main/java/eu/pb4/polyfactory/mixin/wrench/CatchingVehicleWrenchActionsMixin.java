package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Consumer;

@Mixin({MinecartEntity.class, AbstractBoatEntity.class})
public abstract class CatchingVehicleWrenchActionsMixin implements ConfigurableEntity<VehicleEntity>, EntityCatchingVehicle {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<EntityConfig<?, VehicleEntity>> getEntityConfiguration(ServerPlayerEntity player, Vec3d targetPos) {
        //noinspection unchecked
        return (List<EntityConfig<?, VehicleEntity>>) (Object) EntityCatchingVehicle.CONFIGS;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public void writeMinimalConfigurationToStack(Consumer<EntityConfig<?, VehicleEntity>> consumer) {
        if (!this.polyfactory$canCatchEntities()) {
            consumer.accept(CATCH_ENTITIES_CONFIG.cast());
        }
    }
}
