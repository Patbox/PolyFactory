package eu.pb4.polyfactory.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ProjectileEntity.class)
public interface ProjectileEntityAccessor {
    @Accessor
    LazyEntityReference<Entity> getOwner();

    @Accessor
    void setOwner(LazyEntityReference<Entity> owner);

    @Accessor
    boolean isLeftOwner();

    @Accessor
    void setLeftOwner(boolean leftOwner);
}
