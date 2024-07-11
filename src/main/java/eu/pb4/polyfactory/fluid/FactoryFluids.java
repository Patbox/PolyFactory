package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class FactoryFluids {
    public static final FluidType WATER = register(Identifier.ofVanilla("water"), FluidType.of(100, Fluids.WATER, 0x3b3bed));
    public static final FluidType LAVA = register(Identifier.ofVanilla("lava"), FluidType.of(1000, Fluids.LAVA));
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final FluidType MILK = register(Identifier.ofVanilla("milk"), FluidType.of(200));
    public static final FluidType XP = register(Identifier.ofVanilla("xp"), FluidType.of(50));


    public static void register() {
        FluidItemBehaviours.addStaticRelation(Items.WATER_BUCKET, Items.BUCKET, WATER.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);
        FluidItemBehaviours.addStaticRelation(Items.LAVA_BUCKET, Items.BUCKET, LAVA.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY_LAVA, SoundEvents.ITEM_BUCKET_FILL_LAVA);
        FluidItemBehaviours.addStaticRelation(Items.MILK_BUCKET, Items.BUCKET, MILK.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);
        FluidItemBehaviours.addStaticRelation(FactoryItems.EXPERIENCE_BUCKET, Items.BUCKET, XP.ofBucket(), SoundEvents.ITEM_BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_FILL);

        FluidItemBehaviours.addStaticRelation(Items.POTION,
                ComponentPredicate.of(ComponentMap.builder().add(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(Potions.WATER)).build()),
                Items.GLASS_BOTTLE, WATER.ofBottle(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundEvents.ITEM_BOTTLE_FILL);
        if (ModInit.DEV_MODE) {
            FluidItemBehaviours.addStaticRelation(Items.EXPERIENCE_BOTTLE, Items.GLASS_BOTTLE, XP.ofBottle(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundEvents.ITEM_BOTTLE_FILL);
        }
    }

    public static FluidType register(Identifier identifier, FluidType item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, identifier, item);
    }
    public static FluidType register(String path, FluidType item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.of(ModInit.ID, path), item);
    }
}
