package eu.pb4.polyfactory.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;

@Mixin(PotionBrewing.class)
public interface PotionBrewingAccessor {
    @Accessor
    List<PotionBrewing.Mix<Potion>> getPotionMixes();
}
