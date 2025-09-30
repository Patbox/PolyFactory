package eu.pb4.polyfactory.block;

import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Set;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryPoi {
    public static final RegistryKey<PointOfInterestType> WIRELESS_REDSTONE_RECEIVED = of("wireless_redstone_receiver");

    public static void register() {
        register(WIRELESS_REDSTONE_RECEIVED,
                Set.copyOf(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER.getStateManager().getStates()));
    }


    private static RegistryKey<PointOfInterestType> of(String path) {
        return RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, id(path));
    }

    public static PointOfInterestType register(RegistryKey<PointOfInterestType> key, Set<BlockState> blockStates) {
        return register(key, blockStates, 1, 1);
    }
    public static PointOfInterestType register(RegistryKey<PointOfInterestType> key, Set<BlockState> blockStates, int ticketCount) {
        return register(key, blockStates, ticketCount, 1);
    }
    public static PointOfInterestType register(RegistryKey<PointOfInterestType> key, Set<BlockState> blockStates, int ticketCount, int searchDistance) {
        var type = PointOfInterestHelper.register(key.getValue(), ticketCount, searchDistance, blockStates);
        var rep = Registries.POINT_OF_INTEREST_TYPE.get(PointOfInterestTypes.HOME);
        PolymerSyncedObject.setSyncedObject(Registries.POINT_OF_INTEREST_TYPE, type, (obj, context) -> rep);
        return type;
    }
}
