package eu.pb4.polyfactory.models;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryModels {
    FluidModel FLAT_FULL = new FluidModel(id("block/fluid/flat_model"));
    DirectionConnectingModel COLORED_CABLE = new DirectionConnectingModel(id("block/cable_colored"), true);
    DirectionConnectingModel COLORED_WALL_CABLE = new DirectionConnectingModel(id("block/cable_colored_wall"), true);
    DirectionConnectingModel PIPE = new DirectionConnectingModel(id("block/pipe"), false);

    static void load() {
    }
}
