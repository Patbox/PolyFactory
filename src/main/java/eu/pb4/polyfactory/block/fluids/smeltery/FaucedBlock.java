package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.fluids.PortableFluidTankBlock;
import eu.pb4.polyfactory.block.fluids.PortableFluidTankBlockEntity;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class FaucedBlock extends Block implements FactoryBlock, PolymerTexturedBlock, PipeConnectable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FaucedBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onPlace(state, world, pos, oldState, notify);
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
            if (powered) {
                world.scheduleTick(pos, this, 1);
            }
        }
    }


    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        super.tick(state, world, pos, random);
        if (!state.getValue(POWERED)) {
            return;
        }

        this.activate(pos, state, world, 0.33f);
        world.scheduleTick(pos, this, 20);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir;
        if (ctx.getClickedFace().getAxis() != Direction.Axis.Y) {
            dir = ctx.getClickedFace();
        } else {
            dir = ctx.getHorizontalDirection().getOpposite();
        }

        return super.getStateForPlacement(ctx).setValue(FACING, dir);
    }

    public static FaucedProvider getOutput(BlockState state, ServerLevel world, BlockPos pos) {
        var sourcePos = pos.relative(state.getValue(FACING).getOpposite());
        var source = world.getBlockState(sourcePos);
        var model = (Model) (BlockAwareAttachment.get(world, pos) instanceof BlockAwareAttachment attachment ? attachment.holder() : null);
        if (source.getBlock() instanceof FluidOutput.Getter) {
            return new DynamicProvider(model, state, pos, world, sourcePos, state.getValue(FACING));
        } else if (world.getBlockEntity(sourcePos) instanceof BlockEntity be && be instanceof FluidOutput output1) {
            return new SimpleProvider(model, state, pos, world, be::isRemoved, output1, state.getValue(FACING));
        } else {
            return new ModelOnlyProvider(model, state.getValue(FACING));
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level worldx, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var world = (ServerLevel) worldx;
        var output = getOutput(state, world, pos);
        if (!output.isValid()) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }

        var copy = stack.copy();
        var input = new DrainInput(copy, ItemStack.EMPTY, output.getFluidContainerInput(), !(player instanceof FakePlayer));
        var optional = world.recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, player.level());
        if (optional.isEmpty() || !optional.get().value().fluidOutput(input).isEmpty()) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }

        var recipe = optional.get().value();
        var itemOut = recipe.assemble(input, player.registryAccess());
        for (var fluid : recipe.fluidInput(input)) {
            output.extract(fluid);
        }
        ItemUtils.createFilledResult(stack, player, itemOut, false);

        FactoryUtil.playSoundToPlayer(player, recipe.soundEvent().value(), SoundSource.BLOCKS, 0.5f, 1f);

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        return this.activate(pos, state, world, 1f);
    }

    private InteractionResult activate(BlockPos pos, BlockState state, Level world, float rate) {
        var output = getOutput(state, (ServerLevel) world, pos);
        if (!output.isValid()) {
            return InteractionResult.PASS;
        }

        var downBe = world.getBlockEntity(pos.below());
        var downState = world.getBlockState(pos.below());
        if (downBe instanceof CastingTableBlockEntity be) {
            return be.activate(output, rate);
        } else if (downState.is(Blocks.CAULDRON)) {
            return FactoryBlocks.CASTING_CAULDRON.tryCauldronCasting((ServerLevel) world, pos.below(), output, rate);
        } else if (downBe instanceof PortableFluidTankBlockEntity be && downState.getValue(PortableFluidTankBlock.FACING) == Direction.UP) {
            return be.activate(output, rate);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return FactoryUtil.TRAPDOOR_REGULAR.get(blockState.getValue(FACING));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, state.getValue(FACING)).setValue(TrapDoorBlock.OPEN, true);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING) == dir.getOpposite();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement fluid;
        private FluidInstance<?> fluidType;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0, -5 / 16f));
            this.fluid = ItemDisplayElementUtil.createSimple();
            this.fluid.setScale(new Vector3f(2));
            this.fluid.setTranslation(new Vector3f(0, 0, -5 / 16f));

            updateStatePos(state);
            this.addElement(this.main);
            this.addElement(this.fluid);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(FACING);
            this.main.setYaw(direction.toYRot());
            this.fluid.setYaw(direction.toYRot());
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void setFluid(FluidInstance<?> fluid) {
            if (this.fluidType == fluid) {
                return;
            }
            this.fluid.setItem(FactoryModels.FLUID_FAUCED.get(fluid));
            this.fluid.tick();
            this.fluidType = fluid;
        }
    }

    public record SimpleProvider(FaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerLevel world, BooleanSupplier removed, FluidOutput output, Direction direction) implements FaucedProvider {
        @Override
        public FluidContainerInput getFluidContainerInput() {
            return output.getFluidContainerInput(direction);
        }

        @Override
        public boolean isValid() {
            return !removed.getAsBoolean() && world.getBlockState(faucedPos) == selfState;
        }

        @Override
        public void extract(FluidInstance<?> fluid, long amount) {
            output.extractFluid(fluid, amount, direction, true);
        }

        @Override
        public void setActiveFluid(@Nullable FluidInstance<?> fluid) {
            if (this.model != null) {
                this.model.setFluid(fluid);
            }
        }
    }

    public record DynamicProvider(FaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerLevel world, BlockPos sourcePos, Direction direction) implements FaucedProvider {
        private FluidOutput getOutput() {
            return world.getBlockState(sourcePos).getBlock() instanceof FluidOutput.Getter getter ? getter.getFluidOutput(world, sourcePos, direction) : null;
        }

        @Override
        public FluidContainerInput getFluidContainerInput() {
            var output = getOutput();
            return output != null ? output.getFluidContainerInput(direction) : FluidContainerInput.EMPTY;
        }

        @Override
        public boolean isValid() {
            return getOutput() != null && world.getBlockState(faucedPos) == selfState;
        }

        @Override
        public void extract(FluidInstance<?> fluid, long amount) {
            var output = getOutput();
            if (output != null) {
                output.extractFluid(fluid, amount, direction, true);
            }
        }

        @Override
        public void setActiveFluid(@Nullable FluidInstance<?> fluid) {
            if (this.model != null) {
                this.model.setFluid(fluid);
            }
        }
    }

    public record ModelOnlyProvider(FaucedBlock.Model model, Direction direction) implements FaucedProvider {
        @Override
        public FluidContainerInput getFluidContainerInput() {
            return FluidContainerInput.EMPTY;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void extract(FluidInstance<?> fluid, long amount) {

        }

        public void setActiveFluid(@Nullable FluidInstance<?> fluid) {
            if (this.model != null) {
                this.model.setFluid(fluid);
            }
        }
    }
    
    public interface FaucedProvider {
        FaucedProvider EMPTY = new FaucedProvider() {
            @Override
            public FluidContainerInput getFluidContainerInput() {
                return FluidContainerInput.EMPTY;
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public void extract(FluidInstance<?> fluid, long amount) {

            }

            @Override
            public void setActiveFluid(@Nullable FluidInstance<?> fluid) {

            }

            @Override
            public Direction direction() {
                return Direction.UP;
            }
        };

        FluidContainerInput getFluidContainerInput();

        boolean isValid();

        default void extract(FluidStack<?> fluidStacks) {
            this.extract(fluidStacks.instance(), fluidStacks.amount());
        }
        void extract(FluidInstance<?> fluid, long amount);

        void setActiveFluid(@Nullable FluidInstance<?> fluid);

        Direction direction();
    }
}
