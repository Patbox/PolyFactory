package eu.pb4.polyfactory.other;

import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryRegistries {
    ResourceKey<Registry<FluidType<?>>> FLUID_TYPES_KEY = ResourceKey.createRegistryKey(id("fluid_type"));
    Registry<FluidType<?>> FLUID_TYPES = FabricRegistryBuilder.createSimple(FLUID_TYPES_KEY).attribute(RegistryAttribute.OPTIONAL).buildAndRegister();

    static void setup() {
        PolymerUtils.markAsServerOnlyRegistry(FLUID_TYPES_KEY);
    }
}
