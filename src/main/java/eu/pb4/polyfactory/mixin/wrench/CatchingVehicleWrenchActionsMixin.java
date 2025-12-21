package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.Vec3;

@Mixin({Minecart.class, AbstractBoat.class})
public abstract class CatchingVehicleWrenchActionsMixin implements ConfigurableEntity<VehicleEntity>, EntityCatchingVehicle {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<EntityConfig<?, VehicleEntity>> getEntityConfiguration(ServerPlayer player, Vec3 targetPos) {
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
