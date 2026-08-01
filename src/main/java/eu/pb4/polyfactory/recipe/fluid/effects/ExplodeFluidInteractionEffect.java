package eu.pb4.polyfactory.recipe.fluid.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import net.minecraft.core.*;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public record ExplodeFluidInteractionEffect(Optional<Holder<DamageType>> damageType,
                                            Optional<LevelBasedValue> knockbackMultiplier,
                                            Optional<HolderSet<Block>> immuneBlocks, Vec3 offset,
                                            LevelBasedValue radius, boolean createFire,
                                            Level.ExplosionInteraction blockInteraction, ParticleOptions smallParticle,
                                            ParticleOptions largeParticle,
                                            WeightedList<ExplosionParticleInfo> blockParticles,
                                            Holder<SoundEvent> sound) implements FluidInteractionEffect {

    public static final MapCodec<ExplodeFluidInteractionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DamageType.CODEC.optionalFieldOf("damage_type").forGetter(ExplodeFluidInteractionEffect::damageType),
            LevelBasedValue.CODEC.optionalFieldOf("knockback_multiplier").forGetter(ExplodeFluidInteractionEffect::knockbackMultiplier),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("immune_blocks").forGetter(ExplodeFluidInteractionEffect::immuneBlocks),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(ExplodeFluidInteractionEffect::offset),
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(ExplodeFluidInteractionEffect::radius),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(ExplodeFluidInteractionEffect::createFire),
            Level.ExplosionInteraction.CODEC.fieldOf("block_interaction").forGetter(ExplodeFluidInteractionEffect::blockInteraction),
            ParticleTypes.CODEC.fieldOf("small_particle").forGetter(ExplodeFluidInteractionEffect::smallParticle),
            ParticleTypes.CODEC.fieldOf("large_particle").forGetter(ExplodeFluidInteractionEffect::largeParticle),
            WeightedList.codec(ExplosionParticleInfo.CODEC).optionalFieldOf("block_particles", WeightedList.of()).forGetter(ExplodeFluidInteractionEffect::blockParticles),
            SoundEvent.CODEC.fieldOf("sound").forGetter(ExplodeFluidInteractionEffect::sound)
    ).apply(instance, ExplodeFluidInteractionEffect::new));


    public static ExplodeFluidInteractionEffect simple(HolderLookup.Provider access, float power, float perLevel) {
        return new ExplodeFluidInteractionEffect(
                access.get(DamageTypes.EXPLOSION).map(Function.identity()),
                Optional.of(new LevelBasedValue.Constant(1)),
                Optional.empty(),
                Vec3.ZERO,
                LevelBasedValue.perLevel(power, perLevel),
                false,
                Level.ExplosionInteraction.MOB,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                WeightedList.<ExplosionParticleInfo>builder().add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F)).add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F)).build(),
                SoundEvents.GENERIC_EXPLODE
        );
    }

    @Override
    public void apply(ServerLevel serverLevel, FluidContainerInput input, int repetition, @Nullable Entity holderEntity, Vec3 position) {
        Vec3 pos = position.add(this.offset);
        serverLevel.explode(null, this.getDamageSource(holderEntity, pos),
                new SimpleExplosionDamageCalculator(this.blockInteraction != Level.ExplosionInteraction.NONE,
                        this.damageType.isPresent(),
                        this.knockbackMultiplier.map((value) -> value.calculate(repetition)),
                        this.immuneBlocks),
                pos.x(), pos.y(), pos.z(), Math.max(this.radius.calculate(repetition), 0.0F),
                this.createFire, this.blockInteraction, this.smallParticle, this.largeParticle, this.blockParticles, this.sound);
    }

    private @Nullable DamageSource getDamageSource(@Nullable Entity entity, Vec3 position) {
        if (this.damageType.isEmpty()) {
            return null;
        } else {
            return new DamageSource(this.damageType.get(), position);
        }
    }

    public MapCodec<ExplodeFluidInteractionEffect> codec() {
        return CODEC;
    }
}