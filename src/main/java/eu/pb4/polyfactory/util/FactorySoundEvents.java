package eu.pb4.polyfactory.util;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactorySoundEvents {
    public static final SoundEvent ENTITY_DYNAMITE_THROW = of("entity.dynamite.throw");
    public static final SoundEvent BLOCK_PRESS_CRAFT = of("block.press.craft");
    public static final SoundEvent BLOCK_REMOTE_REDSTONE_ON = of("block.remote_redstone.on");
    public static final SoundEvent BLOCK_REMOTE_REDSTONE_OFF = of("block.remote_redstone.off");
    public static final SoundEvent ITEM_WRENCH_USE = of("item.wrench.use");
    public static final SoundEvent ITEM_WRENCH_SWITCH = of("item.wrench.switch");
    public static final SoundEvent ITEM_BUCKET_EMPTY_SLIME = of("item.bucket.empty_slime");
    public static final SoundEvent ITEM_BUCKET_FILL_SLIME = of("item.bucket.fill_slime");
    public static final SoundEvent ITEM_BUCKET_EMPTY_HONEY = of("item.bucket.empty_slime");
    public static final SoundEvent ITEM_BUCKET_FILL_HONEY = of("item.bucket.fill_slime");
    public static final RegistryEntry<SoundEvent> ITEM_FLUID_LAUNCHER_SHOOT_WATER = ofEntry("item.fluid_launcher.shoot_water");
    public static final RegistryEntry<SoundEvent> ITEM_FLUID_LAUNCHER_SHOOT_LAVA = ofEntry("item.fluid_launcher.shoot_lava");

    private static SoundEvent of(String path) {
        return SoundEvent.of(id(path));
    }
    private static RegistryEntry<SoundEvent> ofEntry(String path) {
        return RegistryEntry.of(of(path));
    }
}
