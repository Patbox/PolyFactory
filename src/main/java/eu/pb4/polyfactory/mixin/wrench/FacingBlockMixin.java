package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin({ PistonBlock.class, DispenserBlock.class })
public class FacingBlockMixin implements WrenchableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING);
    }
}
