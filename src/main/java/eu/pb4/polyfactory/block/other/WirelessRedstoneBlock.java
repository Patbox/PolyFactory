package eu.pb4.polyfactory.block.other;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.*;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.FactoryPoi;
import eu.pb4.polyfactory.block.data.output.RedstoneOutputBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;


public class WirelessRedstoneBlock extends Block implements FactoryBlock, RedstoneConnectable, BlockEntityProvider, ConfigurableBlock, SneakBypassingBlock, BarrierBasedWaterloggable, ItemUseLimiter.All {
    public static EnumProperty<Direction> FACING = Properties.FACING;
    public static BooleanProperty POWERED = Properties.POWERED;
    public WirelessRedstoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false).with(POWERED, false));
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
                        world.playSound(null, poi.getPos().getX() + 0.5, poi.getPos().getY() + 0.5, poi.getPos().getZ() + 0.5,
                                FactorySoundEvents.BLOCK_REMOTE_REDSTONE_ON, SoundCategory.BLOCKS, 1, 1);

                        if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                            TriggerCriterion.trigger(player, FactoryTriggers.WIRELESS_REDSTONE);
                        }
                    }
        });
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
    public boolean preventUseItemWhileTargetingBlock(ServerPlayerEntity player, BlockState blockState, World world, BlockHitResult result, ItemStack stack, Hand hand) {
        return !player.shouldCancelInteraction() && result.getSide() == blockState.get(FACING).getOpposite();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be) {
            if (!player.isSneaking() && hit.getSide() == state.get(FACING).getOpposite()) {
                return be.updateKey(player, hit, player.getMainHandStack()) ? ActionResult.SUCCESS : ActionResult.FAIL;
            }

            if (player.isSneaking() && player.getMainHandStack().isOf(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                player.getMainHandStack().set(FactoryDataComponents.REMOTE_KEYS, new Pair<>(be.key1(), be.key2()));
                player.getItemCooldownManager().set(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, 5);
                return ActionResult.SUCCESS;
            }
        }

        return super.onUse(state, world, pos, player, hit);
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
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.get(FACING).getOpposite() == dir;
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

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Receiver extends WirelessRedstoneBlock {
        public Receiver(Settings settings) {
            super(settings);
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, false));
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                        FactorySoundEvents.BLOCK_REMOTE_REDSTONE_OFF, SoundCategory.BLOCKS, 1, 1);
            }

            super.scheduledTick(state, world, pos, random);
        }

        @Override
        public boolean emitsRedstonePower(BlockState state) {
            return true;
        }

        @Override
        public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
            return state.get(POWERED) && direction != state.get(FACING) ? 15 : 0;
        }
    }

    public static final class Transmitter extends WirelessRedstoneBlock {
        public Transmitter(Settings settings) {
            super(settings);
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
            if (!world.isClient) {
                boolean bl = state.get(POWERED);
                if (bl != world.isReceivingRedstonePower(pos)) {
                    if (bl) {
                        world.scheduleBlockTick(pos, this, 4);
                    } else {
                        world.setBlockState(pos, state.cycle(POWERED), Block.NOTIFY_LISTENERS);
                        if (world.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be) {
                            WirelessRedstoneBlock.send((ServerWorld) world, pos, 20, be.key1(), be.key2());
                            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                                    FactorySoundEvents.BLOCK_REMOTE_REDSTONE_ON, SoundCategory.BLOCKS, 1, 1);
                        }
                    }
                }
            }
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            if (state.get(POWERED) && !world.isEmittingRedstonePower(pos.offset(state.get(FACING)), state.get(FACING))) {
                world.setBlockState(pos, state.cycle(POWERED), Block.NOTIFY_LISTENERS);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                        FactorySoundEvents.BLOCK_REMOTE_REDSTONE_OFF, SoundCategory.BLOCKS, 1, 1);
            }
        }

    }

    public final class Model extends BlockModel implements WirelessRedstoneBlockEntity.ItemUpdater {
        private final ItemDisplayElement main;
        private final ItemDisplayElement overlay;
        private final ItemDisplayElement key1;
        private final ItemDisplayElement key2;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(WirelessRedstoneBlock.this.asItem());
            this.main.setScale(new Vector3f(2));

            this.key1 = ItemDisplayElementUtil.createSimple();
            this.key1.setDisplaySize(1, 1);
            this.key1.setModelTransformation(ModelTransformationMode.GUI);
            this.key1.setViewRange(0.3f);

            this.key2 = LodItemDisplayElement.createSimple();
            this.key2.setDisplaySize(1, 1);
            this.key2.setModelTransformation(ModelTransformationMode.GUI);
            this.key2.setViewRange(0.3f);

            this.key1.setScale(new Vector3f(4 / 16f, 4 / 16f, 0.01f));
            this.key1.setTranslation(new Vector3f(0, 2.5f / 16f, -2.2f / 16f));

            this.key2.setScale(new Vector3f(4 / 16f, 4 / 16f, 0.01f));
            this.key2.setTranslation(new Vector3f(0, -2.5f / 16f, -2.2f / 16f));

            this.overlay = ItemDisplayElementUtil.createSimple(createOverlay(state));
            this.overlay.setScale(new Vector3f(2.001f));
            this.overlay.setViewRange(0.6f);

            this.updateStatePos(state);

            this.addElement(this.main);
            this.addElement(this.overlay);
            this.addElement(this.key1);
            this.addElement(this.key2);
        }

        private ItemStack createOverlay(BlockState state) {
            var model = state.isOf(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER) ? RedstoneOutputBlock.Model.OUTPUT_OVERLAY
                    : RedstoneOutputBlock.Model.INPUT_OVERLAY;
            var stack = new ItemStack(model.item());
            stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(RedstoneWireBlock.getWireColor(state.get(POWERED) ? 15 : 0), false));
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(model.value()));
            return stack;
        }

        @Override
        public void updateItems(ItemStack key1, ItemStack key2) {
            this.key1.setItem(key1.copy());
            this.key2.setItem(key2.copy());
            this.key1.tick();
            this.key2.tick();
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


            this.main.setYaw(y + 180);
            this.main.setPitch(-p);
            this.key1.setYaw(y);
            this.key1.setPitch(p);
            this.key2.setYaw(y);
            this.key2.setPitch(p);
            this.overlay.setYaw(y);
            this.overlay.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                updateStatePos(state);
                this.overlay.setItem(createOverlay(state));
                this.main.tick();
                this.key1.tick();
                this.key2.tick();
                this.overlay.tick();
            }
        }
    }
}
