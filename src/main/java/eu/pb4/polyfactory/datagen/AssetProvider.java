package eu.pb4.polyfactory.datagen;

import com.google.common.hash.HashCode;
import eu.pb4.polyfactory.models.ConveyorModels;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

class AssetProvider implements DataProvider {
    private final DataOutput output;

    public AssetProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        BiConsumer<String, byte[]> assetWriter = (path, data) -> {
            try {
                writer.write(this.output.getPath().resolve(path), data, HashCode.fromBytes(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return CompletableFuture.runAsync(() -> {
            runWriters(assetWriter);
        }, Util.getMainWorkerExecutor());
    }

    public static void runWriters(BiConsumer<String,byte[]> assetWriter) {
        ConveyorModels.generateModels(assetWriter);
        UiResourceCreator.generateAssets(assetWriter);
        FactoryModels.COLORED_CABLE.generateModels(assetWriter);
        FactoryModels.COLORED_WALL_CABLE.generateModels(assetWriter);
        FactoryModels.PIPE.generateModels(assetWriter);
    }

    @Override
    public String getName() {
        return "polyfactory:assets";
    }
}
