package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.fluid.shooting.ShootProjectileEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class FactoryFluids {
    public static final FluidType<Unit> WATER = register(Identifier.ofVanilla("water"),
            FluidType.of().density(100).fluid(Fluids.WATER).color(0x385dc6)
                    .particle(new ItemStackParticleEffect(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.getDefaultStack()))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.WATER_SPLASH, 10,300, FactorySoundEvents.FLUID_SHOOT_WATER))
                    .build());
    public static final FluidType<Unit> LAVA = register(Identifier.ofVanilla("lava"),
            FluidType.of().density(1000).fluid(Fluids.LAVA).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.getDimension().ultrawarm() ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType<Unit> MILK = register(Identifier.ofVanilla("milk"),
            FluidType.of().density(200).flowSpeedMultiplier(0.95)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.MILK_SPLASH, 10,350, FactorySoundEvents.FLUID_SHOOT_MILK))
                    .build());

    public static final FluidType<Unit> IRON = register(Identifier.ofVanilla("iron"),
            FluidType.of().density(1005).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.getDimension().ultrawarm() ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());

    public static final FluidType<Unit> GOLD = register(Identifier.ofVanilla("gold"),
            FluidType.of().density(1010).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.getDimension().ultrawarm() ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());

    public static final FluidType<Unit> COPPER = register(Identifier.ofVanilla("copper"),
            FluidType.of().density(1000).brightness(15).heat(BlockHeat.LAVA)
                    .flowSpeedMultiplier(((world, data) -> world != null && world.getDimension().ultrawarm() ? 1 : 0.5))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.LAVA_SPLASH, 10,400, FactorySoundEvents.FLUID_SHOOT_LAVA))
                    .maxFlow(((world, data) -> world != null && world.getDimension().ultrawarm() ? FluidConstants.BOTTLE : FluidConstants.BOTTLE * 2 / 3)).build());
    public static final FluidType<Unit> EXPERIENCE = register(Identifier.ofVanilla("experience"),
            FluidType.of().density(50).flowSpeedMultiplier(1.3).maxFlow(FluidConstants.BOTTLE * 2).brightness(14).heat(BlockHeat.EXPERIENCE)
                    .shootingBehavior(new ShootProjectileEntity<>((world, fluid, amount) -> {
                        var xp = FactoryEntities.EXPERIENCE_SPLASH.create(world, SpawnReason.SPAWN_ITEM_USE);
                        assert xp != null;
                        xp.setAmount(amount / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID);
                        return xp;
                    }, 1,
                            (world, c, f, test) -> test
                                    ? FluidBehaviours.EXPERIENCE_ORB_TO_FLUID
                                    : MathHelper.clamp(c.get(f) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, 1, 3) * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID,
                             2, 0.5f, 0.1f, 0.3f, FactorySoundEvents.FLUID_SHOOT_EXPERIENCE))
                    .build());
    public static final FluidType<PotionContentsComponent> POTION = register(Identifier.ofVanilla("potion"),
            FluidType.of(PotionContentsComponent.CODEC, PotionContentsComponent.DEFAULT).density(150).texture(WATER.texture())
                            .color(PotionContentsComponent::getColor).flowSpeedMultiplier(0.95).name((t, d) -> {
                        var base = d.getName("item.minecraft.potion.effect.");
                        if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("long_")) {
                            return Text.translatable("fluid_type.minecraft.potion.long", base);
                        }
                        if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("strong_")) {
                            return Text.translatable("fluid_type.minecraft.potion.strong", base);
                        }
                        return base;
                    }).particle((data) -> EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, data.data().getColor()))
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.POTION_SPLASH, 4, FluidConstants.BOTTLE / 60, FactorySoundEvents.FLUID_SHOOT_POTION))
                    .build());

    public static final FluidType<Unit> HONEY = register(Identifier.ofVanilla("honey"),
            FluidType.of().density(500).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.HONEY_SPLASH, 3, FluidConstants.BOTTLE / 80, FactorySoundEvents.FLUID_SHOOT_HONEY))
                    .build());

    public static final FluidType<Unit> SLIME = register(Identifier.ofVanilla("slime"),
            FluidType.of().density(600).transparent().flowSpeedMultiplier(0.6).maxFlow(FluidConstants.BOTTLE * 2 / 3)
                    .shootingBehavior(ShootProjectileEntity.ofSplash(FactoryEntities.SLIME_SPLASH, 3, FluidConstants.BOTTLE / 80, FactorySoundEvents.FLUID_SHOOT_SLIME))
                    .build());

    public static final FluidType<Unit> SNOW = register(Identifier.ofVanilla("snow"),
            FluidType.of().density(90).flowSpeedMultiplier(0.98).maxFlow(FluidConstants.BOTTLE * 4 / 5).heat(BlockHeat.SNOW)
                    .shootingBehavior(ShootProjectileEntity.ofEntity(EntityType.SNOWBALL, 1, FluidConstants.BLOCK / 4 / 20 / 2,
                            1.7f, 0.5f, 0, 0.1f, Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_SNOWBALL_THROW)))
                    .texture(Identifier.ofVanilla("block/powder_snow")).build());
    public static void register() {
        FluidBehaviours.addBlockStateConversions(Blocks.WATER.getDefaultState(), Blocks.AIR.getDefaultState(), WATER.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW.getDefaultState(), Blocks.AIR.getDefaultState(), SNOW.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA.getDefaultState(), Blocks.AIR.getDefaultState(), LAVA.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.getDefaultState(), WATER.ofBottle());

        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3),
                Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2), SNOW.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2),
                Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1), SNOW.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.getDefaultState(), SNOW.ofBottle());


        FluidBehaviours.addBlockStateConversions(Blocks.LAVA_CAULDRON.getDefaultState(), Blocks.CAULDRON.getDefaultState(), LAVA.ofBucket());

        FluidBehaviours.addBlockStateInsert(Blocks.SLIME_BLOCK.getDefaultState(), Blocks.AIR.getDefaultState(), SLIME.ofBucket());

        for (var dir : BeehiveBlock.FACING.getValues()) {
            FluidBehaviours.addBlockStateExtract(Blocks.BEEHIVE.getDefaultState().with(BeehiveBlock.FACING, dir).with(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.FULL_HONEY_LEVEL),
                    Blocks.BEEHIVE.getDefaultState().with(BeehiveBlock.FACING, dir), HONEY.of(FluidConstants.BLOCK / 4));
        }


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

    public static FluidInstance<?> getPotion(RegistryEntry<Potion> potion) {
        if (potion == Potions.WATER) {
            return WATER.defaultInstance();
        }

        return POTION.toInstance(PotionContentsComponent.DEFAULT.with(potion));
    }
}
