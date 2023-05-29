package eu.pb4.polyfactory.block.storage;

import eu.pb4.polyfactory.block.AttackableBlock;
import eu.pb4.polyfactory.block.mechanical.WindmillBlockEntity;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;


public class ContainerBlock extends Block implements PolymerBlock, BlockEntityProvider, BlockWithElementHolder, AttackableBlock, VirtualDestroyStage.Marker {
    public static DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;

    public ContainerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ENABLED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && hand == Hand.MAIN_HAND && hit.getSide() == state.get(FACING)) {
            var stack = player.getStackInHand(hand);

            if (stack.isEmpty()) {
                if (be.isEmpty()) {
                    be.setItemStack(ItemStack.EMPTY);
                } else {
                    player.setStackInHand(hand, be.extractStack());
                }
            } else {
                if (be.getItemStack().isEmpty()) {
                    be.setItemStack(stack);
                    stack.decrement(be.addItems(stack.getCount()));
                } else if (be.matches(stack)) {
                    stack.decrement(be.addItems(stack.getCount()));
                }

                if (stack.isEmpty()) {
                    player.setStackInHand(hand, ItemStack.EMPTY);
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public ActionResult onPlayerAttack(BlockState state, PlayerEntity player, World world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && direction == state.get(FACING)) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be) {
                var count = be.storage.amount;
                var max = be.getItemStack().getMaxCount();
                while (count > 0) {
                    var stack = be.storage.variant.toStack((int) Math.min(max, count));
                    count -= stack.getCount();
                    ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);

    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be) {
            return (int) ((be.storage.amount * 15) / be.storage.getCapacity());
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends ElementHolder {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement itemElement;
        private final TextDisplayElement countElement;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = new ItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setItem(FactoryItems.CONTAINER_BLOCK.getDefaultStack());

            this.itemElement = new ItemDisplayElement();
            this.itemElement.setDisplaySize(1, 1);
            this.itemElement.setModelTransformation(ModelTransformationMode.GUI);

            this.countElement = new TextDisplayElement(Text.literal("0"));
            this.countElement.setDisplaySize(1, 1);
            this.countElement.setBackground(0);
            //this.countElement.setShadow(true);

            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.itemElement);
            this.addElement(this.countElement);
        }

        public void setDisplay(ItemStack stack, long count) {
            this.itemElement.setItem(stack);
            this.countElement.setText(Text.literal("" + count));

        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(0, 1f / 16f, -6.2f / 16f);
            mat.rotateY(MathHelper.PI);
            mat.scale(0.45f, 0.45f, 0.01f);
            this.itemElement.setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(0, -6.25f / 16f, -6.2f / 16f);
            mat.rotateY(MathHelper.PI);
            mat.scale(0.5f, 0.5f, 0.02f);
            this.countElement.setTransformation(mat);
            mat.popMatrix();

            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(BlockBoundAttachment.get(this).getBlockState());
            }
        }
    }
}
