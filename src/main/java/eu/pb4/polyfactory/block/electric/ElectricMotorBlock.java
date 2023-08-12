package eu.pb4.polyfactory.block.electric;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;

public class ElectricMotorBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder, RotationUser {
    public static final DirectionProperty FACING = Properties.FACING;

    public ElectricMotorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        modifier.provide(state.get(Properties.AGE_25) * 4, 1000f);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.AGE_25);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayer() != null && ctx.getPlayer().isSneaking() ? ctx.getSide() : ctx.getSide().getOpposite());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.PISTON;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.PISTON.getDefaultState().with(PistonBlock.FACING, state.get(FACING)).with(PistonBlock.EXTENDED, true);
    }


    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING)));
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    private final class Model extends ElementHolder {
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement mainElement;

        private Model(BlockState state) {
            this.mainElement = new ItemDisplayElement(AxleBlock.Model.ITEM_MODEL);
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setInterpolationDuration(4);

            var positive = state.get(FACING).getDirection() == Direction.AxisDirection.POSITIVE;

            if (state.get(FACING).getAxis() == Direction.Axis.Z) {
                positive = !positive;
            }

            this.updateAnimation(0, state.get(FACING).getAxis(), positive);
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis, boolean positive) {
            mat.identity();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.NORTH.getRotationQuaternion());
            }

            mat.rotateY(rotation);
            mat.scale(2, 1.95f, 2);
            mat.translate(0, positive ? 0.0075f : -0.0075f, 0);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                var facing = ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(FACING);

                var positive = facing.getDirection() == Direction.AxisDirection.POSITIVE;

                if (facing.getAxis() == Direction.Axis.Z) {
                    positive = !positive;
                }

                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation(),
                        facing.getAxis(),
                        positive

                );
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                }
            }
        }
    }
}
