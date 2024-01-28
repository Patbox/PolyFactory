package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.SneakBypassingBlock;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.FactoryPoi;
import eu.pb4.polyfactory.util.FactoryUtil;
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
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;


public class WirelessRedstoneReceiverBlock extends Block implements PolymerBlock, BlockEntityProvider, BlockWithElementHolder, SneakBypassingBlock, VirtualDestroyStage.Marker, BarrierBasedWaterloggable {
    public static DirectionProperty FACING = Properties.FACING;
    public static BooleanProperty POWERED = Properties.POWERED;
    public WirelessRedstoneReceiverBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    public static void send(ServerWorld world, BlockPos pos, int ticks, ItemStack key1, ItemStack key2) {
        world.getPointOfInterestStorage().getInCircle(x -> x.matchesKey(FactoryPoi.WIRELESS_REDSTONE_RECEIVED),
                pos, 64, PointOfInterestStorage.OccupationStatus.ANY).forEach(poi -> {
                    var state = world.getBlockState(poi.getPos());
                    if (state.isOf(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                            && world.getBlockEntity(poi.getPos()) instanceof WirelessRedstoneBlockEntity be && be.matches(key1, key2)) {
                        world.setBlockState(poi.getPos(), state.with(POWERED, true));
                        if (ticks > 0) {
                            world.scheduleBlockTick(poi.getPos(), state.getBlock(), ticks);
                        }
                    }
        });
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, false));
        }

        super.scheduledTick(state, world, pos, random);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
        builder.add(POWERED);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be && hand == Hand.MAIN_HAND && hit.getSide() == state.get(FACING)) {

            return be.updateKey(player, hit, player.getMainHandStack()) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) && direction != state.get(FACING).getOpposite() ? 15 : 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
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
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.STONE.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessRedstoneBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends BlockModel implements WirelessRedstoneBlockEntity.ItemUpdater {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement key1;
        private final ItemDisplayElement key2;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(WirelessRedstoneReceiverBlock.this.asItem());

            this.key1 = LodItemDisplayElement.createSimple();
            this.key1.setDisplaySize(1, 1);
            this.key1.setModelTransformation(ModelTransformationMode.GUI);
            this.key1.setViewRange(0.3f);

            this.key2 = LodItemDisplayElement.createSimple();
            this.key2.setDisplaySize(1, 1);
            this.key2.setModelTransformation(ModelTransformationMode.GUI);
            this.key2.setViewRange(0.3f);

            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.key1);
            this.addElement(this.key2);
        }

        @Override
        public void updateItems(ItemStack key1, ItemStack key2) {
            this.key1.setItem(key1.copy());
            this.key2.setItem(key2.copy());
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(MathHelper.PI);
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(0, 4f / 16f, -2.2f / 16f);
            mat.scale(0.45f, 0.45f, 0.01f);
            this.key1.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(0, -4f / 16f, -2.2f / 16f);
            mat.scale(0.45f, 0.45f, 0.01f);
            this.key2.setTransformation(mat);
            mat.popMatrix();

            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }
}
