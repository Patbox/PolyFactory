package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.other.FactoryRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryFluidIds {
    public static final ResourceKey<FluidType<?>> WATER = ofVanilla("water");
    public static final ResourceKey<FluidType<?>> LAVA = ofVanilla("lava");

    public static final ResourceKey<FluidType<?>> MILK = ofVanilla("milk");

    public static final ResourceKey<FluidType<?>> GLASS = ofVanilla("glass");

    public static final ResourceKey<FluidType<?>> IRON = ofVanilla("iron");

    public static final ResourceKey<FluidType<?>> STEEL = of("steel");

    public static final ResourceKey<FluidType<?>> GOLD = ofVanilla("gold");

    public static final ResourceKey<FluidType<?>> COPPER = ofVanilla("copper");

    public static final ResourceKey<FluidType<?>> EXPERIENCE = ofVanilla("experience");

    public static final ResourceKey<FluidType<?>> POTION = ofVanilla("potion");

    public static final ResourceKey<FluidType<?>> HONEY = ofVanilla("honey");

    public static final ResourceKey<FluidType<?>> SLIME = ofVanilla("slime");

    public static final ResourceKey<FluidType<?>> SNOW = ofVanilla("snow");

    public static final ResourceKey<FluidType<?>> PLANT_OIL = of("plant_oil");

    public static final ResourceKey<FluidType<?>> BIODIESEL = of("biodiesel");


    public static ResourceKey<FluidType<?>> of(Identifier identifier) {
        return ResourceKey.create(FactoryRegistries.FLUID_TYPES_KEY, identifier);
    }

    public static ResourceKey<FluidType<?>> of(String path) {
        return of(id(path));
    }
    public static ResourceKey<FluidType<?>> ofVanilla(String path) {
        return of(Identifier.withDefaultNamespace(path));
    }
}
