package eu.pb4.polyfactory.block;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeDrainBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlockEntity;
import eu.pb4.polyfactory.block.data.CableBlockEntity;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.output.HologramProjectorBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeControllerBlockEntity;
import eu.pb4.polyfactory.block.data.providers.ItemReaderBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlockEntity;
import eu.pb4.polyfactory.block.fluids.*;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.PlacerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.block.other.*;
import eu.pb4.polyfactory.mixin.util.BlockEntityTypeAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;

public class FactoryBlockEntities {
    public static final BlockEntityType<ConveyorBlockEntity> CONVEYOR = register("conveyor",
             FabricBlockEntityTypeBuilder.create(ConveyorBlockEntity::new, FactoryBlocks.CONVEYOR, FactoryBlocks.STICKY_CONVEYOR));

    public static final BlockEntityType<FunnelBlockEntity> FUNNEL = register("funnel",
             FabricBlockEntityTypeBuilder.create(FunnelBlockEntity::new, FactoryBlocks.FUNNEL));

    public static final BlockEntityType<SplitterBlockEntity> SPLITTER = register("splitter",
             FabricBlockEntityTypeBuilder.create(SplitterBlockEntity::new, FactoryBlocks.SPLITTER));

    public static final BlockEntityType<FanBlockEntity> FAN = register("fan",
             FabricBlockEntityTypeBuilder.create(FanBlockEntity::new, FactoryBlocks.FAN));

    public static final BlockEntityType<HandCrankBlockEntity> HAND_CRANK = register("hand_crank",
             FabricBlockEntityTypeBuilder.create(HandCrankBlockEntity::new, FactoryBlocks.HAND_CRANK));

    public static final BlockEntityType<WindmillBlockEntity> WINDMILL = register("windmill",
             FabricBlockEntityTypeBuilder.create(WindmillBlockEntity::new, FactoryBlocks.WINDMILL));

    public static final BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE = register("steam_engine",
             FabricBlockEntityTypeBuilder.create(SteamEngineBlockEntity::new, FactoryBlocks.STEAM_ENGINE));

    public static final BlockEntityType<ContainerBlockEntity> CONTAINER = register("container",
             FabricBlockEntityTypeBuilder.create(ContainerBlockEntity::new, FactoryBlocks.CONTAINER));

    public static final BlockEntityType<GrinderBlockEntity> GRINDER = register("grinder",
             FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new, FactoryBlocks.GRINDER));

    public static final BlockEntityType<MinerBlockEntity> MINER = register("miner",
             FabricBlockEntityTypeBuilder.create(MinerBlockEntity::new, FactoryBlocks.MINER));
    public static final BlockEntityType<PlacerBlockEntity> PLACER = register("placer",
             FabricBlockEntityTypeBuilder.create(PlacerBlockEntity::new, FactoryBlocks.PLACER));
    public static final BlockEntityType<PlanterBlockEntity> PLANTER = register("planter",
             FabricBlockEntityTypeBuilder.create(PlanterBlockEntity::new, FactoryBlocks.PLANTER));

    public static final BlockEntityType<PressBlockEntity> PRESS = register("press",
             FabricBlockEntityTypeBuilder.create(PressBlockEntity::new, FactoryBlocks.PRESS));

    public static final BlockEntityType<MixerBlockEntity> MIXER = register("mixer",
             FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new, FactoryBlocks.MIXER));

    public static final BlockEntityType<MCrafterBlockEntity> CRAFTER = register("crafter",
             FabricBlockEntityTypeBuilder.create(MCrafterBlockEntity::new, FactoryBlocks.CRAFTER));

    public static final BlockEntityType<NixieTubeBlockEntity> NIXIE_TUBE = register("nixie_tube",
             FabricBlockEntityTypeBuilder.create(NixieTubeBlockEntity::new, FactoryBlocks.NIXIE_TUBE));

    public static final BlockEntityType<NixieTubeControllerBlockEntity> NIXIE_TUBE_CONTROLLER = register("nixie_tube_controller",
             FabricBlockEntityTypeBuilder.create(NixieTubeControllerBlockEntity::new, FactoryBlocks.NIXIE_TUBE_CONTROLLER));
    public static final BlockEntityType<ElectricMotorBlockEntity> ELECTRIC_MOTOR = register("electric_motor",
             FabricBlockEntityTypeBuilder.create(ElectricMotorBlockEntity::new, FactoryBlocks.ELECTRIC_MOTOR));
    public static final BlockEntityType<CreativeContainerBlockEntity> CREATIVE_CONTAINER = register("creative_container",
             FabricBlockEntityTypeBuilder.create(CreativeContainerBlockEntity::new, FactoryBlocks.CREATIVE_CONTAINER));

    public static final BlockEntityType<CreativeMotorBlockEntity> CREATIVE_MOTOR = register("creative_motor",
             FabricBlockEntityTypeBuilder.create(CreativeMotorBlockEntity::new, FactoryBlocks.CREATIVE_MOTOR));
    public static final BlockEntityType<ChanneledDataBlockEntity> PROVIDER_DATA_CACHE = register("provider_data_cache",
             FabricBlockEntityTypeBuilder.create(ChanneledDataBlockEntity::new, FactoryBlocks.ITEM_COUNTER, FactoryBlocks.REDSTONE_INPUT, FactoryBlocks.REDSTONE_OUTPUT,
                    FactoryBlocks.TACHOMETER, FactoryBlocks.STRESSOMETER, FactoryBlocks.BLOCK_OBSERVER, FactoryBlocks.DATA_MEMORY, FactoryBlocks.TEXT_INPUT));

    public static final BlockEntityType<HologramProjectorBlockEntity> HOLOGRAM_PROJECTOR = register("hologram_projector",
             FabricBlockEntityTypeBuilder.create(HologramProjectorBlockEntity::new, FactoryBlocks.HOLOGRAM_PROJECTOR));

    public static final BlockEntityType<WirelessRedstoneBlockEntity> WIRELESS_REDSTONE = register("wireless_redstone",
             FabricBlockEntityTypeBuilder.create(WirelessRedstoneBlockEntity::new, FactoryBlocks.WIRELESS_REDSTONE_RECEIVER, FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER));
    public static final BlockEntityType<CableBlockEntity> CABLE = register("cable",  FabricBlockEntityTypeBuilder.create(CableBlockEntity::new,
            Util.make(new ArrayList<Block>(), (a) -> {
                a.add(FactoryBlocks.CABLE);
                a.addAll(FactoryBlocks.WALL_WITH_CABLE.values());
            }).toArray(Block[]::new)));
    public static final BlockEntityType<ItemReaderBlockEntity> ITEM_READER = register("item_reader",
             FabricBlockEntityTypeBuilder.create(ItemReaderBlockEntity::new, FactoryBlocks.ITEM_READER));
    public static final BlockEntityType<DoubleInputTransformerBlockEntity> DOUBLE_INPUT_TRANSFORMER = register("double_input_transformer",  FabricBlockEntityTypeBuilder
            .create(DoubleInputTransformerBlockEntity::new, FactoryBlocks.ARITHMETIC_OPERATOR, FactoryBlocks.DATA_COMPARATOR));

    public static final BlockEntityType<WorkbenchBlockEntity> WORKBENCH = register("workbench",  FabricBlockEntityTypeBuilder
            .create(WorkbenchBlockEntity::new, FactoryBlocks.WORKBENCH));
    public static final BlockEntityType<ColorableBlockEntity> COLOR_CONTAINER = register("color_container",  FabricBlockEntityTypeBuilder
            .create(ColorableBlockEntity::new, FactoryBlocks.LAMP, FactoryBlocks.INVERTED_LAMP, FactoryBlocks.CAGED_LAMP, FactoryBlocks.INVERTED_CAGED_LAMP, FactoryBlocks.FIXTURE_LAMP, FactoryBlocks.INVERTED_FIXTURE_LAMP));

    public static final BlockEntityType<PipeBlockEntity> PIPE = register("pipe",  FabricBlockEntityTypeBuilder.create(PipeBlockEntity::new, Util.make(new ArrayList<Block>(), (a) -> {
        a.add(FactoryBlocks.PIPE);
        a.addAll(FactoryBlocks.WALL_WITH_PIPE.values());
    }).toArray(Block[]::new)));

    public static final BlockEntityType<FilteredPipeBlockEntity> FILTERED_PIPE = register("filtered_pipe",
            FabricBlockEntityTypeBuilder.create(FilteredPipeBlockEntity::new, FactoryBlocks.FILTERED_PIPE));
    public static final BlockEntityType<PumpBlockEntity> PUMP = register("pump",  FabricBlockEntityTypeBuilder.create(PumpBlockEntity::new, FactoryBlocks.PUMP));
    public static final BlockEntityType<NozzleBlockEntity> NOZZLE = register("nozzle",  FabricBlockEntityTypeBuilder.create(NozzleBlockEntity::new, FactoryBlocks.NOZZLE));
    public static final BlockEntityType<DrainBlockEntity> DRAIN = register("drain",  FabricBlockEntityTypeBuilder.create(DrainBlockEntity::new, FactoryBlocks.DRAIN));
    public static final BlockEntityType<MDrainBlockEntity> MECHANICAL_DRAIN = register("mechanical_drain",  FabricBlockEntityTypeBuilder.create(MDrainBlockEntity::new, FactoryBlocks.MECHANICAL_DRAIN));
    public static final BlockEntityType<MSpoutBlockEntity> MECHANICAL_SPOUT = register("mechanical_spout",  FabricBlockEntityTypeBuilder.create(MSpoutBlockEntity::new, FactoryBlocks.MECHANICAL_SPOUT));
    public static final BlockEntityType<CreativeDrainBlockEntity> CREATIVE_DRAIN = register("creative_drain",  FabricBlockEntityTypeBuilder.create(CreativeDrainBlockEntity::new, FactoryBlocks.CREATIVE_DRAIN));
    public static final BlockEntityType<FluidTankBlockEntity> FLUID_TANK = register("fluid_tank",  FabricBlockEntityTypeBuilder.create(FluidTankBlockEntity::new, FactoryBlocks.FLUID_TANK));
    public static final BlockEntityType<PortableFluidTankBlockEntity> PORTABLE_FLUID_TANK = register("portable_fluid_tank",
             FabricBlockEntityTypeBuilder.create(PortableFluidTankBlockEntity::new, FactoryBlocks.PORTABLE_FLUID_TANK));
    public static final BlockEntityType<RedstoneValvePipeBlockEntity> REDSTONE_VALVE_PIPE = register("redstone_valve_pipe",
             FabricBlockEntityTypeBuilder.create(RedstoneValvePipeBlockEntity::new, FactoryBlocks.REDSTONE_VALVE_PIPE));

    public static final BlockEntityType<ItemPackerBlockEntity> ITEM_PACKER = register("item_packer",
            FabricBlockEntityTypeBuilder.create(ItemPackerBlockEntity::new, FactoryBlocks.ITEM_PACKER));

    public static void register() {
        var x = (BlockEntityTypeAccessor) BlockEntityType.HOPPER;
        var set = ImmutableSet.<Block>builder();
        set.addAll(x.polyfactory$getBlocks());
        x.polyfactory$setBlocks(set.build());
    }

    public static <T extends BlockEntity> BlockEntityType<T> register(String path, FabricBlockEntityTypeBuilder<T> item) {
        var x = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(ModInit.ID, path), item.build());
        PolymerBlockUtils.registerBlockEntity(x);
        return x;
    }
}
