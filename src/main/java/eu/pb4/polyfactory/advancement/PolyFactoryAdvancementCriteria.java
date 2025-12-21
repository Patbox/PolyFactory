package eu.pb4.polyfactory.advancement;

import static eu.pb4.polyfactory.ModInit.id;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public interface PolyFactoryAdvancementCriteria {
    FluidShootsCriterion NOZZLE_SHOOTS = register("nozzle_shoots", new FluidShootsCriterion());
    FluidShootsCriterion FLUID_LAUNCHER_SHOOTS = register("fluid_launcher_shoots", new FluidShootsCriterion());

    static <T extends CriterionTrigger<?>> T register(String path, T criterion) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES, id(path), criterion);
    }

    static void register() {}
}
