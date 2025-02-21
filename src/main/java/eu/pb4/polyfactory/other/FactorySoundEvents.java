package eu.pb4.polyfactory.other;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public interface FactorySoundEvents {
    SoundEvent ENTITY_DYNAMITE_THROW = of("entity.dynamite.throw");
    SoundEvent BLOCK_PRESS_CRAFT = of("block.press.craft");
    SoundEvent BLOCK_REMOTE_REDSTONE_ON = of("block.remote_redstone.on");
    SoundEvent BLOCK_REMOTE_REDSTONE_OFF = of("block.remote_redstone.off");
    SoundEvent ITEM_WRENCH_USE = of("item.wrench.use");
    SoundEvent ITEM_WRENCH_SWITCH = of("item.wrench.switch");
    SoundEvent ITEM_BUCKET_EMPTY_SLIME = of("item.bucket.empty_slime");
    SoundEvent ITEM_BUCKET_FILL_SLIME = of("item.bucket.fill_slime");
    SoundEvent ITEM_BUCKET_EMPTY_HONEY = of("item.bucket.empty_slime");
    SoundEvent ITEM_BUCKET_FILL_HONEY = of("item.bucket.fill_slime");
    RegistryEntry<SoundEvent> FLUID_SHOOT_WATER = ofEntry("fluid.shoot.water");
    RegistryEntry<SoundEvent> FLUID_SHOOT_MILK = ofEntry("fluid.shoot.milk");
    RegistryEntry<SoundEvent> FLUID_SHOOT_POTION = ofEntry("fluid.shoot.potion");
    RegistryEntry<SoundEvent> FLUID_SHOOT_SLIME = ofEntry("fluid.shoot.slime");
    RegistryEntry<SoundEvent> FLUID_SHOOT_HONEY = ofEntry("fluid.shoot.honey");
    RegistryEntry<SoundEvent> FLUID_SHOOT_LAVA = ofEntry("fluid.shoot.lava");
    RegistryEntry<SoundEvent> FLUID_SHOOT_EXPERIENCE = ofEntry("fluid.shoot.experience");

    static SoundEvent of(String path) {
        return SoundEvent.of(id(path));
    }
    static RegistryEntry<SoundEvent> ofEntry(String path) {
        return RegistryEntry.of(of(path));
    }
}
