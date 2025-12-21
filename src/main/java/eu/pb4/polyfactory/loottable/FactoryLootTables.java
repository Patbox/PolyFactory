package eu.pb4.polyfactory.loottable;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;

public class FactoryLootTables {
    public static void register() {
        register("copy_color", CopyColorLootFunction.TYPE);
        register("copy_cached_data", CopyCachedDataLootFunction.TYPE);
        register("copy_read_only", CopyReadOnlyLootFunction.TYPE);
        register("copy_fluids", CopyFluidsLootFunction.TYPE);
    }


    public static <T extends LootNumberProviderType> T register(String path, T item) {
        return Registry.register(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
    }

    public static <T extends LootItemFunctionType<?>> T register(String path, T item) {
        return Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
    }
}
