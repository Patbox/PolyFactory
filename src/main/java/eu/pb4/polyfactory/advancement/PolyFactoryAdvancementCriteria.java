package eu.pb4.polyfactory.advancement;

import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import static eu.pb4.polyfactory.ModInit.id;

public interface PolyFactoryAdvancementCriteria {
    FluidShootsCriterion NOZZLE_SHOOTS = register("nozzle_shoots", new FluidShootsCriterion());
    FluidShootsCriterion FLUID_LAUNCHER_SHOOTS = register("fluid_launcher_shoots", new FluidShootsCriterion());

    static <T extends Criterion<?>> T register(String path, T criterion) {
        return Registry.register(Registries.CRITERION, id(path), criterion);
    }

    static void register() {}
}
