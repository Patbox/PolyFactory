package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.mechanical_connectors.LargeGearNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class AxleWithLargeGearBlock extends AxleWithGearBlock {
    public AxleWithLargeGearBlock(Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return FactoryItems.LARGE_STEEL_GEAR.getDefaultInstance();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock.Model(world, initialBlockState, pos);
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new LargeGearNode(state.getValue(AXIS)));
    }

    @Override
    public boolean isLargeGear(BlockState state) {
        return true;
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack GEAR_MODEL = ItemDisplayElementUtil.getSolidModel(id("block/large_gear"));

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final LodItemDisplayElement gear;
        private boolean offset;
        private Model(ServerLevel world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.gear = LodItemDisplayElement.createSimple(GEAR_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.gear.setViewRange(0.7f);
            this.offset = ((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0);
            this.updateAnimation(0,  state.getValue(AXIS));
            this.addElement(this.mainElement);
            this.addElement(this.gear);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(rotation);
            mat.pushMatrix();
            mat.scale(2, 2f, 2);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();
            mat.rotateY(!this.offset ? Mth.HALF_PI / 8 : 0);
            this.gear.setTransformation(mat);
        }
        @Override
        protected void onTick() {
            var tick = this.blockAware().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(), this.blockAware().getBlockState().getValue(AXIS));
                this.mainElement.startInterpolationIfDirty();
                this.gear.startInterpolationIfDirty();
            }
        }
    }
}
