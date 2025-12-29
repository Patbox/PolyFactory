package eu.pb4.polyfactory.block.other;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.block.SneakBypassingBlock;
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
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;


public class WirelessRedstoneBlock extends Block implements FactoryBlock, RedstoneConnectable, EntityBlock, ConfigurableBlock, SneakBypassingBlock, BarrierBasedWaterloggable {
    public static EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static BooleanProperty POWERED = BlockStateProperties.POWERED;
    public WirelessRedstoneBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(POWERED, false));
    }

    public static void send(ServerLevel world, BlockPos pos, int ticks, ItemStack key1, ItemStack key2) {
        world.getPoiManager().getInRange(x -> x.is(FactoryPoi.WIRELESS_REDSTONE_RECEIVED),
                pos, 64, PoiManager.Occupancy.ANY).forEach(poi -> {
                    var state = world.getBlockState(poi.getPos());
                    if (state.is(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER)
                            && world.getBlockEntity(poi.getPos()) instanceof WirelessRedstoneBlockEntity be && be.matches(key1, key2)) {
                        world.setBlockAndUpdate(poi.getPos(), state.setValue(POWERED, true));
                        if (ticks > 0) {
                            world.scheduleTick(poi.getPos(), state.getBlock(), ticks);
                        }
                        world.playSound(null, poi.getPos().getX() + 0.5, poi.getPos().getY() + 0.5, poi.getPos().getZ() + 0.5,
                                FactorySoundEvents.BLOCK_REMOTE_REDSTONE_ON, SoundSource.BLOCKS, 1, 1);

                        if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                            TriggerCriterion.trigger(player, FactoryTriggers.WIRELESS_REDSTONE);
                        }
                    }
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(POWERED);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be) {
            if (!player.isShiftKeyDown() && hit.getDirection() == state.getValue(FACING).getOpposite()) {
                return be.updateKey(player, hit, player.getMainHandItem()) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
            }

            if (player.isShiftKeyDown() && player.getMainHandItem().is(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                player.getMainHandItem().set(FactoryDataComponents.REMOTE_KEYS, new Pair<>(be.key1(), be.key2()));
                player.getCooldowns().addCooldown(player.getItemInHand(InteractionHand.MAIN_HAND), 5);
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useWithoutItem(state, world, pos, player, hit);
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.getValue(FACING).getOpposite() == dir;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessRedstoneBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Receiver extends WirelessRedstoneBlock {
        public Receiver(Properties settings) {
            super(settings);
        }

        @Override
        public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
            if (state.getValue(POWERED)) {
                world.setBlockAndUpdate(pos, state.setValue(POWERED, false));
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                        FactorySoundEvents.BLOCK_REMOTE_REDSTONE_OFF, SoundSource.BLOCKS, 1, 1);
            }

            super.tick(state, world, pos, random);
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
            return state.getValue(POWERED) && direction != state.getValue(FACING) ? 15 : 0;
        }
    }

    public static final class Transmitter extends WirelessRedstoneBlock {
        public Transmitter(Properties settings) {
            super(settings);
        }

        @Override
        protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
            if (!world.isClientSide()) {
                boolean bl = state.getValue(POWERED);
                if (bl != world.hasNeighborSignal(pos)) {
                    if (bl) {
                        world.scheduleTick(pos, this, 4);
                    } else {
                        world.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
                        if (world.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be) {
                            WirelessRedstoneBlock.send((ServerLevel) world, pos, 20, be.key1(), be.key2());
                            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                                    FactorySoundEvents.BLOCK_REMOTE_REDSTONE_ON, SoundSource.BLOCKS, 1, 1);
                        }
                    }
                }
            }
        }

        @Override
        public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
            if (state.getValue(POWERED) && !world.hasSignal(pos.relative(state.getValue(FACING)), state.getValue(FACING))) {
                world.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(),
                        FactorySoundEvents.BLOCK_REMOTE_REDSTONE_OFF, SoundSource.BLOCKS, 1, 1);
            }
        }

    }

    public final class Model extends BlockModel implements WirelessRedstoneBlockEntity.ItemUpdater {
        private final ItemDisplayElement main;
        private final ItemDisplayElement overlay;
        private final ItemDisplayElement key1;
        private final ItemDisplayElement key2;

        private Model(ServerLevel world, BlockPos pos, BlockState state) {
            this.main = ItemDisplayElementUtil.createSolid(WirelessRedstoneBlock.this.asItem());
            this.main.setScale(new Vector3f(2));

            this.key1 = ItemDisplayElementUtil.createSimple();
            this.key1.setDisplaySize(1, 1);
            this.key1.setItemDisplayContext(ItemDisplayContext.GUI);
            this.key1.setViewRange(0.3f);

            this.key2 = LodItemDisplayElement.createSimple();
            this.key2.setDisplaySize(1, 1);
            this.key2.setItemDisplayContext(ItemDisplayContext.GUI);
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
            var model = state.is(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER) ? RedstoneOutputBlock.Model.OUTPUT_OVERLAY
                    : RedstoneOutputBlock.Model.INPUT_OVERLAY;
            var stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(RedStoneWireBlock.getColorForPower(state.getValue(POWERED) ? 15 : 0))));
            stack.set(DataComponents.ITEM_MODEL, model);
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
            var dir = state.getValue(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
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
