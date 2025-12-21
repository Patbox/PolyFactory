package eu.pb4.polyfactory.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlock.class)
public interface FallingBlockAccessor {
    @Invoker
    void callFalling(FallingBlockEntity entity);
}
