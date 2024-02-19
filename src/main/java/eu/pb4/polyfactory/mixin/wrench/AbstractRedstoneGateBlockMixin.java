package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(AbstractRedstoneGateBlock.class)
public class AbstractRedstoneGateBlockMixin  implements WrenchableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING_HORIZONTAL);
    }
}
