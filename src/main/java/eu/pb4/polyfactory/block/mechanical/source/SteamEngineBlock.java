package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

public class SteamEngineBlock extends MultiBlock implements FactoryBlock, EntityBlock, WorldlyContainerHolder, RotationUser {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SteamEngineBlock(Properties settings) {
        super(2, 3, 2, settings);
        Model.AXLE.getItem();
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (getY(state) != 2 && !player.isShiftKeyDown() && world.getBlockEntity(getCenter(state, pos)) instanceof SteamEngineBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public int getMaxX(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? 0 : 1;
    }

    @Override
    public int getMaxZ(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? 0 : 1;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        if (world.getBlockEntity(center) instanceof SteamEngineBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }


    @Override
    protected void onPlacedMultiBlock(Level world, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (getY(state) == 2) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (getY(state) == 2) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        if (getY(state) == 2) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return isCenter(initialBlockState) ? new Model(initialBlockState) : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCenter(state) ? new SteamEngineBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return isCenter(state) ? SteamEngineBlockEntity::tick : null;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        var be = world.getBlockEntity(center);

        return be instanceof WorldlyContainer inv ? inv : null;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return getY(state) == 2 ? List.of(getX(state) == 0 && getZ(state) == 0 ?
                new FunctionalAxisNode(state.getValue(FACING).getCounterClockWise().getAxis()) : new SimpleAxisNode(state.getValue(FACING).getCounterClockWise().getAxis())) : List.of();
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack AXLE = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/steam_engine_axle"));
        public static final ItemStack LIT = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/steam_engine_lit"));
        public static final ItemStack LINK = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/steam_engine_link"));

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement main;
        private final ItemDisplayElement rotatingA;
        private final ItemDisplayElement rotatingB;
        private final LodItemDisplayElement axle;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getValue(SteamEngineBlock.LIT) ? LIT : FactoryItems.STEAM_ENGINE.getDefaultInstance());
            this.main.setScale(new Vector3f(2));
            var facing = state.getValue(FACING);
            var offset = new Vec3(
                    facing.getAxis() == Direction.Axis.Z ? 0.5f : 0,
                    -1,
                    facing.getAxis() == Direction.Axis.X ? 0.5f : 0
            );
            this.main.setOffset(offset);
            this.main.setDisplayWidth(3);
            offset = offset.add(0, 2, 0);
            this.axle = LodItemDisplayElement.createSimple(AXLE, 4);
            this.axle.setOffset(offset);
            this.axle.setScale(new Vector3f(2));
            this.axle.setDisplayWidth(3);
            this.rotatingA = LodItemDisplayElement.createSimple(LINK, 4);
            this.rotatingA.setOffset(offset);
            this.rotatingA.setDisplayWidth(3);
            this.rotatingB = LodItemDisplayElement.createSimple(LINK, 4);
            this.rotatingB.setOffset(offset);
            this.rotatingB.setDisplayWidth(3);


            this.updateStatePos(state);
            var dir = state.getValue(FACING);
            this.updateAnimation(0, (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.main);
            this.addElement(this.rotatingA);
            this.addElement(this.rotatingB);
            this.addElement(this.axle);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(FACING);

            this.main.setYaw(direction.toYRot());
            this.main.setItem(state.getValue(SteamEngineBlock.LIT) ? LIT : FactoryItems.STEAM_ENGINE.getDefaultInstance());
            this.axle.setYaw(direction.toYRot());
            this.rotatingA.setYaw(direction.toYRot());
            this.rotatingB.setYaw(direction.toYRot());
        }

        private void updateAnimation(float rotation, boolean negative) {
            rotation = negative ? rotation : -rotation;
            this.axle.setLeftRotation(new Quaternionf().rotateX(rotation));

            var sin = Mth.sin(rotation);
            var cos = Mth.cos(rotation);
            var sin2 = Mth.sin(rotation - Mth.HALF_PI) * 0.6f;

            mat.identity()
                    .translate(-8f / 16, sin * -10 / 16f, cos * 10 / 16f)
                    .rotateX(-sin2)
            ;

            this.rotatingA.setTransformation(mat);

            mat.identity()
                    .translate(8f / 16, sin * 10 / 16f, -cos * 10 / 16f)
                    .rotateX(sin2)
            ;

            this.rotatingB.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        @Override
        protected void onTick() {
            if (this.getTick() % this.getUpdateRate() == 0) {
                var dir = this.blockState().getValue(FACING);
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation(),
                        (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
                //if (this.whisk.isDirty()) {
                //    this.whisk.startInterpolation();
                //}

                this.axle.startInterpolationIfDirty();
                this.rotatingA.startInterpolationIfDirty();
                this.rotatingB.startInterpolationIfDirty();
            }
        }
    }
}
