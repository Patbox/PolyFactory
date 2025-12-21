package eu.pb4.polyfactory.models.fluid;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class TopFluidViewModel extends RotationAwareModel {
    private final ItemDisplayElement fluid;
    private final float startOffset;
    private final float maxHeight;
    private FluidInstance<?> currentFluid = null;
    private float positionFluid = -1;

    public TopFluidViewModel(ElementHolder holder, float startOffset, float maxHeight, float viewRange) {
        this.fluid = ItemDisplayElementUtil.createSimple();
        this.fluid.setViewRange(viewRange);
        this.startOffset = startOffset;
        this.maxHeight = maxHeight;
        holder.addElement(fluid);
    }

    public void destroy() {
        if (fluid.getHolder() != null) {
            fluid.getHolder().removeElement(fluid);
        }
    }

    public ItemDisplayElement fluidDisplay() {
        return this.fluid;
    }

    public void setFluid(@Nullable FluidInstance<?> type, float position) {
        if (type == null || position < 0.01) {
            this.fluid.setItem(ItemStack.EMPTY);
            this.currentFluid = null;
            this.positionFluid = -1;
        }
        if (this.currentFluid != type) {
            this.fluid.setItem(FactoryModels.FLUID_FLAT_FULL.get(type));
            this.fluid.setBrightness(type.brightness().orElse(null));
            this.currentFluid = type;
        }
        if (this.positionFluid != position) {
            this.fluid.setTranslation(new Vector3f(0, this.startOffset + position * this.maxHeight, 0));
            this.positionFluid = position;
        }
    }
}
