package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlock;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.data.io.ArithmeticOperatorBlock;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.block.data.output.HologramProjectorBlock;
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
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FactoryFluids {
    public static final FluidType WATER = register(Identifier.ofVanilla("water"), new FluidType());
    public static final FluidType LAVA = register(Identifier.ofVanilla("lava"), new FluidType());
    public static final FluidType MILK = register(Identifier.ofVanilla("milk"), new FluidType());
    public static final FluidType XP = register(Identifier.ofVanilla("xp"), new FluidType());


    public static void register() {

    }

    public static FluidType register(Identifier identifier, FluidType item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES,identifier, item);
    }
    public static FluidType register(String path, FluidType item) {
        return Registry.register(FactoryRegistries.FLUID_TYPES, Identifier.of(ModInit.ID, path), item);
    }
}
