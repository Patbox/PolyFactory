package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
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
    public static final FluidType<Unit> WATER = register(Identifier.ofVanilla("water"), FluidType.of(100, Fluids.WATER, 0x3b3bed));
    public static final FluidType<Unit> LAVA = register(Identifier.ofVanilla("lava"), FluidType.of(1000, Fluids.LAVA));
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType<Unit> MILK = register(Identifier.ofVanilla("milk"), FluidType.of(200));
    public static final FluidType<Unit> XP = register(Identifier.ofVanilla("xp"), FluidType.of(50));
    public static final FluidType<PotionContentsComponent> POTION = register(Identifier.ofVanilla("potion"), FluidType.of(50, PotionContentsComponent.CODEC,
            PotionContentsComponent.DEFAULT, WATER.texture(), PotionContentsComponent::getColor,
            (t, d) -> {
                var base = Text.translatable(Potion.finishTranslationKey(d.potion(), "item.minecraft.potion.effect."));
                if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("long_")) {
                    return Text.translatable("fluid_type.minecraft.potion.long", base);
                }
                if (d.potion().isPresent() && d.potion().get().getKey().get().getValue().getPath().startsWith("strong_")) {
                    return Text.translatable("fluid_type.minecraft.potion.strong", base);
                }
                return base;
            }));


    public static void register() {
        FluidBehaviours.addStaticRelation(Items.WATER_BUCKET, Items.BUCKET, WATER.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);
        FluidBehaviours.addStaticRelation(Items.LAVA_BUCKET, Items.BUCKET, LAVA.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY_LAVA, SoundEvents.ITEM_BUCKET_FILL_LAVA);
        FluidBehaviours.addStaticRelation(Items.MILK_BUCKET, Items.BUCKET, MILK.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);
        FluidBehaviours.addStaticRelation(FactoryItems.EXPERIENCE_BUCKET, Items.BUCKET, XP.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);

        FluidBehaviours.addStaticRelation(Items.POTION,
                ComponentPredicate.of(ComponentMap.builder().add(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(Potions.WATER)).build()),
                Items.GLASS_BOTTLE, WATER.ofBottle(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundEvents.ITEM_BOTTLE_FILL);

        for (var potion : Registries.POTION.getIndexedEntries()) {
            if (potion == Potions.WATER) {
                continue;
            }
            FluidBehaviours.addStaticRelation(Items.POTION,
                    ComponentPredicate.of(ComponentMap.builder().add(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(potion)).build()),
                    Items.GLASS_BOTTLE, POTION.ofBottle(PotionContentsComponent.DEFAULT.with(potion)), SoundEvents.ITEM_BOTTLE_EMPTY, SoundEvents.ITEM_BOTTLE_FILL);
        }


        FluidBehaviours.addBlockStateConversions(Blocks.WATER.getDefaultState(), Blocks.AIR.getDefaultState(), WATER.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA.getDefaultState(), Blocks.AIR.getDefaultState(), LAVA.ofBucket());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 2),
                Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 1),
                Blocks.CAULDRON.getDefaultState(), WATER.ofBottle());
        FluidBehaviours.addBlockStateConversions(Blocks.LAVA_CAULDRON.getDefaultState(), Blocks.CAULDRON.getDefaultState(), LAVA.ofBucket());
        if (ModInit.DEV_MODE) {
            FluidBehaviours.addStaticRelation(Items.EXPERIENCE_BOTTLE, Items.GLASS_BOTTLE, XP.ofBottle(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundEvents.ITEM_BOTTLE_FILL);
        }
    }

    public static <T> FluidType<T> register(Identifier identifier, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, identifier, item);
    }

    public static <T> FluidType<T> register(String path, FluidType<T> item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.of(ModInit.ID, path), item);
    }
}
