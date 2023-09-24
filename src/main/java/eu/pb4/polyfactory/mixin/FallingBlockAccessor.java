package eu.pb4.polyfactory.mixin;

import net.minecraft.block.FallingBlock;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlock.class)
public interface FallingBlockAccessor {
    @Invoker
    void callConfigureFallingBlockEntity(FallingBlockEntity entity);
}
