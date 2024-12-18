package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlockEntity;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class TurntableBlock extends RotationalNetworkBlock implements FactoryBlock,  BarrierBasedWaterloggable, WrenchableBlock {
    public static final DirectionProperty FACING = Properties.FACING;

    public TurntableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide().getOpposite()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
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
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING)));
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (state.get(FACING) == Direction.DOWN) {
            float mult = 1;

            if (entity instanceof LivingEntity livingEntity) {
                mult = FactoryEnchantments.getMultiplier(livingEntity, FactoryEnchantmentEffectComponents.CONVEYOR_PUSH_MULTIPLIER);
                if (mult <= 0) {
                    return;
                }
            }
            var rot = RotationUser.getRotation(world, pos);
            var speed = Math.min(rot.speed(), 1028f) * mult;
            if (speed != 0) {
                var rotate = (float) (rot.isNegative() ? speed : -speed);
                entity.setYaw(entity.getYaw() + rotate);
                entity.setHeadYaw(entity.getHeadYaw() + rotate);

                if (entity instanceof ServerPlayerEntity player) {
                    player.networkHandler.sendPacket(new PlayerPositionLookS2CPacket(0,0, 0,
                            MathHelper.clamp(rotate, -30, 30), 0, Set.of(PositionFlag.values()), -1));
                }
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.STRIPPED_OAK_LOG.getDefaultState();
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;
        private BlockCollection blocks;

        private Model(ServerWorld world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ItemDisplayElementUtil.getModel(state.getBlock().asItem()), this.getUpdateRate(), 0.3f, 0.6f);
            this.updateAnimation(0, state.get(FACING));
            this.addElement(this.mainElement);
            if (false) {
                this.blocks = new BlockCollection(4, 4, 4);

                this.blocks.setBlockState(0, 0, 0, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(0, 1, 0, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(0, 2, 0, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(0, 2, 1, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(1, 2, 1, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(1, 2, 2, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(2, 2, 2, Blocks.STONE.getDefaultState(), null);
                this.blocks.setBlockState(2, 3, 2, FactoryBlocks.AXLE.getDefaultState(), null);
                this.blocks.setOffset(Vec3d.of(state.get(FACING).getOpposite().getVector()));

                this.addElement(this.blocks);
            }
        }

        private void updateAnimation(float speed, Direction facing) {
            var mat = mat();
            mat.rotate(facing.getOpposite().getRotationQuaternion());
            mat.rotateY(((float) ((facing.getDirection() == Direction.AxisDirection.NEGATIVE) ? speed : -speed)));
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();
            var facing = this.blockState().get(FACING);
            var rotation = this.getRotation();
            if (this.blocks != null) {
                this.blocks.setQuaternion(new Quaternionf().rotateAxis(
                        ((float) ((facing.getDirection() == Direction.AxisDirection.NEGATIVE) ? rotation : -rotation)),
                        facing.getOpposite().getUnitVector()));
                this.blocks.tick();
            }

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(rotation, facing);
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                }
            }
        }
    }
}
