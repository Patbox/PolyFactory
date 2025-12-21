package eu.pb4.polyfactory.fluid.shooting;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public interface ShooterContext {
    RandomSource random();
    ServerLevel world();
    UUID uuid();
    Vec3 position();
    Vec3 rotation();
    SoundSource soundCategory();

    default float force() {
        return 1;
    }

    default float extraSpread() {
        return 0;
    }

    default boolean isEntity() {
        return false;
    }
}
