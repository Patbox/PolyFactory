package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.other.FactoryDamageTypes;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class ExperienceSplashEntity extends SplashEntity<Unit> {
    private int amount = 1;

    public ExperienceSplashEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.EXPERIENCE);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("xp", this.amount);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.amount = nbt.getInt("xp");
    }

    @Override
    protected void onBlockHit(BlockHitResult context) {
        var entity = new ExperienceOrbEntity(this.getWorld(), context.getPos().x, context.getPos().y, context.getPos().z, this.amount);
        this.getWorld().spawnEntity(entity);
        super.onBlockHit(context);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.canDamageEntity(entityHitResult.getEntity())) {
            entityHitResult.getEntity().damage(this.getDamageSources().create(FactoryDamageTypes.EXPERIENCE_SPLASH, this, this.getOwner()), 0.5F * amount);
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    protected void onNaturalDiscard() {
        var entity = new ExperienceOrbEntity(this.getWorld(), this.getPos().x, this.getPos().y, this.getPos().z, this.amount);
        this.getWorld().spawnEntity(entity);
        super.onNaturalDiscard();
    }

    @Override
    protected boolean forceParticles() {
        return false;
    }

    @Override
    protected boolean discardInBlock(BlockState state) {
        return false;
    }

    public void setAmount(long l) {
        this.amount = (int) l;
    }
}
