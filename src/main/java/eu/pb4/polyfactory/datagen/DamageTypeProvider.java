package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.other.FactoryDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DamageTypeProvider extends FabricCodecDataProvider<DamageType> {
    protected DamageTypeProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(dataOutput, registriesFuture, Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, DamageType> provider, HolderLookup.Provider lookup) {
        provider.accept(FactoryDamageTypes.EXPERIENCE_SPLASH.identifier(), new DamageType("polyfactory.experience_bolt", 0.1f));

    }

    @Override
    public String getName() {
        return "damage_type";
    }

    public static class Tags extends FabricTagProvider<DamageType> {
        public Tags(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, Registries.DAMAGE_TYPE, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider wrapperLookup) {
            this.builder(DamageTypeTags.BYPASSES_COOLDOWN)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.BYPASSES_ARMOR)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.BYPASSES_SHIELD)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.IS_PROJECTILE)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.PANIC_CAUSES)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);
            this.builder(DamageTypeTags.BYPASSES_EFFECTS)
                    .addOptional(FactoryDamageTypes.EXPERIENCE_SPLASH);

        }
    }
}
