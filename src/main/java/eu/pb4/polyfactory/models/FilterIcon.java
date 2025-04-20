package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

public class FilterIcon {
    private final List<ItemDisplayElement> elements = new ArrayList<>();
    private final ElementHolder holder;
    private final Matrix4f transform = new Matrix4f();
    private FilterData filterData = FilterData.EMPTY_TRUE;

    public FilterIcon(ElementHolder holder) {
        this.holder = holder;
    }

    public void setFilter(FilterData data) {
        this.filterData = data;
        var items = data.icon().size();
        var count = items;
        if (data.prevent()) {
            count *= 2;
        }
        while (this.elements.size() > count) {
            this.holder.removeElement(this.elements.removeLast());
        }
        while (this.elements.size() < count) {
            this.elements.add(this.holder.addElement(createDefaultElement()));
        }

        int i = 0;
        for (; i < items; i++) {
            this.elements.get(i).setItem(data.icon().get(i));
        }
        for (; i < count; i++) {
            this.elements.get(i).setItem(GuiTextures.ITEM_FILTER_BLOCKED);
        }

        this.applyTransforms();
    }

    private void applyTransforms() {
        var items = this.filterData.icon().size();
        var count = items / 2f - 0.5f;
        var mat = BlockModel.mat();
        mat.set(this.transform);
        if (items > 3) {
            mat.scale(3f / items);
        }
        mat.translate(count, 0, 0);
        for (var i = 0; i < items; i++) {
            this.elements.get(i).setTransformation(mat);
            if (this.filterData.prevent()) {
                mat.translate(0, 0,-1);
                this.elements.get(i + items).setTransformation(mat);
                mat.translate(0, 0,1);
            }
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
        element.setItemDisplayContext(ItemDisplayContext.GUI);
        element.setInvisible(true);
        element.setViewRange(0.3f);
        return element;
    }
}
