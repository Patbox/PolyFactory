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
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class TurntableBlock extends RotationalNetworkBlock implements FactoryBlock,  BarrierBasedWaterloggable, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public TurntableBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }


    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING)));
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(FACING) == Direction.DOWN) {
            float mult = 1;

            if (entity instanceof LivingEntity livingEntity) {
                mult = FactoryEnchantments.getMultiplier(livingEntity, FactoryEnchantmentEffectComponents.CONVEYOR_PUSH_MULTIPLIER);
                if (mult <= 0) {
                    return;
                }
            }
            var rot = RotationUser.getRotation(world, pos);
            var speed = Math.min(rot.speed(), 1028f) * mult;
            if (speed != 0 && !(entity instanceof ServerPlayer)) {
                var rotate = (float) (rot.isNegative() ? speed : -speed);
                entity.setYRot(entity.getYRot() + rotate);
                entity.setYHeadRot(entity.getYHeadRot() + rotate);

                //if (entity instanceof ServerPlayerEntity player) {
                //    player.networkHandler.requestTeleport(new EntityPosition(Vec3d.ZERO, Vec3d.ZERO, rotate, 0), EnumSet.allOf(PositionFlag.class));
                //}
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;
        private BlockCollection blocks;

        private Model(ServerLevel world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ItemDisplayElementUtil.getSolidModel(state.getBlock().asItem()), this.getUpdateRate(), 0.3f, 0.6f);
            this.updateAnimation(0, state.getValue(FACING));
            this.addElement(this.mainElement);
            if (false) {
                this.blocks = new BlockCollection(BlockCollectionData.createDebug());
                this.blocks.setCenter(4, 0, 4);
                this.blocks.setOffset(Vec3.atLowerCornerOf(state.getValue(FACING).getOpposite().getUnitVec3i()));

                this.addElement(this.blocks);
            }
        }


        @Override
        public void setAttachment(@Nullable HolderAttachment attachment) {
            if (this.blocks != null) {
                this.blocks.setLevel(attachment != null ? attachment.getWorld() : null);
            }

            super.setAttachment(attachment);
        }

        @Override
        public void destroy() {
            super.destroy();
            if (this.blocks != null) {
                this.blocks.setLevel(null);
            }
        }

        private void updateAnimation(float speed, Direction facing) {
            var mat = mat();
            mat.rotate(facing.getOpposite().getRotation());
            mat.rotateY(((float) ((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? speed : -speed)));
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();
            var facing = this.blockState().getValue(FACING);
            var rotation = this.getRotation();
            if (this.blocks != null) {
                this.blocks.setQuaternion(new Quaternionf().rotateAxis(
                        ((float) ((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? rotation : -rotation)),
                        facing.getOpposite().step()));
                this.blocks.tick();
            }

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(rotation, facing);
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
