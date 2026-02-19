package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.mixin.PropertiesAccessor;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MergedContainer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;


public class ItemOutputBufferBlock extends Block implements FactoryBlock, EntityBlock, ConfigurableBlock {
    public static EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    public static BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final ItemStack model;
    private final ItemStack modelOpen;

    public ItemOutputBufferBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(OPEN, false));
        var modelId = ((PropertiesAccessor) settings).getId().identifier().withPrefix("block/");
        this.model = ItemDisplayElementUtil.getSolidModel(modelId);
        this.modelOpen = ItemDisplayElementUtil.getSolidModel(modelId.withSuffix("_open"));
    }

    public static Container getOutputContainer(Container original, Level level, BlockPos pos, Iterable<Direction> directions) {
        var list = new ArrayList<Container>();
        list.add(original);
        findBuffers(level, pos, directions, x -> list.add(x.getOwnContainer()));
        return list.size() == 1 ? original : new MergedContainer(list);
    }

    public static Container getOutputContainer(Container original, Level level, BlockPos pos, Direction direction) {
        var list = new ArrayList<Container>();
        list.add(original);
        findBuffers(level, pos, direction, x -> list.add(x.getOwnContainer()));
        return list.size() == 1 ? original : new MergedContainer(list);
    }

    public static void findBuffers(Level level, BlockPos pos, Iterable<Direction> directions, Consumer<ItemOutputBufferBlockEntity> consumer) {
        var mut = pos.mutable();

        for (var dir : directions) {
            if (level.getBlockEntity(mut.set(pos).move(dir)) instanceof ItemOutputBufferBlockEntity buffer && buffer.getBlockState().getValue(ORIENTATION).front() == dir.getOpposite()) {
                consumer.accept(buffer);
            }
        }
    }

    public static void findBuffers(Level level, BlockPos pos, Direction direction, Consumer<ItemOutputBufferBlockEntity> consumer) {
        if (level.getBlockEntity(pos.relative(direction)) instanceof ItemOutputBufferBlockEntity buffer && buffer.getBlockState().getValue(ORIENTATION).front() == direction.getOpposite()) {
            consumer.accept(buffer);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ORIENTATION, OPEN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var front = context.getClickedFace().getOpposite();
        var top = switch (front) {
            case DOWN -> context.getHorizontalDirection();
            case UP -> context.getHorizontalDirection().getOpposite();
            default -> Direction.UP;
        };

        return super.getStateForPlacement(context).setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(front, top));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ItemOutputBufferBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            if (be.checkUnlocked(player)) {
                be.createGui(serverPlayer, state.getValue(ORIENTATION).front() == hit.getDirection());
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jspecify.annotations.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof ItemOutputBufferBlockEntity be && be.isConnected()) {
            TriggerCriterion.trigger(player, FactoryTriggers.ITEM_OUTPUT_BUFFER);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof ItemOutputBufferBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return  new ItemOutputBufferBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.ORIENTATION);
    }

    public final class Model extends BlockModel {
        private final ItemDisplayElement main;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setScale(new Vector3f(2f));
            this.main.setDisplaySize(5, 5);
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            this.main.setItem(state.getValue(OPEN) ? ItemOutputBufferBlock.this.modelOpen : ItemOutputBufferBlock.this.model);
            var orientation = state.getValue(ORIENTATION);
            var dir = orientation.front();
            float p = -90;
            float y = 0;

            if (dir.getAxis() == Direction.Axis.Y) {
                if (dir == Direction.DOWN) {
                    p = 90;
                }
                y = orientation.top().toYRot();
            } else {
                p = 0;
                y = dir.toYRot();
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}
