package eu.pb4.polyfactory.mixin;

import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.ToolMaterial.class)
public interface ToolMaterialAccessor {
    @Invoker
    ItemAttributeModifiers callCreateToolAttributes(final float attackDamageBaseline, final float attackSpeedBaseline);
}
