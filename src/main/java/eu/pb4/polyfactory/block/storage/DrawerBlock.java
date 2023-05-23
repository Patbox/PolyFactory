package eu.pb4.polyfactory.block.storage;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;


public class DrawerBlock extends Block implements PolymerBlock, BlockEntityProvider, BlockWithElementHolder {
    public static DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;

    public DrawerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ENABLED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var be = world.getBlockEntity(pos);

        if (be instanceof DrawerBlockEntity drawerBlockEntity && hand == Hand.MAIN_HAND) {
            var stack = player.getStackInHand(hand);

            if (stack.isEmpty()) {
                if (drawerBlockEntity.isEmpty()) {
                    drawerBlockEntity.setItemStack(ItemStack.EMPTY);
                } else {
                    player.setStackInHand(hand, drawerBlockEntity.extractStack());
                }
            } else {
                if (drawerBlockEntity.getItemStack().isEmpty()) {
                    drawerBlockEntity.setItemStack(stack);
                    stack.decrement(drawerBlockEntity.addItems(stack.getCount()));
                } else if (drawerBlockEntity.matches(stack)) {
                    stack.decrement(drawerBlockEntity.addItems(stack.getCount()));
                }

                if (stack.isEmpty()) {
                    player.setStackInHand(hand, ItemStack.EMPTY);
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
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

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DrawerBlockEntity(pos, state);
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
            this.mainElement.setItem(FactoryItems.DRAWER_BLOCK.getDefaultStack());

            this.itemElement = new ItemDisplayElement();
            this.itemElement.setDisplaySize(1, 1);
            this.itemElement.setModelTransformation(ModelTransformationMode.GUI);

            this.countElement = new TextDisplayElement(Text.literal("0"));
            this.countElement.setDisplaySize(1, 1);
            this.countElement.setBackground(0);
            this.countElement.setShadow(true);

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
            mat.translate(0, 0, -0.51f);
            mat.rotateY(MathHelper.PI);
            mat.scale(0.5f, 0.5f, 0.02f);
            this.itemElement.setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(0, -0.4f, -0.51f);
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
