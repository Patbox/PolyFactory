package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.other.FactoryRegistries;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class FactoryFluidTags {
    public static final TagKey<FluidType<?>> MOLTEN_METAL = of("molten_metal");
    public static final TagKey<FluidType<?>> SMELTERY_ALLOWED_FLUIDS = of("smeltery/allowed_fluids");
    public static final TagKey<FluidType<?>> USE_INGOTS_FOR_AMOUNT = of("use_ingots_for_amount");

    private static TagKey<FluidType<?>> of(String path) {
        return TagKey.of(FactoryRegistries.FLUID_TYPES_KEY, ModInit.id(path));
    }
}
