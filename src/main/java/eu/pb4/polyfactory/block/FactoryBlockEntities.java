package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.ItemGeneratorBlockEntity;
import eu.pb4.polyfactory.block.machines.*;
import eu.pb4.polyfactory.block.mechanical.HandCrankBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlockEntity;
import eu.pb4.polyfactory.block.storage.DrawerBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
            FabricBlockEntityTypeBuilder.create(HandCrankBlockEntity::new).addBlock(FactoryBlocks.FAN));


    public static final BlockEntityType<DrawerBlockEntity> DRAWER = register("drawer",
            FabricBlockEntityTypeBuilder.create(DrawerBlockEntity::new).addBlock(FactoryBlocks.DRAWER));

    public static final BlockEntityType<GrinderBlockEntity> GRINDER = register("grinder",
            FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new).addBlock(FactoryBlocks.GRINDER));

    public static final BlockEntityType<MinerBlockEntity> MINER = register("miner",
            FabricBlockEntityTypeBuilder.create(MinerBlockEntity::new).addBlock(FactoryBlocks.MINER));

    public static final BlockEntityType<PressBlockEntity> PRESS = register("press",
            FabricBlockEntityTypeBuilder.create(PressBlockEntity::new).addBlock(FactoryBlocks.PRESS));

    public static final BlockEntityType<ItemGeneratorBlockEntity> ITEM_GENERATOR = register("item_generator",
            FabricBlockEntityTypeBuilder.create(ItemGeneratorBlockEntity::new).addBlock(FactoryBlocks.ITEM_GENERATOR));

    public static void register() {}


    public static <T extends BlockEntity> BlockEntityType<T> register(String path, FabricBlockEntityTypeBuilder<T> item) {
        var x = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(ModInit.ID, path), item.build());
        PolymerBlockUtils.registerBlockEntity(x);
        return x;
    }
}
