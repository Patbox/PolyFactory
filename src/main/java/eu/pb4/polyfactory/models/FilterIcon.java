package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.item.ModelTransformationMode;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

// Todo: Blocking filter indication
public class FilterIcon {
    private final List<ItemDisplayElement> elements = new ArrayList<>();
    private final ElementHolder holder;
    private final Matrix4f transform = new Matrix4f();

    public FilterIcon(ElementHolder holder) {
        this.holder = holder;
    }

    public void setFilter(FilterData data) {
        var count = data.icon().size();
        while (this.elements.size() > count) {
            this.holder.removeElement(this.elements.removeLast());
        }
        while (this.elements.size() < count) {
            this.elements.add(this.holder.addElement(createDefaultElement()));
        }

        for (int i = 0; i < count; i++) {
            this.elements.get(i).setItem(data.icon().get(i));
        }

        this.applyTransforms();
    }

    private void applyTransforms() {
        var count = this.elements.size() / 2f - 0.5f;
        var mat = BlockModel.mat();
        mat.set(this.transform);
        if (this.elements.size() > 2) {
            mat.scale(3f / this.elements.size());
        }
        mat.translate(count, 0, 0);
        for (var element : this.elements) {
            element.setTransformation(mat);
            mat.translate(-1, 0,0);
        }
    }

    public void tick() {
        for (var element : elements) {
            element.tick();
        }
    }

    public void setTransformation(Matrix4fc matrix4fc) {
        this.transform.set(matrix4fc);
        this.applyTransforms();
    }

    public void setYaw(float yaw) {
        for (var element : elements) {
            element.setYaw(yaw);
        }
    }

    public void setPitch(float pitch) {
        for (var element : elements) {
            element.setPitch(pitch);
        }
    }

    private static ItemDisplayElement createDefaultElement() {
        var element = new ItemDisplayElement();
        element.setDisplaySize(1, 1);
        element.setModelTransformation(ModelTransformationMode.GUI);
        element.setInvisible(true);
        element.setViewRange(0.3f);
        return element;
    }
}
