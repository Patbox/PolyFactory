package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StateManager.class)
public class StateManagerMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), ordinal = 1)
    private MapCodec<BlockState> modifyCodec(MapCodec<BlockState> codec, @Local Object owner) {
        if (owner instanceof GenericCabledDataBlock) {
            return GenericCabledDataBlock.modifyPropertiesCodec(codec);
        }
        return codec;
    }
}
