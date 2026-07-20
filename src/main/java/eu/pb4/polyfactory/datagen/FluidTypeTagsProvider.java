package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.fluid.FactoryFluidTags;
import eu.pb4.polyfactory.fluid.FactoryFluidIds;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

class FluidTypeTagsProvider extends FabricTagsProvider<FluidType<?>> {
    public FluidTypeTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, FactoryRegistries.FLUID_TYPES_KEY, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.tag(FactoryFluidTags.MOLTEN_METAL)
                .add(FactoryFluidIds.IRON)
                .add(FactoryFluidIds.GOLD)
                .add(FactoryFluidIds.COPPER)
                .add(FactoryFluidIds.STEEL)
        ;

        this.tag(FactoryFluidTags.USE_INGOTS_FOR_AMOUNT)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL);

        this.tag(FactoryFluidTags.SMELTERY_ALLOWED_FLUIDS)
                .addOptionalTag(FactoryFluidTags.MOLTEN_METAL)
                .add(FactoryFluidIds.GLASS)
        ;

        this.tag(FactoryFluidTags.DRILL_FUEL)
                .add(FactoryFluidIds.BIODIESEL);
    }
}
