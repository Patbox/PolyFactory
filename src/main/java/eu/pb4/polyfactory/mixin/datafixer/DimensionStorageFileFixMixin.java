package eu.pb4.polyfactory.mixin.datafixer;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(DimensionStorageFileFix.class)
public abstract class DimensionStorageFileFixMixin extends FileFix {
    public DimensionStorageFileFixMixin(Schema schema) {
        super(schema);
    }

    @Inject(method = "makeFixer", at = @At("HEAD"))
    private void handlePolyFactoryFixes(CallbackInfo ci) {
        this.addFileFixOperation(FileFixOperations.groupMove(
                Map.of("data", "dimensions/minecraft/overworld/data/",
                        "DIM-1/data", "dimensions/minecraft/the_nether/data/",
                        "DIM1/data", "dimensions/minecraft/the_end/data/"
                ), List.of(
                        FileFixOperations.moveSimple("polyfactory")
                )));
    }
}
