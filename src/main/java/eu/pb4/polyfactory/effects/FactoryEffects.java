package eu.pb4.polyfactory.effects;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.entity.DynamiteEntity;
import eu.pb4.polyfactory.entity.splash.*;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class FactoryEffects {
    public static final RegistryEntry<StatusEffect> STICKY_SLIME = register("sticky/slime",
            new StickyStatusEffect("slime", StatusEffectCategory.HARMFUL,0x73c262,
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState())));

    public static final RegistryEntry<StatusEffect> STICKY_HONEY = register("sticky/honey",
            new StickyStatusEffect("honey", StatusEffectCategory.HARMFUL,0xfaab1c,
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.getDefaultState())));
    public static void register() {

    }

    public static RegistryEntry<StatusEffect> register(String path, StatusEffect effect) {
        var id = Identifier.of(ModInit.ID, path);
        return Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
    }
}
