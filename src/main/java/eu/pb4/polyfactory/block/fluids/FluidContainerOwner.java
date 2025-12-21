package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface FluidContainerOwner extends FilledStateProvider {
    @Nullable
    FluidContainer getFluidContainer(Direction direction);

    @Nullable
    FluidContainer getMainFluidContainer();
    @Override
    default Component getFilledStateText() {
        var main = getMainFluidContainer();
        return main != null ?
                Component.translatable("text.polyfactory.x_out_of_y", FactoryUtil.fluidTextGeneric(main.stored()), FactoryUtil.fluidTextGeneric(main.capacity()))
                : null;
    }

    @Override
    default long getFillCapacity() {
        var main = getMainFluidContainer();
        return main != null ? main.capacity() : 0;
    }

    @Override
    default long getFilledAmount() {
        var main = getMainFluidContainer();
        return main != null ? main.stored() : 0;
    }
}
