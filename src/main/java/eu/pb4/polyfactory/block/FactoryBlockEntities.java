package eu.pb4.polyfactory.block;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeDrainBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlockEntity;
import eu.pb4.polyfactory.block.data.CableBlockEntity;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.io.DataExtractorBlockEntity;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlockEntity;
import eu.pb4.polyfactory.block.data.output.HologramProjectorBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeControllerBlockEntity;
import eu.pb4.polyfactory.block.data.providers.RecordPlayerBlockEntity;
import eu.pb4.polyfactory.block.data.providers.ItemReaderBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlockEntity;
import eu.pb4.polyfactory.block.fluids.*;
import eu.pb4.polyfactory.block.fluids.smeltery.CastingCauldronBlockEntity;
import eu.pb4.polyfactory.block.fluids.smeltery.CastingTableBlockEntity;
import eu.pb4.polyfactory.block.fluids.smeltery.IndustrialSmelteryBlockEntity;
import eu.pb4.polyfactory.block.fluids.smeltery.PrimitiveSmelteryBlockEntity;
import eu.pb4.polyfactory.block.fluids.transport.FilteredPipeBlockEntity;
import eu.pb4.polyfactory.block.fluids.transport.PipeBlockEntity;
import eu.pb4.polyfactory.block.fluids.transport.PumpBlockEntity;
import eu.pb4.polyfactory.block.fluids.transport.RedstoneValvePipeBlockEntity;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlockEntity;
import eu.pb4.polyfactory.block.mechanical.EjectorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SlotAwareFunnelBlockEntity;
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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class FactoryBlockEntities {
    public static final BlockEntityType<ConveyorBlockEntity> CONVEYOR = register("conveyor",
             FabricBlockEntityTypeBuilder.create(ConveyorBlockEntity::new, FactoryBlocks.CONVEYOR, FactoryBlocks.STICKY_CONVEYOR));

    public static final BlockEntityType<FunnelBlockEntity> FUNNEL = register("funnel",
             FabricBlockEntityTypeBuilder.create(FunnelBlockEntity::new, FactoryBlocks.FUNNEL));

    public static final BlockEntityType<SlotAwareFunnelBlockEntity> SLOT_AWARE_FUNNEL = register("slot_aware_funnel",
            FabricBlockEntityTypeBuilder.create(SlotAwareFunnelBlockEntity::new, FactoryBlocks.SLOT_AWARE_FUNNEL));

    public static final BlockEntityType<SplitterBlockEntity> SPLITTER = register("splitter",
             FabricBlockEntityTypeBuilder.create(SplitterBlockEntity::new, FactoryBlocks.SPLITTER));

    public static final BlockEntityType<FanBlockEntity> FAN = register("fan",
             FabricBlockEntityTypeBuilder.create(FanBlockEntity::new, FactoryBlocks.FAN));
    public static final BlockEntityType<ChainDriveBlockEntity> CHAIN_DRIVE = register("chain_drive",
            FabricBlockEntityTypeBuilder.create(ChainDriveBlockEntity::new, FactoryBlocks.CHAIN_DRIVE));
    public static final BlockEntityType<EjectorBlockEntity> EJECTOR = register("ejector",
            FabricBlockEntityTypeBuilder.create(EjectorBlockEntity::new, FactoryBlocks.EJECTOR));
    public static final BlockEntityType<HandCrankBlockEntity> HAND_CRANK = register("hand_crank",
             FabricBlockEntityTypeBuilder.create(HandCrankBlockEntity::new, FactoryBlocks.HAND_CRANK));

    public static final BlockEntityType<WindmillBlockEntity> WINDMILL = register("windmill",
             FabricBlockEntityTypeBuilder.create(WindmillBlockEntity::new, FactoryBlocks.WINDMILL));

    public static final BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE = register("steam_engine",
             FabricBlockEntityTypeBuilder.create(SteamEngineBlockEntity::new, FactoryBlocks.STEAM_ENGINE));

    public static final BlockEntityType<IndustrialSmelteryBlockEntity> SMELTERY = register("smeltery",
            FabricBlockEntityTypeBuilder.create(IndustrialSmelteryBlockEntity::new, FactoryBlocks.SMELTERY));

    public static final BlockEntityType<PrimitiveSmelteryBlockEntity> PRIMITIVE_SMELTERY = register("primitive_smeltery",
            FabricBlockEntityTypeBuilder.create(PrimitiveSmelteryBlockEntity::new, FactoryBlocks.PRIMITIVE_SMELTERY));

    public static final BlockEntityType<CastingTableBlockEntity> CASTING_TABLE = register("casting_table",
            FabricBlockEntityTypeBuilder.create(CastingTableBlockEntity::new, FactoryBlocks.CASTING_TABLE));
    public static final BlockEntityType<CastingCauldronBlockEntity> CASTING_CAULDRON = register("casting_cauldron",
            FabricBlockEntityTypeBuilder.create(CastingCauldronBlockEntity::new, FactoryBlocks.CASTING_CAULDRON));
    public static final BlockEntityType<ContainerBlockEntity> CONTAINER = register("container",
             FabricBlockEntityTypeBuilder.create(ContainerBlockEntity::new, FactoryBlocks.CONTAINER));
    public static final BlockEntityType<DeepStorageContainerBlockEntity> DEEP_STORAGE_CONTAINER = register("deep_storage_container",
            FabricBlockEntityTypeBuilder.create(DeepStorageContainerBlockEntity::new, FactoryBlocks.DEEP_STORAGE_CONTAINER));
    public static final BlockEntityType<ItemOutputBufferBlockEntity> ITEM_BUFFER = register("item_buffer",
            FabricBlockEntityTypeBuilder.create(ItemOutputBufferBlockEntity::new, FactoryBlocks.ITEM_OUTPUT_BUFFER));

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
    public static final BlockEntityType<BlockEntity> PROVIDER_DATA_CACHE = register("provider_data_cache",
             FabricBlockEntityTypeBuilder.create(ChanneledDataBlockEntity::migrating, FactoryBlocks.ITEM_COUNTER, FactoryBlocks.REDSTONE_INPUT, FactoryBlocks.REDSTONE_OUTPUT,
                    FactoryBlocks.TACHOMETER, FactoryBlocks.STRESSOMETER, FactoryBlocks.BLOCK_OBSERVER, FactoryBlocks.DATA_MEMORY, FactoryBlocks.TEXT_INPUT,
                     FactoryBlocks.SPEAKER, FactoryBlocks.TPS_PROVIDER, FactoryBlocks.GAUGE, FactoryBlocks.DIGITAL_CLOCK));

    public static final BlockEntityType<DataMemoryBlockEntity> DATA_MEMORY = register("data_memory",
            FabricBlockEntityTypeBuilder.create(DataMemoryBlockEntity::new, FactoryBlocks.DATA_MEMORY));

    public static final BlockEntityType<HologramProjectorBlockEntity> HOLOGRAM_PROJECTOR = register("hologram_projector",
             FabricBlockEntityTypeBuilder.create(HologramProjectorBlockEntity::new, FactoryBlocks.HOLOGRAM_PROJECTOR));

    public static final BlockEntityType<WirelessRedstoneBlockEntity> WIRELESS_REDSTONE = register("wireless_redstone",
             FabricBlockEntityTypeBuilder.create(WirelessRedstoneBlockEntity::new, FactoryBlocks.WIRELESS_REDSTONE_RECEIVER, FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER));
    public static final BlockEntityType<CableBlockEntity> CABLE = register("cable",  FabricBlockEntityTypeBuilder.create(CableBlockEntity::new, FactoryBlocks.CABLE));
    public static final BlockEntityType<ItemReaderBlockEntity> ITEM_READER = register("item_reader",
             FabricBlockEntityTypeBuilder.create(ItemReaderBlockEntity::new, FactoryBlocks.ITEM_READER));
    public static final BlockEntityType<RecordPlayerBlockEntity> RECORD_PLAYER = register("record_player",
            FabricBlockEntityTypeBuilder.create(RecordPlayerBlockEntity::new, FactoryBlocks.RECORD_PLAYER));
    public static final BlockEntityType<DoubleInputTransformerBlockEntity> DOUBLE_INPUT_TRANSFORMER = register("double_input_transformer",  FabricBlockEntityTypeBuilder
            .create(DoubleInputTransformerBlockEntity::new, FactoryBlocks.ARITHMETIC_OPERATOR, FactoryBlocks.DATA_COMPARATOR, FactoryBlocks.PROGRAMMABLE_DATA_EXTRACTOR));
    public static final BlockEntityType<InputTransformerBlockEntity> INPUT_TRANSFORMER = register("input_transformer",  FabricBlockEntityTypeBuilder
            .create(InputTransformerBlockEntity::new));

    public static final BlockEntityType<DataExtractorBlockEntity> DATA_EXTRACTOR = register("data_extractor", FabricBlockEntityTypeBuilder
            .create(DataExtractorBlockEntity::new, FactoryBlocks.DATA_EXTRACTOR));

    public static final BlockEntityType<WorkbenchBlockEntity> WORKBENCH = register("workbench",  FabricBlockEntityTypeBuilder
            .create(WorkbenchBlockEntity::new, FactoryBlocks.WORKBENCH));

    public static final BlockEntityType<BlueprintWorkbenchBlockEntity> BLUEPRINT_WORKBENCH = register("blueprint_workbench",  FabricBlockEntityTypeBuilder
            .create(BlueprintWorkbenchBlockEntity::new, FactoryBlocks.BLUEPRINT_WORKBENCH));
    public static final BlockEntityType<ColorableBlockEntity> COLOR_CONTAINER = register("color_container",  FabricBlockEntityTypeBuilder
            .create(ColorableBlockEntity::new, FactoryBlocks.LAMP, FactoryBlocks.INVERTED_LAMP, FactoryBlocks.CAGED_LAMP, FactoryBlocks.INVERTED_CAGED_LAMP, FactoryBlocks.FIXTURE_LAMP, FactoryBlocks.INVERTED_FIXTURE_LAMP));

    public static final BlockEntityType<PipeBlockEntity> PIPE = register("pipe",  FabricBlockEntityTypeBuilder.create(PipeBlockEntity::new, FactoryBlocks.PIPE));

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
        var x = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item.build());
        PolymerBlockUtils.registerBlockEntity(x);
        return x;
    }
}
