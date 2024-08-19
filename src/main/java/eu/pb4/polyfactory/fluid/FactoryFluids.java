package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.entity.splash.PotionSplashEntity;
import eu.pb4.polyfactory.fluid.shooting.ShootSplashed;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactorySoundEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.function.Function;

public class FactoryFluids {
    public static final FluidType<Unit> WATER = register(Identifier.ofVanilla("water"),
            FluidType.of().density(100).fluid(Fluids.WATER).color(0x3b3bed)
                    .particle(new ItemStackParticleEffect(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.getDefaultStack()))
                    .shootingBehavior(ShootSplashed.of(FactoryEntities.WATER_SPLASH, 300, FactorySoundEvents.ITEM_FLUID_LAUNCHER_SHOOT_WATER))
                    .build());
    public static final FluidType<Unit> LAVA = register(Identifier.ofVanilla("lava"),
            FluidType.of().density(1000).fluid(Fluids.LAVA).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.getDimension().ultrawarm() ? 1 : 0.5))
                    .shootingBehavior(ShootSplashed.of(FactoryEntities.LAVA_SPLASH, 400, FactorySoundEvents.ITEM_FLUID_LAUNCHER_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType<Unit> MILK = register(Identifier.ofVanilla("milk"),
            FluidType.of().density(200)
                    .flowSpeedMultiplier(0.95)
                    .build());
    public static final FluidType<Unit> EXPERIENCE = register(Identifier.ofVanilla("experience"),
            FluidType.of().density(50).flowSpeedMultiplier(1.3).maxFlow(FluidConstants.BOTTLE * 2).build());
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
                    }).particle((data) -> EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, data.data().getColor()))
                    .shootingBehavior(new ShootSplashed<>(PotionSplashEntity::of, 4, FluidConstants.BOTTLE / 20, FactorySoundEvents.ITEM_FLUID_LAUNCHER_SHOOT_WATER))
                    .build());

    public static final FluidType<Unit> HONEY = register(Identifier.ofVanilla("honey"),
            FluidType.of().density(500).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3).build());

    public static final FluidType<Unit> SLIME = register(Identifier.ofVanilla("slime"),
            FluidType.of().density(600).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3).build());
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

        FluidBehaviours.addBlockStateInsert(Blocks.SLIME_BLOCK.getDefaultState(), Blocks.AIR.getDefaultState(), SLIME.ofBucket());
        FluidBehaviours.addBlockStateInsert(Blocks.HONEY_BLOCK.getDefaultState(), Blocks.AIR.getDefaultState(), HONEY.ofBucket());

        FluidBehaviours.addItemToFluidLink(Items.BUCKET, (FluidInstance<?>) null);
        FluidBehaviours.addItemToFluidLink(Items.WATER_BUCKET, WATER.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.LAVA_BUCKET, LAVA.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.MILK_BUCKET, MILK.defaultInstance());
        FluidBehaviours.addItemToFluidLink(FactoryItems.EXPERIENCE_BUCKET, EXPERIENCE.defaultInstance());
        FluidBehaviours.addItemToFluidLink(FactoryItems.SLIME_BUCKET, SLIME.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.SLIME_BALL, SLIME.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.SLIME_BLOCK, SLIME.defaultInstance());
        FluidBehaviours.addItemToFluidLink(FactoryItems.HONEY_BUCKET, HONEY.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.HONEY_BLOCK, HONEY.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.HONEY_BOTTLE, HONEY.defaultInstance());

        Function<ItemStack, FluidInstance<?>> potionFunction = (stack) -> {
            var x = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            if (x.potion().isPresent() && x.potion().get() == Potions.WATER) {
                return WATER.defaultInstance();
            }
            return POTION.toInstance(x);
        };

        FluidBehaviours.addItemToFluidLink(Items.POTION, potionFunction);
        FluidBehaviours.addItemToFluidLink(Items.SPLASH_POTION, potionFunction);
        FluidBehaviours.addItemToFluidLink(Items.LINGERING_POTION, potionFunction);
    }

    public static <T> FluidType<T> register(Identifier identifier, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, identifier, item);
    }

    public static <T> FluidType<T> register(String path, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.of(ModInit.ID, path), item);
    }
}
