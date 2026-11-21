package eu.pb4.polyfactory.mixin.block;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.other.DynamicContent;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public class BuiltinRegistriesMixin {
    @Shadow
    @Final
    public static DefaultedRegistry<Fluid> FLUID;

    @Inject(method = "freeze", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;", ordinal = 0))
    private static void autoFluidInject(CallbackInfo ci) {
        for (var fluid : FLUID) {
            var key = FLUID.getKey(fluid);
            if (!fluid.isSource(fluid.defaultFluidState()) || FluidType.get(fluid) != null || FactoryRegistries.FLUID_TYPES.containsKey(key)) {
                continue;
            }
            var block = fluid.defaultFluidState().createLegacyBlock();
            var variant = FluidVariant.of(fluid);
            var name = block.isAir()
                    ? Component.translatable(key.toLanguageKey("block"))
                    : block.getBlock().getName();

            var type = Registry.register(FactoryRegistries.FLUID_TYPES, key, FluidType.of()
                    .fluid(fluid)
                    .name((_, _) -> name)
                    .brightness(FluidVariantAttributes.getLuminance(variant))
                    .density(FluidVariantAttributes.getViscosity(variant, null) / 10)
                    .maxFlow((_, _) -> FluidConstants.BOTTLE * FluidVariantAttributes.getViscosity(variant, null) / FluidConstants.WATER_VISCOSITY)
                    .soundEvents(FluidVariantAttributes.getFillSound(variant), FluidVariantAttributes.getEmptySound(variant))
                    .build());

            if (fluid.getBucket() != Items.AIR) {
                DynamicContent.GENERATE_FLUID_BUCKET_RECIPES.add(new Pair<>(type.defaultInstance(), fluid.getBucket()));
                FluidBehaviours.addItemToFluidLink(fluid.getBucket(), type.defaultInstance());
            }

            if (!block.isAir()) {
                FluidBehaviours.addBlockStateConversions(block, Blocks.AIR.defaultBlockState(), type.ofBucket());
            }
        }
    }
}
