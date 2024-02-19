package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class MCrafterBlock extends RotationalNetworkBlock implements FactoryBlock, BlockEntityProvider, RotationUser {
    public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

    public MCrafterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }
    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(state.get(FACING).rotateYClockwise().getAxis()));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.CRAFTER.getDefaultState();
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof MCrafterBlockEntity be ? ScreenHandler.calculateComparatorOutput((Inventory) be) : 0;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND && !player.isSneaking() && world.getBlockEntity(pos) instanceof MCrafterBlockEntity be) {
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
        return new MCrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.CRAFTER ? MCrafterBlockEntity::ticker : null;
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }



    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof MCrafterBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public static class Model extends BlockModel {
        private final LodItemDisplayElement base;

        private Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.base.tick();
            }
        }
    }
}
