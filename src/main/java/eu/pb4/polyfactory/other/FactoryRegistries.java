package eu.pb4.polyfactory.other;

import eu.pb4.polyfactory.fluid.FluidType;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryRegistries {
    RegistryKey<Registry<FluidType<?>>> FLUID_TYPES_KEY = RegistryKey.ofRegistry(id("fluid_type"));
    Registry<FluidType<?>> FLUID_TYPES = FabricRegistryBuilder.createSimple(FLUID_TYPES_KEY).buildAndRegister();

    static void setup() {
    }
}
