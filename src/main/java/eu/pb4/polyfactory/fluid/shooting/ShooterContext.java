package eu.pb4.polyfactory.fluid.shooting;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.UUID;

public interface ShooterContext {
    Random random();
    ServerWorld world();
    UUID uuid();
    Vec3d position();
    Vec3d rotation();
    SoundCategory soundCategory();

    default float force() {
        return 1;
    }

    default float extraSpread() {
        return 0;
    }
}
