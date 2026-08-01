package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.block.AttackableBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInteractionMode;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.nodes.pipe.PumpNode;
import eu.pb4.polyfactory.util.RedstoneActivationType;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class DieselEngineBlock extends NetworkBlock implements FactoryBlock, EntityBlock, PipeConnectable, RotationUser, ConfigurableBlock, NetworkComponent.Rotational, NetworkComponent.Pipe, AttackableBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final BlockConfig<?> REDSTONE_ACTIVATION_CONFIG = BlockConfig.ofBlockEntity("redstone_activation", RedstoneActivationType.CODEC, DieselEngineBlockEntity.class,
            BlockValueFormatter.text(RedstoneActivationType::asName), DieselEngineBlockEntity::getRedstoneActivationType,
            DieselEngineBlockEntity::setRedstoneActivationType, WrenchModifyBlockValue.enums(RedstoneActivationType.values()));

    private static final BlockConfig<?> ENGINE_GEAR_CONFIG = BlockConfig.ofBlockEntity("engine_gear", Codec.INT, DieselEngineBlockEntity.class,
            BlockValueFormatter.str(String::valueOf), DieselEngineBlockEntity::getGear,
            DieselEngineBlockEntity::setGear, WrenchModifyBlockValue.simple(IntStream.rangeClosed(1, 5).boxed().toList()));

    private final Identifier model;
    private final Identifier modelUp;
    private final Identifier modelDown;

    public DieselEngineBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
        this.model = settings.blockIdOrThrow().identifier().withPrefix("block/");
        this.modelUp = this.model.withSuffix("_up");
        this.modelDown = this.model.withSuffix("_down");
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof DieselEngineBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() ? ctx.getClickedFace().getOpposite() : ctx.getNearestLookingDirection());
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onPlace(state, world, pos, oldState, notify);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
        NetworkComponent.Pipe.updatePipeAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Rotational || block instanceof NetworkComponent.Pipe;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updatePowered(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updatePowered(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlock(pos, state.setValue(POWERED, powered), 4);
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING)));
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new PumpNode(state.getValue(FACING), true, PumpNode.WEAK_RANGE));
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DieselEngineBlockEntity be) || hit.getDirection() != state.getValue(FACING).getOpposite() || stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        var x = FluidContainerUtil.interactWithInWorld(be.getMainFluidContainer(), player, stack, hand, FluidInteractionMode.ANY, FluidInteractionMode.INSERT);
        if (x == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        return x;
    }

    @Override
    public InteractionResult onPlayerAttack(BlockState state, Player player, Level world, BlockPos pos, Direction direction) {
        var itemStack = player.getMainHandItem();
        if (itemStack.is(FactoryItemTags.FLUID_CONTAINER_INTERACTABLE_ON_ATTACK) && world.getBlockEntity(pos) instanceof DieselEngineBlockEntity be) {
            var x = FluidContainerUtil.interactWithInWorld(be.getMainFluidContainer(), player, itemStack, InteractionHand.MAIN_HAND, FluidInteractionMode.ANY, FluidInteractionMode.EXTRACT);
            if (x != null) {
                return x;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof DieselEngineBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DieselEngineBlockEntity(pos, state);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING, REDSTONE_ACTIVATION_CONFIG, ENGINE_GEAR_CONFIG);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING).getOpposite() == dir;
    }

    @Override
    public @org.jspecify.annotations.Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return DieselEngineBlockEntity::tick;
    }

    public final class Model extends RotationAwareModel {
        private final ItemDisplayElement axle;
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.axle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT.get(), this.getUpdateRate(), 0.3f, 0.6f);
            this.base = ItemDisplayElementUtil.createSimple();
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, state.getValue(FACING));
            this.addElement(this.axle);
            this.addElement(this.base);
        }

        private void updateAnimation(float speed, Direction facing) {
            var mat = mat();
            mat.rotate(facing.getOpposite().getRotation());
            mat.rotateY((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? speed : -speed);

            mat.scale(2f);
            this.axle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                var facing = this.blockState().getValue(FACING);

                this.updateAnimation(this.getRotation(), facing);
                this.axle.startInterpolationIfDirty();
            }
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);

            this.base.setItem(ItemDisplayElementUtil.getModel(switch (dir) {
                case UP -> modelUp;
                case DOWN -> modelDown;
                default -> model;
            }).get());

            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
            //this.axle.setYaw(y);
            //this.axle.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }
    }
}
