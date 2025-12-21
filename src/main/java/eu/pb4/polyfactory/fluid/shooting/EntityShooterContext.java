package eu.pb4.polyfactory.fluid.shooting;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record EntityShooterContext(LivingEntity entity) implements ShooterContext {
    @Override
    public RandomSource random() {
        return entity.getRandom();
    }

    @Override
    public ServerLevel world() {
        return (ServerLevel) entity.level();
    }

    @Override
    public UUID uuid() {
        return entity.getUUID();
    }

    @Override
    public Vec3 position() {
        return entity.getEyePosition().add(entity.getKnownMovement()).subtract(0, 0.1, 0).add(rotation().scale(0.5));
    }

    @Override
    public Vec3 rotation() {
        return entity.getForward();
    }

    @Override
    public SoundSource soundCategory() {
        return entity.getSoundSource();
    }

    @Override
    public boolean isEntity() {
        return true;
    }
}
