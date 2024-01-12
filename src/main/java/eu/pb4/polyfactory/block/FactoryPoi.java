package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlock;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlock;
import eu.pb4.polyfactory.block.data.output.NixieTubeControllerBlock;
import eu.pb4.polyfactory.block.data.output.RedstoneOutputBlock;
import eu.pb4.polyfactory.block.data.providers.*;
import eu.pb4.polyfactory.block.electric.ElectricGeneratorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.electric.WitherSkullGeneratorBlock;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlacerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlock;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.other.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.*;
import net.minecraft.block.enums.Instrument;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FactoryPoi {
    public static PointOfInterestType WIRELESS_REDSTONE_RECEIVED = register("wireless_redstone_receiver", Set.of());
    public static void register() {

    }
    public static PointOfInterestType register(String path, Set<BlockState> blockStates) {
        return register(path, new PointOfInterestType(blockStates, 1, 1));
    }
    public static PointOfInterestType register(String path, Set<BlockState> blockStates, int ticketCount) {
        return register(path, new PointOfInterestType(blockStates, ticketCount, 1));
    }
    public static PointOfInterestType register(String path, Set<BlockState> blockStates, int ticketCount, int searchDistance) {
        return register(path, new PointOfInterestType(blockStates, ticketCount, searchDistance));
    }
    public static PointOfInterestType register(String path, PointOfInterestType item) {
        return Registry.register(Registries.POINT_OF_INTEREST_TYPE, new Identifier(ModInit.ID, path), item);
    }
}
