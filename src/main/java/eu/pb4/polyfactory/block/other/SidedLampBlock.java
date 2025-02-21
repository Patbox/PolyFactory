package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.QuickWaterloggable;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class SidedLampBlock extends Block implements FactoryBlock, BlockEntityProvider, BlockStateNameProvider, QuickWaterloggable, PolymerBlock {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty LIT = Properties.LIT;
    private final ItemStack onModel;
    private final ItemStack offModel;
    public static final DirectionProperty FACING = Properties.FACING;

    public SidedLampBlock(Settings settings, Identifier onModel, Identifier offModel) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false).with(WATERLOGGED, false));
        this.onModel = BaseItemProvider.requestModel(Items.FIREWORK_STAR, onModel.withPrefixedPath("item/"));
        this.offModel = BaseItemProvider.requestModel(Items.FIREWORK_STAR, offModel.withPrefixedPath("item/"));
    }

    public SidedLampBlock(Settings settings, Identifier id, boolean inverted) {
        this(settings, inverted ? id : id.withPrefixedPath("inverted_"), inverted ? id.withPrefixedPath("inverted_") : id);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(LIT).add(FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity context) {
        return Blocks.GLASS.getDefaultState();
    }

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState()
                .with(FACING, ctx.getSide())
                .with(LIT, ctx.getWorld().isEmittingRedstonePower(ctx.getBlockPos().offset(ctx.getSide().getOpposite()), ctx.getSide().getOpposite()))
        );
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean bl = state.get(LIT);
            if (bl != world.isEmittingRedstonePower(pos.offset(state.get(FACING).getOpposite()), state.get(FACING).getOpposite())) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 4);
                } else {
                    world.setBlockState(pos, state.cycle(LIT), 2);
                }
            }
        }
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        var dir = state.get(FACING);
        if (state.get(LIT) != world.isEmittingRedstonePower(pos.offset(dir.getOpposite()), dir.getOpposite())) {
            world.setBlockState(pos, state.cycle(LIT), 2);
        }
    }

    public boolean setColor(World world, BlockPos pos, int color) {
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
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be) {
            be.setColor(FactoryItems.LAMP.getItemColor(itemStack));
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(pos, initialBlockState);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ColorableBlockEntity(pos, state);
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Text.translatable(this.getTranslationKey() + ".colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable(this.getTranslationKey() + ".colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    public static final class Full extends SidedLampBlock implements BarrierBasedWaterloggable {
        public Full(Settings settings, Identifier id, boolean inverted) {
            super(settings, id, inverted);
        }
    }

    public static final class Flat extends SidedLampBlock implements BarrierBasedWaterloggable {
        /*private static final EnumMap<Direction, BlockState> STATES_REGULAR = Util.mapEnum(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf(switch (x) {
            case UP -> "BOTTOM";
            case DOWN -> "TOP";
            default -> x.asString().toUpperCase(Locale.ROOT);
        } + "_TRAPDOOR")));
        private static final EnumMap<Direction, BlockState> STATES_WATERLOGGED = Util.mapEnum(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf(switch (x) {
            case UP -> "BOTTOM";
            case DOWN -> "TOP";
            default -> x.asString().toUpperCase(Locale.ROOT);
        } + "_TRAPDOOR_WATERLOGGED")));*/
        public Flat(Settings settings, Identifier id, boolean inverted) {
            super(settings, id, inverted);
        }

        /*@Override
        public BlockState getPolymerBlockState(BlockState state) {
            return (state.get(WATERLOGGED) ? STATES_WATERLOGGED : STATES_REGULAR).get(state.get(FACING));
        }*/
    }


    public final class Model extends BlockModel implements ColorProvider.Consumer {
        private final ItemDisplayElement main;
        private int color = -2;
        private BlockState state;

        private Model(BlockPos pos, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setScale(new Vector3f(2));
            this.main.setViewRange(0.8f);
            //this.main.setRightRotation(new Quaternionf().rotateX(MathHelper.HALF_PI));
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
            var dir = state.get(FACING);
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 90;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 180;
            }


            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        private void updateModel() {
            var stack = (this.state.get(LIT) ? onModel : offModel).copy();
            stack.set(DataComponentTypes.FIREWORK_EXPLOSION, new FireworkExplosionComponent(FireworkExplosionComponent.Type.BURST, IntList.of(this.color), IntList.of(), false, false));
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
