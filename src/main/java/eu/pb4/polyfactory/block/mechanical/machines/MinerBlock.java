package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.DirectionalRotationUserNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;

public class MinerBlock extends RotationalNetworkBlock implements PolymerBlock, BlockEntityProvider, RotationUser, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public static final Property<Direction> FACING = Properties.FACING;

    public MinerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new DirectionalRotationUserNode(state.get(FACING).getOpposite()));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.DISPENSER.getDefaultState();
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof MinerBlockEntity be && be.getStack().isDamageable()
                ? (int) (16d * be.getStack().getDamage() / be.getStack().getMaxDamage()) : 0;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer instanceof PlayerEntity player && world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            be.owner = player.getGameProfile();
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND && !player.isSneaking() && world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
            }
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.MINER ? MinerBlockEntity::ticker : null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, FACING);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public final class Model extends BaseModel {
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement item;
        private final ItemDisplayElement main;
        private float rotation = 0;

        private Model(ServerWorld world, BlockState state) {
            this.main = LodItemDisplayElement.createSimple(FactoryItems.MINER_BLOCK.getDefaultStack());
            this.item = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 1, 0.5f);

            this.updateAnimation(state.get(FACING));
            this.addElement(this.main);
            this.addElement(this.item);
        }

        private void updateAnimation(Direction direction) {
            mat.identity();
            mat.rotate(direction.getOpposite().getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion()));
            mat.scale(2f);
            this.main.setTransformation(mat);

            mat.rotateY(MathHelper.HALF_PI);
            mat.scale(0.5f);


            if (this.item.getItem().getItem() instanceof ToolItem) {
                mat.translate(-0.1f, 0.25f, 0);
                mat.translate(-0.25f, -0.25f, 0);
                mat.rotateZ(this.rotation);
                mat.translate(0.25f, 0.25f, 0);
            } else {
                mat.translate(-0.3f, 0, 0);
                mat.rotateZ(this.rotation);
            }

            this.item.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            this.updateAnimation(((BlockBoundAttachment) this.getAttachment()).getBlockState().get(FACING));
            if (this.item.isDirty()) {
                this.item.startInterpolation();
            }
        }

        public void setItem(ItemStack stack) {
            this.item.setItem(stack);
        }

        public void rotate(float value) {
            this.rotation += value;
            if (this.rotation > MathHelper.TAU) {
                this.rotation -= MathHelper.TAU;
            }
        }
    }
}
