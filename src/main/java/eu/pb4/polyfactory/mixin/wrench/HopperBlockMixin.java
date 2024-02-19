package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.state.property.DirectionProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(HopperBlock.class)
public class HopperBlockMixin implements WrenchableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING_HOPPER);
    }
}
