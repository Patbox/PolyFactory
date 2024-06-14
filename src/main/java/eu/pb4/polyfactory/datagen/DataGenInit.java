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
        pack.addProvider(LootTables::new);
        pack.addProvider(EnchantmentProvider::new);
        pack.addProvider(EnchantmentTagProvider::new);
        pack.addProvider(RecipesProvider::new);
        pack.addProvider(AdvancementsProvider::new);
        pack.addProvider(AssetProvider::new);
    }
}
