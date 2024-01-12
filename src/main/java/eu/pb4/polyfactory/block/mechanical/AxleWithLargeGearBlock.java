package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.mechanical_connectors.LargeGearNode;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class AxleWithLargeGearBlock extends AxleWithGearBlock {
    public AxleWithLargeGearBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return FactoryItems.LARGE_STEEL_GEAR.getDefaultStack();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState, pos);
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new LargeGearNode(state.get(AXIS)));
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack GEAR_MODEL = new ItemStack(BaseItemProvider.requestModel());

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final LodItemDisplayElement gear;
        private boolean offset;
        private Model(ServerWorld world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.gear = LodItemDisplayElement.createSimple(GEAR_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.gear.setViewRange(0.7f);
            this.offset = ((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0);
            this.updateAnimation(0,  state.get(AXIS));
            this.addElement(this.mainElement);
            this.addElement(this.gear);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(rotation);
            mat.pushMatrix();
            mat.scale(2, 2.005f, 2);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();
            mat.rotateY(!this.offset ? MathHelper.HALF_PI / 8 : 0);
            this.gear.setTransformation(mat);
        }
        @Override
        protected void onTick() {
            var tick = this.blockAware().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(), this.blockAware().getBlockState().get(AXIS));
                this.mainElement.startInterpolationIfDirty();
                this.gear.startInterpolationIfDirty();
            }
        }

        static {
            GEAR_MODEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(GEAR_MODEL.getItem(), id("block/large_gear")).value());
        }
    }
}
