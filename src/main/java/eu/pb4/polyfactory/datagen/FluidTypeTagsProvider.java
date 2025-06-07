package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.fluid.FactoryFluidTags;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

class FluidTypeTagsProvider extends FabricTagProvider.FabricValueLookupTagProvider<FluidType<?>> {
    public FluidTypeTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, FactoryRegistries.FLUID_TYPES_KEY, completableFuture, x -> FactoryRegistries.FLUID_TYPES.getKey(x).orElseThrow());
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        this.valueLookupBuilder(FactoryFluidTags.MOLTEN_METAL)
                .add(FactoryFluids.IRON)
                .add(FactoryFluids.GOLD)
                .add(FactoryFluids.COPPER)
                .add(FactoryFluids.STEEL)
        ;

        this.valueLookupBuilder(FactoryFluidTags.USE_INGOTS_FOR_AMOUNT)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL);

        this.valueLookupBuilder(FactoryFluidTags.SMELTERY_ALLOWED_FLUIDS)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL)
                .add(FactoryFluids.GLASS)
        ;
    }
}
