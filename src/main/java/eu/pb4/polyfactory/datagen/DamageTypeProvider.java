package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.other.FactoryDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DamageTypeProvider extends FabricCodecDataProvider<DamageType> {
    protected DamageTypeProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(dataOutput, registriesFuture, RegistryKeys.DAMAGE_TYPE, DamageType.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, DamageType> provider, RegistryWrapper.WrapperLookup lookup) {
        provider.accept(FactoryDamageTypes.EXPERIENCE_SPLASH.getValue(), new DamageType("polyfactory.experience_bolt", 0.1f));

    }

    @Override
    public String getName() {
        return "damage_type";
    }

    public static class Tags extends FabricTagProvider<DamageType> {
        public Tags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, RegistryKeys.DAMAGE_TYPE, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
            this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_COOLDOWN)
                    .addOptionalTag(FactoryDamageTypes.EXPERIENCE_SPLASH.getValue());
            this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR)
                    .addOptionalTag(FactoryDamageTypes.EXPERIENCE_SPLASH.getValue());
            this.getOrCreateTagBuilder(DamageTypeTags.BYPASSES_SHIELD)
                    .addOptionalTag(FactoryDamageTypes.EXPERIENCE_SPLASH.getValue());
            this.getOrCreateTagBuilder(DamageTypeTags.IS_PROJECTILE)
                    .addOptionalTag(FactoryDamageTypes.EXPERIENCE_SPLASH.getValue());
        }
    }
}
