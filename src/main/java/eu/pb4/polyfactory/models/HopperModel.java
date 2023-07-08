package eu.pb4.polyfactory.models;

import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class HopperModel extends BaseModel {
    private final LodItemDisplayElement icon;
    private final LodItemDisplayElement model;

    public HopperModel(BlockState cachedState) {
        this.model = LodItemDisplayElement.createSimple(GenericParts.FILTER_MESH);
        this.model.setScale(new Vector3f(2));
        this.model.setTranslation(new Vector3f(0, 15 / 16f, 0));
        this.icon = LodItemDisplayElement.createSimple();
        this.icon.setModelTransformation(ModelTransformationMode.GUI);
        this.icon.setViewRange(0.1f);
        this.icon.setScale(new Vector3f(0.3f, 0.3f, 0.005f));
        this.icon.setLeftRotation(new Quaternionf().rotateX(MathHelper.HALF_PI * 0.3f));
        this.icon.setTranslation(new Vector3f(0, 0.5f, 0.37f));
        this.addElement(this.icon);
        this.addElement(this.model);
        this.updateRotation(cachedState);
    }

    public void setItem(ItemStack stack) {
        this.icon.setItem(stack);
    }

    public void updateRotation(BlockState state) {
        var facing = state.get(HopperBlock.FACING);

        if (facing == Direction.DOWN) {
            this.icon.setYaw(0);
        }
        this.icon.setYaw(facing.asRotation());

        this.tick();
    }
}
