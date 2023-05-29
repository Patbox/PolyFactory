package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.graph.BlockNode;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.WrenchItem;
import eu.pb4.polyfactory.item.util.SimpleModeledPolymerItem;
import eu.pb4.polyfactory.nodes.mechanical.AxisMechanicalNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;

public class AxleBlock extends NetworkBlock implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public static final Property<Direction.Axis> AXIS = Properties.AXIS;

    public AxleBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(AXIS, ctx.getSide().getAxis());
    }

    @Override
    public Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AxisMechanicalNode(state.get(AXIS)));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.STRIPPED_OAK_LOG.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends LodElementHolder {
        public static final ItemStack ITEM_MODEL = new ItemStack(Items.PAPER);
        public static final ItemStack ITEM_MODEL_SHORT = new ItemStack(Items.PAPER);
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement mainElement;

        private Model(ServerWorld world, BlockState state) {
            this.mainElement = new LodItemDisplayElement(ITEM_MODEL);
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setInterpolationDuration(5);
            this.updateAnimation(0, 0, state.get(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(double speed, long worldTick, Direction.Axis axis) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.NORTH.getRotationQuaternion());
            }

            mat.rotateY(((float) speed * worldTick));

            mat.scale(2f);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                this.updateAnimation(RotationalSource.getNetworkSpeed(this.getAttachment().getWorld(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockPos()), tick,
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(AXIS));
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                }
            }
        }

        static {
            ITEM_MODEL.getOrCreateNbt().putInt("CustomModelData", SimpleModeledPolymerItem.MODELS.get(FactoryItems.AXLE_BLOCK).value());
            ITEM_MODEL_SHORT.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(ITEM_MODEL_SHORT.getItem(), FactoryUtil.id("block/axle_short")).value());
        }
    }
}
