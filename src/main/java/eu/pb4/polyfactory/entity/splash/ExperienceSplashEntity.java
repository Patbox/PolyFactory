package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.other.FactoryDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class ExperienceSplashEntity extends SplashEntity<Unit> {
    private int amount = 1;

    public ExperienceSplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.EXPERIENCE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putInt("xp", this.amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        this.amount = view.getIntOr("xp", 1);
    }

    @Override
    protected void onHitBlock(BlockHitResult context) {
        var entity = new ExperienceOrb(this.level(), context.getLocation().x, context.getLocation().y, context.getLocation().z, this.amount);
        this.level().addFreshEntity(entity);
        super.onHitBlock(context);
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.level() instanceof ServerLevel world && this.canDamageEntity(entityHitResult.getEntity())) {
            entityHitResult.getEntity().hurtServer(world, this.damageSources().source(FactoryDamageTypes.EXPERIENCE_SPLASH, this, this.getOwner()), 0.05F * amount);
        }
        super.onHitEntity(entityHitResult);
    }

    @Override
    protected void onNaturalDiscard() {
        var entity = new ExperienceOrb(this.level(), this.position().x, this.position().y, this.position().z, this.amount);
        this.level().addFreshEntity(entity);
        super.onNaturalDiscard();
    }

    @Override
    protected boolean forceParticles() {
        return false;
    }

    @Override
    protected boolean discardInBlock(BlockState state, BlockPos blockPos) {
        return false;
    }

    public void setAmount(long l) {
        this.amount = (int) l;
    }
}
