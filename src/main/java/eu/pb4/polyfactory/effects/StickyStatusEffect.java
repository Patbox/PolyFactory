package eu.pb4.polyfactory.effects;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.other.PolymerStatusEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static eu.pb4.polyfactory.ModInit.id;

public class StickyStatusEffect extends StatusEffect implements PolymerStatusEffect {
    //private final Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> wallEffects;
    public StickyStatusEffect(String type, StatusEffectCategory category, int color, ParticleEffect particleEffect) {
        super(category, color, particleEffect);
        var attributeId = id("sticky/" + type);
        this.addAttributeModifier(EntityAttributes.MOVEMENT_SPEED, attributeId, -0.2, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(EntityAttributes.ATTACK_SPEED, attributeId, -0.15, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(EntityAttributes.BLOCK_BREAK_SPEED, attributeId, -0.15, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }


    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        if (entity.isWet()) {
            return false;
        }

        var box = entity.getBoundingBox().expand(0.01, -0.1, 0.01);
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

        return super.applyUpdateEffect(world, entity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    private void updateSlidingVelocity(Entity entity) {
        Vec3d vec3d = entity instanceof ServerPlayerEntity player ? player.getMovement() : entity.getVelocity();
        if (vec3d.y < 0) {
            var y = speedModifier(-0.04);
            entity.setVelocity(new Vec3d(vec3d.x, y, vec3d.z));

            if (entity instanceof ServerPlayerEntity player) {
                if (vec3d.y < y) {
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity()));
                } else {
                    FactoryUtil.sendVelocityDelta(player, new Vec3d(0, y - vec3d.y + entity.getFinalGravity(), 0));
                }
            }

            if (entity.getWorld().random.nextInt(5) == 0) {
                entity.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
                if (!entity.isSilent()) {
                    entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, entity.getSoundCategory(), 1.0F, 1.0F);
                }
            }

            entity.onLanding();
        }
    }

    private static double inverseSpeedModifier(double d) {
        return d / 0.98F + 0.08;
    }

    private static double speedModifier(double d) {
        return (d - 0.08) * 0.98F;
    }

    @Override
    public void onRemoved(AttributeContainer attributeContainer) {
        super.onRemoved(attributeContainer);
    }
}
