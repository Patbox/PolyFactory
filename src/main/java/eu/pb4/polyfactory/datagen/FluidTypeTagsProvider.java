package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.fluid.FactoryFluidTags;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

class FluidTypeTagsProvider extends FabricTagProvider<FluidType<?>> {
    public FluidTypeTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, FactoryRegistries.FLUID_TYPES_KEY, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        this.getOrCreateTagBuilder(FactoryFluidTags.MOLTEN_METAL)
                .add(FactoryFluids.IRON)
                .add(FactoryFluids.GOLD)
                .add(FactoryFluids.COPPER)
                .add(FactoryFluids.STEEL)
        ;

        this.getOrCreateTagBuilder(FactoryFluidTags.USE_INGOTS_FOR_AMOUNT)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL);

        this.getOrCreateTagBuilder(FactoryFluidTags.SMELTERY_ALLOWED_FLUIDS)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL)
        ;
    }
}
