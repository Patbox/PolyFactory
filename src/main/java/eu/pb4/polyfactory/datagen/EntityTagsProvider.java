package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.util.FactoryEntityTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EntityTypeTags;

import java.util.concurrent.CompletableFuture;

class EntityTagsProvider extends FabricTagProvider.EntityTypeTagProvider {
    public EntityTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        this.getOrCreateTagBuilder(FactoryEntityTags.GRID_PASSABLE)
                .addOptionalTag(EntityTypeTags.ARROWS)
                .addOptionalTag(EntityTypeTags.IMPACT_PROJECTILES)
                .add(EntityType.ITEM)
                .add(EntityType.EXPERIENCE_ORB)
                .add(EntityType.EXPERIENCE_BOTTLE)
                .add(EntityType.POTION)
        ;
    }
}
