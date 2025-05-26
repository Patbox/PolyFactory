package eu.pb4.polyfactory.other;

import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public interface FactorySoundEvents {
    SoundEvent ENTITY_DYNAMITE_THROW = of("entity.dynamite.throw");
    SoundEvent BLOCK_PRESS_CRAFT = of("block.press.craft");
    SoundEvent BLOCK_REMOTE_REDSTONE_ON = of("block.remote_redstone.on");
    SoundEvent BLOCK_REMOTE_REDSTONE_OFF = of("block.remote_redstone.off");
    SoundEvent BLOCK_EJECTOR_LAUNCH = of("block.ejector.launch");
    SoundEvent ITEM_WRENCH_USE = of("item.wrench.use");
    SoundEvent ITEM_WRENCH_SWITCH = of("item.wrench.switch");
    SoundEvent ITEM_BUCKET_EMPTY_SLIME = of("item.bucket.empty_slime");
    SoundEvent ITEM_BUCKET_FILL_SLIME = of("item.bucket.fill_slime");
    SoundEvent ITEM_BUCKET_EMPTY_HONEY = of("item.bucket.empty_honey");
    SoundEvent ITEM_BUCKET_FILL_HONEY = of("item.bucket.fill_honey");
    RegistryEntry<SoundEvent> FLUID_SHOOT_WATER = ofEntry("fluid.shoot.water");
    RegistryEntry<SoundEvent> FLUID_SHOOT_MILK = ofEntry("fluid.shoot.milk");
    RegistryEntry<SoundEvent> FLUID_SHOOT_POTION = ofEntry("fluid.shoot.potion");
    RegistryEntry<SoundEvent> FLUID_SHOOT_SLIME = ofEntry("fluid.shoot.slime");
    RegistryEntry<SoundEvent> FLUID_SHOOT_HONEY = ofEntry("fluid.shoot.honey");
    RegistryEntry<SoundEvent> FLUID_SHOOT_LAVA = ofEntry("fluid.shoot.lava");
    RegistryEntry<SoundEvent> FLUID_SHOOT_EXPERIENCE = ofEntry("fluid.shoot.experience");
    SoundEvent ITEM_CLIPBOARD_WRITE = of("item.clipboard.write");
    SoundEvent ITEM_CLIPBOARD_APPLY = of("item.clipboard.apply");
    SoundEvent BLOCK_SPOUT_METAL_COOLED = of("block.spout.metal_cooled");

    static SoundEvent of(String path) {
        var obj = SoundEvent.of(id(path));
        PolymerSoundEvent.registerOverlay(obj);
        return Registry.register(Registries.SOUND_EVENT, obj.id(), obj);
    }
    static RegistryEntry<SoundEvent> ofEntry(String path) {
        var obj = SoundEvent.of(id(path));
        PolymerSoundEvent.registerOverlay(obj);
        return Registry.registerReference(Registries.SOUND_EVENT, obj.id(), obj);
    }
}
