package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.fluid.shooting.ShootProjectileEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryFluids {
    public static final FluidType<Unit> WATER = register(Identifier.withDefaultNamespace("water"),
            FluidType.of().density(100).fluid(Fluids.WATER).color(0x385dc6)
                    .particle(new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.getDefaultInstance()))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.WATER_SPLASH, 10,300, FactorySoundEvents.FLUID_SHOOT_WATER))
                    .build());
    public static final FluidType<Unit> LAVA = register(Identifier.withDefaultNamespace("lava"),
            FluidType.of().density(1000).fluid(Fluids.LAVA).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType<Unit> MILK = register(Identifier.withDefaultNamespace("milk"),
            FluidType.of().density(200).flowSpeedMultiplier(0.95)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.MILK_SPLASH, 10,350, FactorySoundEvents.FLUID_SHOOT_MILK))
                    .build());

    public static final FluidType<Unit> GLASS = register(Identifier.withDefaultNamespace("glass"),
            FluidType.of().density(800).brightness(15).heat(BlockHeat.LAVA / 2)
                    .solidTexture(id("block/fluid/glass_solid"))
                    .flowSpeedMultiplier(1)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow((world, data) -> FluidConstants.BOTTLE * 2 / 3).build());

    public static final FluidType<Unit> IRON = register(Identifier.withDefaultNamespace("iron"),
            FluidType.of().density(1005).brightness(15).heat(BlockHeat.LAVA)
                    .solidTexture(id("block/fluid/iron_solid"))
                    .flowSpeedMultiplier(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());

    public static final FluidType<Unit> STEEL = register(id("steel"),
            FluidType.of().density(1007).brightness(15).heat(BlockHeat.LAVA)
                    .solidTexture(id("block/fluid/steel_solid"))
                    .flowSpeedMultiplier(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());


    public static final FluidType<Unit> GOLD = register(Identifier.withDefaultNamespace("gold"),
            FluidType.of().density(1010).brightness(15).heat(BlockHeat.LAVA)
                    .solidTexture(id("block/fluid/gold_solid"))
                    .flowSpeedMultiplier(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());

    public static final FluidType<Unit> COPPER = register(Identifier.withDefaultNamespace("copper"),
            FluidType.of().density(1000).brightness(15).heat(BlockHeat.LAVA)
                    .solidTexture(id("block/fluid/copper_solid"))
                    .flowSpeedMultiplier(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    public static final FluidType<Unit> EXPERIENCE = register(Identifier.withDefaultNamespace("experience"),
            FluidType.of().density(50).flowSpeedMultiplier(1.3).maxFlow(FluidConstants.BOTTLE * 2).brightness(14).heat(BlockHeat.EXPERIENCE)
                    .shootingBehavior(new ShootProjectileEntity<>((world, fluid, amount) -> {
                        var xp = FactoryEntities.EXPERIENCE_SPLASH.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
                        assert xp != null;
                        xp.setAmount(amount / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID);
                        return xp;
                    }, 1,
                            (world, c, f, test) -> test
                                    ? FluidBehaviours.EXPERIENCE_ORB_TO_FLUID
                                    : Mth.clamp(c.get(f) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, 1, 3) * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID,
                             2, 0.5f, 0.1f, 0.3f, FactorySoundEvents.FLUID_SHOOT_EXPERIENCE))
                    .build());
    public static final FluidType<PotionContents> POTION = register(Identifier.withDefaultNamespace("potion"),
            FluidType.of(PotionContents.CODEC, PotionContents.EMPTY).density(150).texture(WATER.texture())
                            .color(PotionContents::getColor).flowSpeedMultiplier(0.95).name((t, d) -> {
                        var base = d.getName("item.minecraft.potion.effect.");
                        if (d.potion().isPresent() && d.potion().get().unwrapKey().get().identifier().getPath().startsWith("long_")) {
                            return Component.translatable("fluid_type.minecraft.potion.long", base);
                        }
                        if (d.potion().isPresent() && d.potion().get().unwrapKey().get().identifier().getPath().startsWith("strong_")) {
                            return Component.translatable("fluid_type.minecraft.potion.strong", base);
                        }
                        return base;
                    }).particle((data) -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, data.data().getColor()))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.POTION_SPLASH, 4, FluidConstants.BOTTLE / 60, FactorySoundEvents.FLUID_SHOOT_POTION))
                    .build());

    public static final FluidType<Unit> HONEY = register(Identifier.withDefaultNamespace("honey"),
            FluidType.of().density(500).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.HONEY_SPLASH, 3, FluidConstants.BOTTLE / 80, FactorySoundEvents.FLUID_SHOOT_HONEY))
                    .build());

    public static final FluidType<Unit> SLIME = register(Identifier.withDefaultNamespace("slime"),
            FluidType.of().density(600).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.SLIME_SPLASH, 3, FluidConstants.BOTTLE / 80, FactorySoundEvents.FLUID_SHOOT_SLIME))
                    .build());

    public static final FluidType<Unit> SNOW = register(Identifier.withDefaultNamespace("snow"),
            FluidType.of().density(90).flowSpeedMultiplier(0.98).maxFlow(FluidConstants.BOTTLE * 4 / 5).heat(BlockHeat.SNOW)
                    .shootingBehavior(ShootProjectileEntity.ofEntity(EntityType.SNOWBALL, 1, FluidConstants.BLOCK / 4 / 20 / 2,
                            1.7f, 0.5f, 0, 0.1f, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.SNOWBALL_THROW)))
                    .texture(Identifier.withDefaultNamespace("block/powder_snow")).build());
    public static void register() {
        FluidBehaviours.addBlockStateConversions(Blocks.WATER.defaultBlockState(), Blocks.AIR.defaultBlockState(), WATER.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW.defaultBlockState(), Blocks.AIR.defaultBlockState(), SNOW.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA.defaultBlockState(), Blocks.AIR.defaultBlockState(), LAVA.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.defaultBlockState(), WATER.ofBottle());

        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2), SNOW.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2),
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1), SNOW.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.defaultBlockState(), SNOW.ofBottle());


        FluidBehaviours.addBlockStateConversions(Blocks.LAVA_CAULDRON.defaultBlockState(), Blocks.CAULDRON.defaultBlockState(), LAVA.ofBucket());

        FluidBehaviours.addBlockStateInsert(Blocks.SLIME_BLOCK.defaultBlockState(), Blocks.AIR.defaultBlockState(), SLIME.ofBucket());

        for (var dir : BeehiveBlock.FACING.getPossibleValues()) {
            FluidBehaviours.addBlockStateExtract(Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.FACING, dir).setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS),
                    Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.FACING, dir), HONEY.of(FluidConstants.BLOCK / 4));
        }


        FluidBehaviours.addBlockStateInsert(Blocks.HONEY_BLOCK.defaultBlockState(), Blocks.AIR.defaultBlockState(), HONEY.ofBucket());

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
        FluidBehaviours.addItemToFluidLink(Items.IRON_INGOT, IRON.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.GOLD_INGOT, GOLD.defaultInstance());
        FluidBehaviours.addItemToFluidLink(Items.COPPER_INGOT, COPPER.defaultInstance());
        FluidBehaviours.addItemToFluidLink(FactoryItems.STEEL_INGOT, STEEL.defaultInstance());

        Function<ItemStack, FluidInstance<?>> potionFunction = (stack) -> {
            var x = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
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
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
    }

    public static FluidInstance<?> getPotion(Holder<Potion> potion) {
        if (potion == Potions.WATER) {
            return WATER.defaultInstance();
        }

        return POTION.toInstance(PotionContents.EMPTY.withPotion(potion));
    }
}
