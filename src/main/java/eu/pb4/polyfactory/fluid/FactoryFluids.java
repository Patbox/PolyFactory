package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

public class FactoryFluids {
    public static final FluidType<Unit> WATER = register(Identifier.ofVanilla("water"),
            FluidType.of().density(100).fluid(Fluids.WATER).color(0x3b3bed).particle(ParticleTypes.FALLING_WATER).build());
    public static final FluidType<Unit> LAVA = register(Identifier.ofVanilla("lava"),
            FluidType.of().density(1000).fluid(Fluids.LAVA).particle(ParticleTypes.FALLING_LAVA).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world.getDimension().ultrawarm() ? 1 : 0.5))
                    .maxFlow(((world, data) -> world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType<Unit> MILK = register(Identifier.ofVanilla("milk"),
            FluidType.of().density(200).particle(EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFF))
                    .flowSpeedMultiplier(0.95)
                    .build());
    public static final FluidType<Unit> EXPERIENCE = register(Identifier.ofVanilla("experience"),
            FluidType.of().density(50).particle(EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0x55FF55))
                    .flowSpeedMultiplier(1.3).maxFlow(FluidConstants.BOTTLE * 2)
                    .build());
    public static final FluidType<PotionContentsComponent> POTION = register(Identifier.ofVanilla("potion"),
            FluidType.of(PotionContentsComponent.CODEC, PotionContentsComponent.DEFAULT).density(150).texture(WATER.texture())
                            .color(PotionContentsComponent::getColor).flowSpeedMultiplier(0.95).name((t, d) -> {
                        var base = Text.translatable(Potion.finishTranslationKey(d.potion(), "item.minecraft.potion.effect."));
                        if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("long_")) {
                            return Text.translatable("fluid_type.minecraft.potion.long", base);
                        }
                        if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("strong_")) {
                            return Text.translatable("fluid_type.minecraft.potion.strong", base);
                        }
                        return base;
                    }).particle((data) -> EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, data.getColor())).build());

    public static void register() {
        FluidBehaviours.addBlockStateConversions(Blocks.WATER.getDefaultState(), Blocks.AIR.getDefaultState(), WATER.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA.getDefaultState(), Blocks.AIR.getDefaultState(), LAVA.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.getDefaultState(), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA_CAULDRON.getDefaultState(), Blocks.CAULDRON.getDefaultState(), LAVA.ofBucket());
    }

    public static <T> FluidType<T> register(Identifier identifier, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, identifier, item);
    }

    public static <T> FluidType<T> register(String path, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.of(ModInit.ID, path), item);
    }
}
