package eu.pb4.polyfactory.effects;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.entity.DynamiteEntity;
import eu.pb4.polyfactory.entity.splash.*;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactoryEffects {
    public static final Holder<MobEffect> STICKY_SLIME = register("sticky/slime",
            new StickyStatusEffect("slime", MobEffectCategory.HARMFUL,0x73c262,
                    new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.SLIME_BLOCK.defaultBlockState())));

    public static final Holder<MobEffect> STICKY_HONEY = register("sticky/honey",
            new StickyStatusEffect("honey", MobEffectCategory.HARMFUL,0xfaab1c,
                    new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.HONEY_BLOCK.defaultBlockState())));

    public static final Holder<MobEffect> SLIPPERY = register("slippery", new SimpleStatusEffect(MobEffectCategory.HARMFUL,0xccc262)
            .addAttributeModifier(Attributes.FRICTION_MODIFIER, id("slippery/friction_modifier"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.AIR_DRAG_MODIFIER, id("slippery/air_drag_modifier"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("slippery/jump_strength"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );

    public static final Holder<MobEffect> FLAMMABLE = register("flammable", new SimpleStatusEffect(MobEffectCategory.HARMFUL,0x714916)
            .addAttributeModifier(Attributes.BURNING_TIME, id("flammable/burn_time"), 3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );

    public static void register() {

    }

    public static Holder<MobEffect> register(String path, MobEffect effect) {
        var id = Identifier.fromNamespaceAndPath(ModInit.ID, path);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, effect);
    }
}
