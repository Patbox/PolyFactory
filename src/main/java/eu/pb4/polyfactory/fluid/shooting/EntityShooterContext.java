package eu.pb4.polyfactory.fluid.shooting;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

public record EntityShooterContext(LivingEntity entity) implements ShooterContext {
    @Override
    public Random random() {
        return entity.getRandom();
    }

    @Override
    public ServerWorld world() {
        return (ServerWorld) entity.getWorld();
    }

    @Override
    public UUID uuid() {
        return entity.getUuid();
    }

    @Override
    public Vec3d position() {
        return entity.getEyePos().add(entity.getMovement()).subtract(0, 0.1, 0).add(rotation().multiply(0.5));
    }

    @Override
    public Vec3d rotation() {
        return entity.getRotationVecClient();
    }

    @Override
    public SoundCategory soundCategory() {
        return entity.getSoundCategory();
    }
}
