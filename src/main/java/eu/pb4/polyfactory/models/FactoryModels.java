package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.models.fluid.FluidModel;
import net.minecraft.util.Util;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryModels {
    FluidModel FLAT_FULL = new FluidModel(id("block/fluid/flat_model"));

    FluidModel[] FLAT_SCALED = Util.make(new FluidModel[16], (arr) -> {
       for (int i = 0; i < 15; i++) {
           arr[i] = new FluidModel(id("block/fluid/flat_scaled/" + (i + 1)));
       }
       arr[15] = FLAT_FULL;
    });

    DirectionConnectingModel COLORED_CABLE = new DirectionConnectingModel(id("block/cable_colored"), true);
    DirectionConnectingModel COLORED_WALL_CABLE = new DirectionConnectingModel(id("block/cable_colored_wall"), true);
    DirectionConnectingModel PIPE = new DirectionConnectingModel(id("block/pipe"), false);
    MultiElementConnectedModel FLUID_TANK = new MultiElementConnectedModel(id("block/fluid_tank"), id("block/fluids/tank/side"),
            id("block/fluids/tank/bottom"), MultiElementConnectedModel.WITH_INNER);

    static void load() {
    }
}
