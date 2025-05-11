package eu.pb4.polyfactory.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class DataGenInit implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        var blockTags = pack.addProvider(BlockTagsProvider::new);
        pack.addProvider((a, b) -> new ItemTagsProvider(a, b, blockTags));
        pack.addProvider(EntityTagsProvider::new);
        pack.addProvider(BiomeTagsProvider::new);
        pack.addProvider(FluidTypeTagsProvider::new);
        pack.addProvider(LootTables::new);
        pack.addProvider(EnchantmentProvider::new);
        pack.addProvider(EnchantmentProvider.Tags::new);
        pack.addProvider(DamageTypeProvider::new);
        pack.addProvider(DamageTypeProvider.Tags::new);
        pack.addProvider(RecipesProvider::new);
        pack.addProvider(AdvancementsProvider::new);
        pack.addProvider(AssetProvider::new);
    }
}
