package eu.pb4.polyfactory.loottable;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import net.minecraft.block.Block;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.LootNumberProviderType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryLootTables {
    public static void register() {
        register("copy_color", CopyColorLootFunction.TYPE);
        register("copy_cached_data", CopyCachedDataLootFunction.TYPE);
        register("copy_read_only", CopyReadOnlyLootFunction.TYPE);
        register("copy_fluids", CopyFluidsLootFunction.TYPE);
    }


    public static <T extends LootNumberProviderType> T register(String path, T item) {
        return Registry.register(Registries.LOOT_NUMBER_PROVIDER_TYPE, Identifier.of(ModInit.ID, path), item);
    }

    public static <T extends LootFunctionType<?>> T register(String path, T item) {
        return Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of(ModInit.ID, path), item);
    }
}
