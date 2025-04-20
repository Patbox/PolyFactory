package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class HopperModel extends BlockModel {
    private final FilterIcon icon = new FilterIcon(this);
    private final ItemDisplayElement model;

    public HopperModel(BlockState cachedState) {
        this.model = ItemDisplayElementUtil.createSimple(GenericParts.FILTER_MESH);
        this.model.setScale(new Vector3f(2));
        this.model.setTranslation(new Vector3f(0, 15 / 16f, 0));
        this.icon.setTransformation(mat().translate(0, 0.5f, 0.37f).rotateX(MathHelper.HALF_PI * 0.3f).scale(0.25f, 0.25f, 0.005f));
        this.addElement(this.model);
        this.updateRotation(cachedState);
    }

    public void setFilter(FilterData data) {
        this.icon.setFilter(data);
    }

    public void updateRotation(BlockState state) {
        var facing = state.get(HopperBlock.FACING);

        if (facing == Direction.DOWN) {
            this.icon.setYaw(0);
        }
        this.icon.setYaw(facing.getPositiveHorizontalDegrees());

        this.tick();
    }
}
