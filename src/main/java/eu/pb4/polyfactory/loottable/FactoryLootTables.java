package eu.pb4.polyfactory.loottable;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import net.minecraft.block.Block;
import net.minecraft.loot.provider.number.LootNumberProviderType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryLootTables {
    public static void register() {
        register("number_from_state", IntStateLootNumberProvider.TYPE);
    }


    public static <T extends LootNumberProviderType> T register(String path, T item) {
        return Registry.register(Registries.LOOT_NUMBER_PROVIDER_TYPE, new Identifier(ModInit.ID, path), item);
    }
}
