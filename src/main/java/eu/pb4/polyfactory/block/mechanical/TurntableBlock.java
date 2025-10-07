package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.block.collection.BlockCollectionData;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class TurntableBlock extends RotationalNetworkBlock implements FactoryBlock,  BarrierBasedWaterloggable, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = Properties.FACING;

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
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
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
            if (speed != 0 && !(entity instanceof ServerPlayerEntity)) {
                var rotate = (float) (rot.isNegative() ? speed : -speed);
                entity.setYaw(entity.getYaw() + rotate);
                entity.setHeadYaw(entity.getHeadYaw() + rotate);

                //if (entity instanceof ServerPlayerEntity player) {
                //    player.networkHandler.requestTeleport(new EntityPosition(Vec3d.ZERO, Vec3d.ZERO, rotate, 0), EnumSet.allOf(PositionFlag.class));
                //}
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
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
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;
        private BlockCollection blocks;

        private Model(ServerWorld world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ItemDisplayElementUtil.getModel(state.getBlock().asItem()), this.getUpdateRate(), 0.3f, 0.6f);
            this.updateAnimation(0, state.get(FACING));
            this.addElement(this.mainElement);
            if (false) {
                this.blocks = new BlockCollection(BlockCollectionData.createDebug());
                this.blocks.setCenter(4, 0, 4);
                this.blocks.setOffset(Vec3d.of(state.get(FACING).getOpposite().getVector()));

                this.addElement(this.blocks);
            }
        }


        @Override
        public void setAttachment(@Nullable HolderAttachment attachment) {
            if (this.blocks != null) {
                this.blocks.setWorld(attachment != null ? attachment.getWorld() : null);
            }

            super.setAttachment(attachment);
        }

        @Override
        public void destroy() {
            super.destroy();
            if (this.blocks != null) {
                this.blocks.setWorld(null);
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
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
