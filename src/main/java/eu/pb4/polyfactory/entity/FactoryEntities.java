package eu.pb4.polyfactory.entity;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.creative.CreativeContainerBlockEntity;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlockEntity;
import eu.pb4.polyfactory.block.data.CableBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlockEntity;
import eu.pb4.polyfactory.block.data.output.NixieTubeControllerBlockEntity;
import eu.pb4.polyfactory.block.data.providers.ItemReaderBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.FanBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.block.other.ContainerBlockEntity;
import eu.pb4.polyfactory.mixin.util.BlockEntityTypeAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryEntities {
    public static final EntityType<ArtificialWitherSkullEntity> ARTIFICIAL_WITHER_SKULL = register("artificial_wither_skull", FabricEntityTypeBuilder
            .create().fireImmune().dimensions(EntityDimensions.fixed(0.5f, 0.5f)).entityFactory(ArtificialWitherSkullEntity::new));

    public static void register() {

    }

    public static <T extends Entity> EntityType<T> register(String path, FabricEntityTypeBuilder<T> item) {
        var x = Registry.register(Registries.ENTITY_TYPE, new Identifier(ModInit.ID, path), item.build());
        PolymerEntityUtils.registerType(x);
        return x;
    }
}
