package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.Map;

public class FactoryLootTables {
    public static void register() {
        register("copy_color", CopyColorLootFunction.TYPE);
        register("copy_cached_data", CopyCachedDataLootFunction.TYPE);
        register("copy_read_only", CopyReadOnlyLootFunction.TYPE);
        register("copy_fluids", CopyFluidsLootFunction.TYPE);
    }


    public static void register(String path, MapCodec<? extends LootItemFunction> item) {
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
    }
}
