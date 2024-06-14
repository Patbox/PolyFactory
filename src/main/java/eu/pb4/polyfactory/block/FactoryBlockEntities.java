package eu.pb4.polyfactory.block;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlockEntity;
import eu.pb4.polyfactory.block.data.CableBlockEntity;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.output.HologramProjectorBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeControllerBlockEntity;
import eu.pb4.polyfactory.block.data.providers.ItemReaderBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlockEntity;
import eu.pb4.polyfactory.block.electric.WitherSkullGeneratorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.*;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlockEntity;
import eu.pb4.polyfactory.block.other.ContainerBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlockEntity;
import eu.pb4.polyfactory.block.other.ColorableBlockEntity;
import eu.pb4.polyfactory.block.other.WirelessRedstoneBlockEntity;
import eu.pb4.polyfactory.block.other.WorkbenchBlockEntity;
import eu.pb4.polyfactory.mixin.util.BlockEntityTypeAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryBlockEntities {
    public static final BlockEntityType<ConveyorBlockEntity> CONVEYOR = register("conveyor",
            FabricBlockEntityTypeBuilder.create(ConveyorBlockEntity::new).addBlocks(FactoryBlocks.CONVEYOR, FactoryBlocks.STICKY_CONVEYOR));

    public static final BlockEntityType<FunnelBlockEntity> FUNNEL = register("funnel",
            FabricBlockEntityTypeBuilder.create(FunnelBlockEntity::new).addBlock(FactoryBlocks.FUNNEL));

    public static final BlockEntityType<SplitterBlockEntity> SPLITTER = register("splitter",
            FabricBlockEntityTypeBuilder.create(SplitterBlockEntity::new).addBlock(FactoryBlocks.SPLITTER));

    public static final BlockEntityType<FanBlockEntity> FAN = register("fan",
            FabricBlockEntityTypeBuilder.create(FanBlockEntity::new).addBlock(FactoryBlocks.FAN));

    public static final BlockEntityType<HandCrankBlockEntity> HAND_CRANK = register("hand_crank",
            FabricBlockEntityTypeBuilder.create(HandCrankBlockEntity::new).addBlock(FactoryBlocks.HAND_CRANK));

    public static final BlockEntityType<WindmillBlockEntity> WINDMILL = register("windmill",
            FabricBlockEntityTypeBuilder.create(WindmillBlockEntity::new).addBlock(FactoryBlocks.WINDMILL));

    public static final BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE = register("steam_engine",
            FabricBlockEntityTypeBuilder.create(SteamEngineBlockEntity::new).addBlock(FactoryBlocks.STEAM_ENGINE));

    public static final BlockEntityType<ContainerBlockEntity> CONTAINER = register("container",
            FabricBlockEntityTypeBuilder.create(ContainerBlockEntity::new).addBlock(FactoryBlocks.CONTAINER));

    public static final BlockEntityType<GrinderBlockEntity> GRINDER = register("grinder",
            FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new).addBlock(FactoryBlocks.GRINDER));

    public static final BlockEntityType<MinerBlockEntity> MINER = register("miner",
            FabricBlockEntityTypeBuilder.create(MinerBlockEntity::new).addBlock(FactoryBlocks.MINER));
    public static final BlockEntityType<PlacerBlockEntity> PLACER = register("placer",
            FabricBlockEntityTypeBuilder.create(PlacerBlockEntity::new).addBlock(FactoryBlocks.PLACER));
    public static final BlockEntityType<PlanterBlockEntity> PLANTER = register("planter",
            FabricBlockEntityTypeBuilder.create(PlanterBlockEntity::new).addBlock(FactoryBlocks.PLANTER));

    public static final BlockEntityType<PressBlockEntity> PRESS = register("press",
            FabricBlockEntityTypeBuilder.create(PressBlockEntity::new).addBlock(FactoryBlocks.PRESS));

    public static final BlockEntityType<MixerBlockEntity> MIXER = register("mixer",
            FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new).addBlock(FactoryBlocks.MIXER));

    public static final BlockEntityType<MCrafterBlockEntity> CRAFTER = register("crafter",
            FabricBlockEntityTypeBuilder.create(MCrafterBlockEntity::new).addBlock(FactoryBlocks.CRAFTER));

    public static final BlockEntityType<NixieTubeBlockEntity> NIXIE_TUBE = register("nixie_tube",
            FabricBlockEntityTypeBuilder.create(NixieTubeBlockEntity::new).addBlock(FactoryBlocks.NIXIE_TUBE));

    public static final BlockEntityType<NixieTubeControllerBlockEntity> NIXIE_TUBE_CONTROLLER = register("nixie_tube_controller",
            FabricBlockEntityTypeBuilder.create(NixieTubeControllerBlockEntity::new).addBlock(FactoryBlocks.NIXIE_TUBE_CONTROLLER));
    public static final BlockEntityType<ElectricMotorBlockEntity> ELECTRIC_MOTOR = register("electric_motor",
            FabricBlockEntityTypeBuilder.create(ElectricMotorBlockEntity::new).addBlock(FactoryBlocks.ELECTRIC_MOTOR));
    public static final BlockEntityType<CreativeContainerBlockEntity> CREATIVE_CONTAINER = register("creative_container",
            FabricBlockEntityTypeBuilder.create(CreativeContainerBlockEntity::new).addBlock(FactoryBlocks.CREATIVE_CONTAINER));

    public static final BlockEntityType<CreativeMotorBlockEntity> CREATIVE_MOTOR = register("creative_motor",
            FabricBlockEntityTypeBuilder.create(CreativeMotorBlockEntity::new).addBlock(FactoryBlocks.CREATIVE_MOTOR));
    public static final BlockEntityType<ChanneledDataBlockEntity> PROVIDER_DATA_CACHE = register("provider_data_cache", FabricBlockEntityTypeBuilder
            .create(ChanneledDataBlockEntity::new).addBlocks(FactoryBlocks.ITEM_COUNTER, FactoryBlocks.REDSTONE_INPUT, FactoryBlocks.REDSTONE_OUTPUT,
                    FactoryBlocks.TACHOMETER, FactoryBlocks.STRESSOMETER));

    public static final BlockEntityType<HologramProjectorBlockEntity> HOLOGRAM_PROJECTOR = register("hologram_projector", FabricBlockEntityTypeBuilder
            .create(HologramProjectorBlockEntity::new).addBlocks(FactoryBlocks.HOLOGRAM_PROJECTOR));

    public static final BlockEntityType<WirelessRedstoneBlockEntity> WIRELESS_REDSTONE = register("wireless_redstone", FabricBlockEntityTypeBuilder
            .create(WirelessRedstoneBlockEntity::new).addBlock(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER));
    public static final BlockEntityType<CableBlockEntity> CABLE = register("cable", FabricBlockEntityTypeBuilder
            .create(CableBlockEntity::new).addBlock(FactoryBlocks.CABLE));
    public static final BlockEntityType<ItemReaderBlockEntity> ITEM_READER = register("item_reader", FabricBlockEntityTypeBuilder
            .create(ItemReaderBlockEntity::new).addBlock(FactoryBlocks.ITEM_READER));
    public static final BlockEntityType<DoubleInputTransformerBlockEntity> DOUBLE_INPUT_TRANSFORMER = register("double_input_transformer", FabricBlockEntityTypeBuilder
            .create(DoubleInputTransformerBlockEntity::new).addBlocks(FactoryBlocks.ARITHMETIC_OPERATOR));

    public static final BlockEntityType<WorkbenchBlockEntity> WORKBENCH = register("workbench", FabricBlockEntityTypeBuilder
            .create(WorkbenchBlockEntity::new).addBlock(FactoryBlocks.WORKBENCH));
    public static final BlockEntityType<WitherSkullGeneratorBlockEntity> WITHER_SKULL_GENERATOR = register("wither_skull_generator", FabricBlockEntityTypeBuilder
            .create(WitherSkullGeneratorBlockEntity::new).addBlock(FactoryBlocks.WITHER_SKULL_GENERATOR));

    public static final BlockEntityType<ColorableBlockEntity> COLOR_CONTAINER = register("color_container", FabricBlockEntityTypeBuilder
            .create(ColorableBlockEntity::new).addBlocks(FactoryBlocks.LAMP, FactoryBlocks.INVERTED_LAMP, FactoryBlocks.CAGED_LAMP, FactoryBlocks.INVERTED_CAGED_LAMP));


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
