package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.AllSideNode;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class GearboxBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public GearboxBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AllSideNode());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model();
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends LodElementHolder {
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement xAxle;
        private final ItemDisplayElement yAxle;
        private final ItemDisplayElement zAxle;

        private final Matrix4f mat = new Matrix4f();

        private Model() {
            this.mainElement = new LodItemDisplayElement(FactoryItems.GEARBOX_BLOCK.getDefaultStack());
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setScale(new Vector3f(2));
            this.addElement(this.mainElement);

            this.xAxle = new LodItemDisplayElement(AxleBlock.Model.ITEM_MODEL);
            this.xAxle.setDisplaySize(1, 1);
            this.xAxle.setModelTransformation(ModelTransformationMode.FIXED);
            this.xAxle.setInterpolationDuration(5);

            this.yAxle = new LodItemDisplayElement(AxleBlock.Model.ITEM_MODEL);
            this.yAxle.setDisplaySize(1, 1);
            this.yAxle.setModelTransformation(ModelTransformationMode.FIXED);
            this.yAxle.setInterpolationDuration(5);

            this.zAxle = new LodItemDisplayElement(AxleBlock.Model.ITEM_MODEL);
            this.zAxle.setDisplaySize(1, 1);
            this.zAxle.setModelTransformation(ModelTransformationMode.FIXED);
            this.zAxle.setInterpolationDuration(5);
            this.updateAnimation(0);

            this.addElement(this.mainElement);
            this.addElement(this.xAxle);
            this.addElement(this.yAxle);
            this.addElement(this.zAxle);
        }

        private void updateAnimation(float rotation) {
            mat.identity();
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.yAxle.setTransformation(mat);
            mat.identity();
            mat.rotate(Direction.EAST.getRotationQuaternion());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.xAxle.setTransformation(mat);

            mat.identity();
            mat.rotate(Direction.NORTH.getRotationQuaternion());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.zAxle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation());
                if (this.xAxle.isDirty()) {
                    this.xAxle.startInterpolation();
                    this.yAxle.startInterpolation();
                    this.zAxle.startInterpolation();
                }
            }
        }
    }
}
