package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class SteamEngineBlock extends MultiBlock implements FactoryBlock, BlockEntityProvider, InventoryProvider, RotationUser {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public SteamEngineBlock(Settings settings) {
        super(2, 3, 2, settings);
        Model.AXLE.getItem();
        this.setDefaultState(this.getDefaultState().with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        super.appendProperties(builder);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (getY(state) != 2 && hand == Hand.MAIN_HAND && !player.isSneaking() && world.getBlockEntity(getCenter(state, pos)) instanceof SteamEngineBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
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
    protected int getMaxX(BlockState state) {
        return state.get(FACING).getAxis() == Direction.Axis.X ? 0 : 1;
    }

    @Override
    protected int getMaxZ(BlockState state) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? 0 : 1;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        if (world.getBlockEntity(center) instanceof SteamEngineBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }


    @Override
    protected void onPlacedMultiBlock(World world, BlockPos pos, BlockState state, PlayerEntity player, ItemStack stack) {
        if (getY(state) == 2) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (isCenter(state) && !newState.isOf(state.getBlock())) {
            var be = world.getBlockEntity(pos);
            if (be instanceof Inventory inventory) {
                ItemScatterer.spawn(world, pos, inventory);
            }
        }

        if (getY(state) == 2) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
        }
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return isCenter(initialBlockState) ? new Model(initialBlockState) : null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return isCenter(state) ? new SteamEngineBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return isCenter(state) ? SteamEngineBlockEntity::tick : null;
    }

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        var be = world.getBlockEntity(center);

        return be instanceof SidedInventory inv ? inv : null;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.DEEPSLATE_BRICKS.getDefaultState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return getY(state) == 2 ? List.of(getX(state) == 0 && getZ(state) == 0 ?
                new FunctionalAxisNode(state.get(FACING).rotateYCounterclockwise().getAxis()) : new SimpleAxisNode(state.get(FACING).rotateYCounterclockwise().getAxis())) : List.of();
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack AXLE = new ItemStack(BaseItemProvider.requestModel());
        public static final ItemStack LIT = new ItemStack(BaseItemProvider.requestModel());
        public static final ItemStack LINK = new ItemStack(BaseItemProvider.requestModel());

        static {
            AXLE.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(AXLE.getItem(), FactoryUtil.id("block/steam_engine_axle")).value());
            LINK.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(LINK.getItem(), FactoryUtil.id("block/steam_engine_link")).value());
            LIT.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(LIT.getItem(), FactoryUtil.id("block/steam_engine_lit")).value());
        }

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement main;
        private final ItemDisplayElement rotatingA;
        private final ItemDisplayElement rotatingB;
        private final LodItemDisplayElement axle;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.get(SteamEngineBlock.LIT) ? LIT : FactoryItems.STEAM_ENGINE.getDefaultStack());
            this.main.setScale(new Vector3f(2));
            var facing = state.get(FACING);
            var offset = new Vec3d(
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
            var dir = state.get(FACING);
            this.updateAnimation(0, (dir.getDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.main);
            this.addElement(this.rotatingA);
            this.addElement(this.rotatingB);
            this.addElement(this.axle);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.get(FACING);

            this.main.setYaw(direction.asRotation());
            this.main.setItem(state.get(SteamEngineBlock.LIT) ? LIT : FactoryItems.STEAM_ENGINE.getDefaultStack());
            this.axle.setYaw(direction.asRotation());
            this.rotatingA.setYaw(direction.asRotation());
            this.rotatingB.setYaw(direction.asRotation());
        }

        private void updateAnimation(float rotation, boolean negative) {
            rotation = negative ? rotation : -rotation;
            this.axle.setLeftRotation(new Quaternionf().rotateX(rotation));

            var sin = MathHelper.sin(rotation);
            var cos = MathHelper.cos(rotation);
            var sin2 = MathHelper.sin(rotation - MathHelper.HALF_PI) * 0.6f;

            mat.identity()
                    .translate(-8f / 16,  sin * -10 / 16f, cos * 10 / 16f)
                    .rotateX(-sin2)
            ;

            this.rotatingA.setTransformation(mat);

            mat.identity()
                    .translate(8f / 16,  sin * 10 / 16f, -cos * 10 / 16f)
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
                var dir = this.blockState().get(FACING);
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().up()).rotation(),
                        (dir.getDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
                //if (this.whisk.isDirty()) {
                //    this.whisk.startInterpolation();
                //}

                if (this.axle.isDirty()) {
                    this.axle.startInterpolation();
                    this.rotatingA.startInterpolation();
                    this.rotatingB.startInterpolation();
                }
            }
        }
    }
}
