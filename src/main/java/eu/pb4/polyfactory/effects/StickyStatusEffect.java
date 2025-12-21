package eu.pb4.polyfactory.effects;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.other.PolymerStatusEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static eu.pb4.polyfactory.ModInit.id;

public class StickyStatusEffect extends MobEffect implements PolymerStatusEffect {
    //private final Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> wallEffects;
    public StickyStatusEffect(String type, MobEffectCategory category, int color, ParticleOptions particleEffect) {
        super(category, color, particleEffect);
        var attributeId = id("sticky/" + type);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, attributeId, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, attributeId, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, attributeId, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }


    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        if (entity.isUnderWater()) {
            return false;
        }

        var box = entity.getBoundingBox().inflate(0.01, -0.1, 0.01);
        boolean active = false;
        for (var voxelShape : world.getBlockCollisions(entity, box)) {
            if (!voxelShape.isEmpty()) {
                updateSlidingVelocity(entity);
                active = true;
                break;
            }
        }
        //if (entity instanceof ServerPlayerEntity player) {
        //    var y = Math.abs(entity.getMovement().getY());
        //    player.sendMessage(Text.literal("Active: " + active + " | " + (entity.getMovement().getY() < 0 ? "-" : "+") + ((int) y) + "." + ((int) ((y - ((int) y)) * 100))), true);
        //}

        return super.applyEffectTick(world, entity, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private void updateSlidingVelocity(Entity entity) {
        Vec3 vec3d = entity instanceof ServerPlayer player ? player.getKnownMovement() : entity.getDeltaMovement();
        if (vec3d.y < 0) {
            var y = speedModifier(-0.04);
            entity.setDeltaMovement(new Vec3(vec3d.x, y, vec3d.z));

            if (entity instanceof ServerPlayer player) {
                if (vec3d.y < y) {
                    player.connection.send(new ClientboundSetEntityMotionPacket(entity.getId(), entity.getDeltaMovement()));
                } else {
                    FactoryUtil.sendVelocityDelta(player, new Vec3(0, y - vec3d.y + entity.getGravity(), 0));
                }
                TriggerCriterion.trigger(player, FactoryTriggers.STICKY_WALL_SLIDING);
            }

            if (entity.level().random.nextInt(5) == 0) {
                entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
                if (!entity.isSilent()) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, entity.getSoundSource(), 1.0F, 1.0F);
                }
            }

            entity.resetFallDistance();
        }
    }

    private static double inverseSpeedModifier(double d) {
        return d / 0.98F + 0.08;
    }

    private static double speedModifier(double d) {
        return (d - 0.08) * 0.98F;
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeContainer) {
        super.removeAttributeModifiers(attributeContainer);
    }
}
