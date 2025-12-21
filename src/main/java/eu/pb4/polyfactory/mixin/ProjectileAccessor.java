package eu.pb4.polyfactory.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;

@Mixin(Projectile.class)
public interface ProjectileAccessor {
    @Accessor
    EntityReference<Entity> getOwner();

    @Accessor
    void setOwner(EntityReference<Entity> owner);

    @Accessor
    boolean isLeftOwner();

    @Accessor
    void setLeftOwner(boolean leftOwner);
}
