package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class SmelteryFaucedBlock extends Block implements FactoryBlock, PolymerTexturedBlock {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    private static final Map<Direction, BlockState> STATES_REGULAR = Util.mapEnum(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf(switch (x) {
        case UP -> "BOTTOM";
        case DOWN -> "TOP";
        default -> x.asString().toUpperCase(Locale.ROOT);
    } + "_TRAPDOOR")));

    public SmelteryFaucedBlock(Settings settings) {
        super(settings);
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
        if (source.getBlock() instanceof FluidOutput.Getter getter) {
            return new DynamicProvider(model, state, pos, world, getter, sourcePos, state.get(FACING));
        } else if (world.getBlockEntity(sourcePos) instanceof BlockEntity be && be instanceof FluidOutput output1) {
            return new SimpleProvider(model, state, pos, world, be::isRemoved, output1, state.get(FACING));
        } else {
            return new ModelOnlyProvider(model);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var output = getOutput(state, (ServerWorld) world, pos);

        if (output == null || !output.isValid()) {
            return ActionResult.PASS;
        }

        if (!player.shouldCancelInteraction() && world.getBlockEntity(pos.down()) instanceof CastingTableBlockEntity be) {
            return be.activate(output);
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return STATES_REGULAR.get(blockState.get(FACING));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.getDefaultState();
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

    public record SimpleProvider(SmelteryFaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerWorld world, BooleanSupplier removed, FluidOutput output, Direction direction) implements FaucedProvider {
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

    public record DynamicProvider(SmelteryFaucedBlock.Model model, BlockState selfState, BlockPos faucedPos, ServerWorld world, FluidOutput.Getter getter, BlockPos sourcePos, Direction direction) implements FaucedProvider {
        private FluidOutput getOutput() {
            return getter.getFluidOutput(world, sourcePos, direction);
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

    public record ModelOnlyProvider(SmelteryFaucedBlock.Model model) implements FaucedProvider {
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
        };

        FluidContainerInput getFluidContainerInput();

        boolean isValid();

        void extract(FluidStack<?> fluidStacks);

        void setActiveFluid(@Nullable FluidInstance<?> fluid);
    }
}
