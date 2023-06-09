package eu.pb4.polyfactory.block;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.ItemGeneratorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.*;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlockEntity;
import eu.pb4.polyfactory.block.other.ContainerBlockEntity;
import eu.pb4.polyfactory.mixin.BlockEntityTypeAccessor;
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


    public static final BlockEntityType<ContainerBlockEntity> CONTAINER = register("container",
            FabricBlockEntityTypeBuilder.create(ContainerBlockEntity::new).addBlock(FactoryBlocks.CONTAINER));

    public static final BlockEntityType<GrinderBlockEntity> GRINDER = register("grinder",
            FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new).addBlock(FactoryBlocks.GRINDER));

    public static final BlockEntityType<MinerBlockEntity> MINER = register("miner",
            FabricBlockEntityTypeBuilder.create(MinerBlockEntity::new).addBlock(FactoryBlocks.MINER));

    public static final BlockEntityType<PressBlockEntity> PRESS = register("press",
            FabricBlockEntityTypeBuilder.create(PressBlockEntity::new).addBlock(FactoryBlocks.PRESS));

    public static final BlockEntityType<MixerBlockEntity> MIXER = register("mixer",
            FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new).addBlock(FactoryBlocks.MIXER));

    public static final BlockEntityType<ItemGeneratorBlockEntity> ITEM_GENERATOR = register("item_generator",
            FabricBlockEntityTypeBuilder.create(ItemGeneratorBlockEntity::new).addBlock(FactoryBlocks.ITEM_GENERATOR));

    public static void register() {
        var x = (BlockEntityTypeAccessor) BlockEntityType.HOPPER;
        var set = ImmutableSet.<Block>builder();
        set.addAll(x.polyfactory$getBlocks());
        x.polyfactory$setBlocks(set.build());
    }


    public static <T extends BlockEntity> BlockEntityType<T> register(String path, FabricBlockEntityTypeBuilder<T> item) {
        var x = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(ModInit.ID, path), item.build());
        PolymerBlockUtils.registerBlockEntity(x);
        return x;
    }
}
