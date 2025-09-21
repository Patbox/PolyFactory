package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class FaucedBlock extends Block implements FactoryBlock, PolymerTexturedBlock, PipeConnectable {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public FaucedBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onBlockAdded(state, world, pos, oldState, notify);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        this.updatePowered(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updatePowered(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered), 4);
            if (powered) {
                world.scheduleBlockTick(pos, this, 1);
            }
        }
    }


    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.scheduledTick(state, world, pos, random);
        if (!state.get(POWERED)) {
            return;
        }

        this.activate(pos, state, world, 0.2f);
        world.scheduleBlockTick(pos, this, 20);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction dir;
        if (ctx.getSide().getAxis() != Direction.Axis.Y) {
            dir = ctx.getSide();
        } else {
            dir = ctx.getHorizontalPlayerFacing().getOpposite();
        }

        return super.getPlacementState(ctx).with(FACING, dir);
    }

    public static FaucedProvider getOutput(BlockState state, ServerWorld world, BlockPos pos) {
        var sourcePos = pos.offset(state.get(FACING).getOpposite());
        var source = world.getBlockState(sourcePos);
        var model = (Model) (BlockAwareAttachment.get(world, pos) instanceof BlockAwareAttachment attachment ? attachment.holder() : null);
        if (source.getBlock() instanceof FluidOutput.Getter) {
            return new DynamicProvider(model, state, pos, world, sourcePos, state.get(FACING));
        } else if (world.getBlockEntity(sourcePos) instanceof BlockEntity be && be instanceof FluidOutput output1) {
            return new SimpleProvider(model, state, pos, world, be::isRemoved, output1, state.get(FACING));
        } else {
            return new ModelOnlyProvider(model, state.get(FACING));
        }
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World worldx, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var world = (ServerWorld) worldx;
        var output = getOutput(state, world, pos);
        if (!output.isValid()) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }

        var copy = stack.copy();
        var input = new DrainInput(copy, ItemStack.EMPTY, output.getFluidContainerInput(), !(player instanceof FakePlayer));
        var optional = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, player.getWorld());
        if (optional.isEmpty() || !optional.get().value().fluidOutput(input).isEmpty()) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }

        var recipe = optional.get().value();
        var itemOut = recipe.craft(input, player.getRegistryManager());
        for (var fluid : recipe.fluidInput(input)) {
            output.extract(fluid);
        }
        ItemUsage.exchangeStack(stack, player, itemOut, false);

        player.playSoundToPlayer(recipe.soundEvent().value(), SoundCategory.BLOCKS, 0.5f, 1f);

        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS;
        }

        return this.activate(pos, state, world, 1f);
    }

    private ActionResult activate(BlockPos pos, BlockState state, World world, float rate) {
        var output = getOutput(state, (ServerWorld) world, pos);
        if (!output.isValid()) {
            return ActionResult.PASS;
        }

        if (world.getBlockEntity(pos.down()) instanceof CastingTableBlockEntity be) {
            return be.activate(output, rate);
        } else if (world.getBlockState(pos.down()).isOf(Blocks.CAULDRON)) {
            return FactoryBlocks.CASTING_CAULDRON.tryCauldronCasting((ServerWorld) world, pos.down(), output, rate);
        }
        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return FactoryUtil.TRAPDOOR_REGULAR.get(blockState.get(FACING));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_TRAPDOOR.getDefaultState().with(TrapdoorBlock.FACING, state.get(FACING)).with(TrapdoorBlock.OPEN, true);
    }

    @Override
    public boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING) == dir.getOpposite();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement fluid;
        private FluidInstance<?> fluidType;

        private Model(ServerWorld world, BlockState state) {
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
            var direction = state.get(FACING);
            this.main.setYaw(direction.getPositiveHorizontalDegrees());
            this.fluid.setYaw(direction.getPositiveHorizontalDegrees());
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

    public record SimpleProvider(FaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerWorld world, BooleanSupplier removed, FluidOutput output, Direction direction) implements FaucedProvider {
        @Override
        public FluidContainerInput getFluidContainerInput() {
            return output.getFluidContainerInput(direction);
        }

        @Override
        public boolean isValid() {
            return !removed.getAsBoolean() && world.getBlockState(faucedPos) == selfState;
        }

        @Override
        public void extract(FluidStack<?> fluid) {
            output.extractFluid(fluid.instance(), fluid.amount(), direction, true);
        }

        @Override
        public void setActiveFluid(@Nullable FluidInstance<?> fluid) {
            if (this.model != null) {
                this.model.setFluid(fluid);
            }
        }
    }

    public record DynamicProvider(FaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerWorld world, BlockPos sourcePos, Direction direction) implements FaucedProvider {
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
        public void extract(FluidStack<?> fluid) {
            var output = getOutput();
            if (output != null) {
                output.extractFluid(fluid.instance(), fluid.amount(), direction, true);
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
        public void extract(FluidStack<?> fluidStacks) {

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
            public void extract(FluidStack<?> fluidStacks) {

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

        void extract(FluidStack<?> fluidStacks);

        void setActiveFluid(@Nullable FluidInstance<?> fluid);

        Direction direction();
    }
}
