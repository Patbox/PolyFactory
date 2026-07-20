package eu.pb4.polyfactory.effects;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.other.PolymerMobEffect;
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

public class SimpleStatusEffect extends MobEffect implements PolymerMobEffect {
    public SimpleStatusEffect(MobEffectCategory category, int color, ParticleOptions particleEffect) {
        super(category, color, particleEffect);
    }

    public SimpleStatusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
