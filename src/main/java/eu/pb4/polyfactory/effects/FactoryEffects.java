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
import net.minecraft.world.level.block.Blocks;

public class FactoryEffects {
    public static final Holder<MobEffect> STICKY_SLIME = register("sticky/slime",
            new StickyStatusEffect("slime", MobEffectCategory.HARMFUL,0x73c262,
                    new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.SLIME_BLOCK.defaultBlockState())));

    public static final Holder<MobEffect> STICKY_HONEY = register("sticky/honey",
            new StickyStatusEffect("honey", MobEffectCategory.HARMFUL,0xfaab1c,
                    new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.HONEY_BLOCK.defaultBlockState())));
    public static void register() {

    }

    public static Holder<MobEffect> register(String path, MobEffect effect) {
        var id = Identifier.fromNamespaceAndPath(ModInit.ID, path);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, effect);
    }
}
