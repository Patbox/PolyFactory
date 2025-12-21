package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StateDefinition.class)
public class StateDefinitionMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), ordinal = 1)
    private MapCodec<BlockState> modifyCodec(MapCodec<BlockState> codec, @Local Object owner) {
        if (owner instanceof StatePropertiesCodecPatcher patcher) {
            return patcher.modifyPropertiesCodec(codec);
        }
        return codec;
    }
}
