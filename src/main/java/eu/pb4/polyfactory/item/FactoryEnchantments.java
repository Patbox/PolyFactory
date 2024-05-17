package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.item.enchantment.PolymericEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class FactoryEnchantments {
    public static final RegistryEntry<Enchantment> IGNORE_MOVEMENT = register("ignore_movement", new PolymericEnchantment());
    public static void register() {

    }


    public static <T extends Enchantment> RegistryEntry<T> register(String path, T item) {
        return (RegistryEntry<T>) Registry.registerReference(Registries.ENCHANTMENT, new Identifier(ModInit.ID, path), item);
    }
}
