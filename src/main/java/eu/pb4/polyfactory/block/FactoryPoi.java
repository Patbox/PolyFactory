package eu.pb4.polyfactory.block;

import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Set;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryPoi {
    public static final ResourceKey<PoiType> WIRELESS_REDSTONE_RECEIVED = of("wireless_redstone_receiver");

    public static void register() {
        register(WIRELESS_REDSTONE_RECEIVED,
                Set.copyOf(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER.getStateDefinition().getPossibleStates()));
    }


    private static ResourceKey<PoiType> of(String path) {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id(path));
    }

    public static PoiType register(ResourceKey<PoiType> key, Set<BlockState> blockStates) {
        return register(key, blockStates, 1, 1);
    }
    public static PoiType register(ResourceKey<PoiType> key, Set<BlockState> blockStates, int ticketCount) {
        return register(key, blockStates, ticketCount, 1);
    }
    public static PoiType register(ResourceKey<PoiType> key, Set<BlockState> blockStates, int ticketCount, int searchDistance) {
        var type = PointOfInterestHelper.register(key.identifier(), ticketCount, searchDistance, blockStates);
        var rep = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getValue(PoiTypes.HOME);
        PolymerSyncedObject.setSyncedObject(BuiltInRegistries.POINT_OF_INTEREST_TYPE, type, (obj, context) -> rep);
        return type;
    }
}
