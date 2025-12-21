package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.entity.FactoryEntityTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import java.util.concurrent.CompletableFuture;

class EntityTagsProvider extends FabricTagProvider.EntityTypeTagProvider {
    public EntityTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.valueLookupBuilder(FactoryEntityTags.GRID_PASSABLE)
                .addOptionalTag(EntityTypeTags.ARROWS)
                .addOptionalTag(EntityTypeTags.IMPACT_PROJECTILES)
                .add(EntityType.ITEM)
                .add(EntityType.EXPERIENCE_ORB)
                .add(EntityType.EXPERIENCE_BOTTLE)
                .add(EntityType.LINGERING_POTION)
                .add(EntityType.SPLASH_POTION)
        ;
    }
}
