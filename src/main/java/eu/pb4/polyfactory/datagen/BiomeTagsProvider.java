package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.entity.FactoryEntityTags;
import eu.pb4.polyfactory.other.FactoryBiomeTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import java.util.concurrent.CompletableFuture;

class BiomeTagsProvider extends FabricTagProvider<Biome> {
    public BiomeTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, Registries.BIOME, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.builder(FactoryBiomeTags.WINDMILL_HIGH_SPEED_BONUS)
                .addOptionalTag(ConventionalBiomeTags.IS_OCEAN);
        ;

        this.builder(FactoryBiomeTags.WINDMILL_MIDDLE_SPEED_BONUS)
                .addOptionalTag(ConventionalBiomeTags.IS_BEACH)
                .addOptionalTag(ConventionalBiomeTags.IS_MOUNTAIN)
        ;

        this.builder(FactoryBiomeTags.WINDMILL_LOW_SPEED_BONUS)
                .addOptionalTag(ConventionalBiomeTags.IS_OLD_GROWTH)
        ;

        this.builder(FactoryBiomeTags.TEMPERATURE_REALLY_HOT)
                .addOptionalTag(ConventionalBiomeTags.IS_NETHER)
        ;

        this.builder(FactoryBiomeTags.TEMPERATURE_HOT)
                .addOptionalTag(ConventionalBiomeTags.IS_DESERT)
                .addOptionalTag(ConventionalBiomeTags.IS_BADLANDS)
                .addOptionalTag(ConventionalBiomeTags.IS_SAVANNA)
        ;

        this.builder(FactoryBiomeTags.TEMPERATURE_COLD)
                .addOptionalTag(ConventionalBiomeTags.IS_SNOWY)
        ;

        this.builder(FactoryBiomeTags.TEMPERATURE_REALLY_COLD)
                .addOptionalTag(ConventionalBiomeTags.IS_ICY)
        ;
    }
}
