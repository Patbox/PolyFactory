package eu.pb4.polyfactory.mixin;

import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ProjectileEntity.class)
public interface ProjectileEntityAccessor {
    @Accessor
    UUID getOwnerUuid();

    @Accessor
    void setOwnerUuid(UUID ownerUuid);
}
