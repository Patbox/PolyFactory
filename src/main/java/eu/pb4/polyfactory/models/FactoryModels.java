package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.models.fluid.FluidModel;
import eu.pb4.polyfactory.util.ModelRenderType;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import static eu.pb4.polyfactory.ModInit.id;

public interface FactoryModels {
    Identifier PLACEHOLDER = id("placeholder");
    FluidModel FLUID_FLAT_FULL = new FluidModel(id("block/fluid/flat_model"));
    FluidModel FLUID_PARTICLE = new FluidModel(id("particle/fluid/particle"));

    FluidModel[] FLUID_FLAT_SCALED = Util.make(new FluidModel[16], (arr) -> {
       for (int i = 0; i < 15; i++) {
           arr[i] = new FluidModel(id("block/fluid/flat_scaled/" + (i + 1)));
       }
       arr[15] = FLUID_FLAT_FULL;
    });

    FluidModel FLUID_FILTERED_PIPE = new FluidModel(id("block/fluid/filtered_pipe"));
    FluidModel[] FLUID_PORTABLE_FLUID_TANK_VERTICAL = Util.make(new FluidModel[16], (arr) -> {
        for (int i = 0; i < 16; i++) {
            arr[i] = new FluidModel(id("block/fluid/portable_fluid_tank/v" + (i + 1)));
        }
    });

    FluidModel FLUID_PORTABLE_FLUID_TANK_ITEM = new FluidModel(id("block/fluid/portable_fluid_tank/item"));

    FluidModel[] FLUID_PORTABLE_FLUID_TANK_HORIZONTAL = Util.make(new FluidModel[12], (arr) -> {
        for (int i = 0; i < 12; i++) {
            arr[i] = new FluidModel(id("block/fluid/portable_fluid_tank/h" + (i + 1)));
        }
    });
    DirectionConnectingModel COLORED_CABLE = new DirectionConnectingModel(id("block/cable_colored"), true);
    DirectionConnectingModel COLORED_WALL_CABLE = new DirectionConnectingModel(id("block/cable_colored_wall"), true);
    DirectionConnectingModel PIPE = new DirectionConnectingModel(id("block/pipe"), false);
    MultiElementConnectedModel BLOCK_FLUID_TANK = new MultiElementConnectedModel(id("block/fluid_tank"), id("block/fluids/tank/side"),
            id("block/fluids/tank/bottom"), MultiElementConnectedModel.WITH_INNER);

    static void load() {
    }
}
