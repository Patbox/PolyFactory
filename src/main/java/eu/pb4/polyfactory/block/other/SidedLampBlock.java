package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.QuickWaterloggable;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class SidedLampBlock extends Block implements FactoryBlock, EntityBlock, BlockStateNameProvider, QuickWaterloggable, PolymerBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private final Identifier onModel;
    private final Identifier offModel;

    public SidedLampBlock(Properties settings, Identifier onModel, Identifier offModel) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false).setValue(WATERLOGGED, false));
        this.onModel = onModel;
        this.offModel = offModel;
    }

    public SidedLampBlock(Properties settings, Identifier id, boolean inverted) {
        this(settings, inverted ? id : id.withPrefix("inverted_"), inverted ? id.withPrefix("inverted_") : id);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(LIT).add(FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.GLASS.defaultBlockState();
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState()
                .setValue(FACING, ctx.getClickedFace())
                .setValue(LIT, ctx.getLevel().hasSignal(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()), ctx.getClickedFace().getOpposite()))
        );
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
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
        if (!world.isClientSide()) {
            boolean bl = state.getValue(LIT);
            if (bl != world.hasSignal(pos.relative(state.getValue(FACING).getOpposite()), state.getValue(FACING).getOpposite())) {
                if (bl) {
                    world.scheduleTick(pos, this, 4);
                } else {
                    world.setBlock(pos, state.cycle(LIT), 2);
                }
            }
        }
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        var dir = state.getValue(FACING);
        if (state.getValue(LIT) != world.hasSignal(pos.relative(dir.getOpposite()), dir.getOpposite())) {
            world.setBlock(pos, state.cycle(LIT), 2);
        }
    }

    public boolean setColor(Level world, BlockPos pos, int color) {
        color = FactoryItems.LAMP.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            return true;
        }

        return false;
    }

    @Override
    public boolean forceLightUpdates(BlockState blockState) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var stack = super.getCloneItemStack(world, pos, state, includeData);
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be) {
            be.setColor(FactoryItems.LAMP.getItemColor(itemStack));
        }

        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(pos, initialBlockState);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColorableBlockEntity(pos, state);
    }

    @Override
    public Component getName(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Component.translatable(this.getDescriptionId() + ".colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Component.translatable(this.getDescriptionId() + ".colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    public static final class Full extends SidedLampBlock implements BarrierBasedWaterloggable {
        public Full(Properties settings, Identifier id, boolean inverted) {
            super(settings, id, inverted);
        }
    }

    public static final class Flat extends SidedLampBlock implements PolymerTexturedBlock {
        public Flat(Properties settings, Identifier id, boolean inverted) {
            super(settings, id, inverted);
        }

        @Override
        public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return (state.getValue(WATERLOGGED) ? FactoryUtil.TRAPDOOR_WATERLOGGED : FactoryUtil.TRAPDOOR_REGULAR).get(state.getValue(FACING));
        }
    }


    public final class Model extends BlockModel implements ColorProvider.Consumer{
        private final ItemDisplayElement main;
        private int color = -2;
        private BlockState state;

        private Model(BlockPos pos, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setScale(new Vector3f(2));
            this.main.setViewRange(0.8f);
            this.main.setRightRotation(new Quaternionf().rotateX(Mth.HALF_PI));
            this.state = state;
            updateStatePos(state);
            this.addElement(this.main);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        private void setState(BlockState blockState) {
            this.state = blockState;
            updateStatePos(blockState);
            if (color != -2) {
                updateModel();
            }
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            } else {
                p = -90;
            }


            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        private void updateModel() {
            var stack = new ItemStack(Items.FIREWORK_STAR);
            stack.set(DataComponents.ITEM_MODEL, this.state.getValue(LIT) ? onModel : offModel);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(this.color)));
            this.main.setItem(stack);
            this.tick();
        }

        public void setColor(int color) {
            this.color = color;
            if (color != -2) {
                updateModel();
            }
        }
    }
}
