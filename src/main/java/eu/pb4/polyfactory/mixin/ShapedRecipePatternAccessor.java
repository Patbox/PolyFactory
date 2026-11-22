package eu.pb4.polyfactory.mixin;

import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.crafting.ShapedRecipePattern.class)
public interface ShapedRecipePatternAccessor {
    @Accessor
    Optional<ShapedRecipePattern.Data> getData();
}
