package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.mechanical.AxleWithGearMechanicalNode;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldAccess;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class AxleWithGearBlock extends AxleBlock {
    public AxleWithGearBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AxleWithGearMechanicalNode(state.get(AXIS)));
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState, pos);
    }

    public final class Model extends BaseModel {
        public static final ItemStack ITEM_MODEL = AxleBlock.Model.ITEM_MODEL;
        public static final ItemStack ITEM_MODEL_GEAR = new ItemStack(Items.IRON_BARS);
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final LodItemDisplayElement gear;
        private float offset;

        private Model(ServerWorld world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(ITEM_MODEL, 4, 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.gear = LodItemDisplayElement.createSimple(ITEM_MODEL_GEAR, 4, 0.3f, 0.6f);
            this.gear.setViewRange(0.7f);
            this.updateAnimation(0,  state.get(AXIS));
            this.offset = (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0 ? MathHelper.HALF_PI / 4 : 0;
            this.addElement(this.mainElement);
            this.addElement(this.gear);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }


            mat.pushMatrix();
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();
            mat.rotateY(rotation + this.offset);
            mat.scale(1.2f);
            this.gear.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                //var x = BlockBoundAttachment.get(this);
                ///assert x != null;
                //var pos = x.getBlockPos();
                //this.offset = (pos.getManhattanDistance(Vec3i.ZERO) - pos.getComponentAlongAxis(x.getBlockState().get(AXIS))) % 8 * MathHelper.PI / 4;
            }
        }

        @Override
        public boolean stopWatching(ServerPlayNetworkHandler player) {
            if (super.stopWatching(player)) {
                return true;
            }
            return false;
        }

        @Override
        protected void onTick() {

            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(AXIS));
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                    this.gear.startInterpolation();
                }
            }
        }

        static {
            ITEM_MODEL_GEAR.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(ITEM_MODEL_GEAR.getItem(), id("block/gear")).value());
        }
    }
}
