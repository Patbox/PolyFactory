package eu.pb4.polyfactory.util;

import net.minecraft.sound.SoundEvent;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactorySoundEvents {
    public static final SoundEvent ENTITY_DYNAMITE_THROW = of("entity.dynamite.throw");
    public static final SoundEvent BLOCK_PRESS_CRAFT = of("block.press.craft");

    private static SoundEvent of(String path) {
        return SoundEvent.of(id(path));
    }
}
