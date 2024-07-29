package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.property.ConnectablePart;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class FluidTankBlock extends Block implements FactoryBlock, PipeConnectable, BlockEntityProvider {
    public static final EnumProperty<ConnectablePart> PART_X = FactoryProperties.CONNECTABLE_PART_X;
    public static final EnumProperty<ConnectablePart> PART_Y = FactoryProperties.CONNECTABLE_PART_Y;
    public static final EnumProperty<ConnectablePart> PART_Z = FactoryProperties.CONNECTABLE_PART_Z;
    public FluidTankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(PART_X, ConnectablePart.SINGLE).with(PART_Y, ConnectablePart.SINGLE).with(PART_Z, ConnectablePart.SINGLE));
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getBlockStateAt(ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return getBlockStateAt(world, pos);
    }

    private BlockState getBlockStateAt(WorldAccess world, BlockPos pos) {
        var x = ConnectablePart.SINGLE;
        var y = ConnectablePart.SINGLE;
        var z = ConnectablePart.SINGLE;
        if (world.getBlockState(pos.down()).isOf(this)) {
            y = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.up()).isOf(this)) {
            y = y == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.north()).isOf(this)) {
            z = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.south()).isOf(this)) {
            z = z == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.west()).isOf(this)) {
            x = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.east()).isOf(this)) {
            x = x == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }


        return this.getDefaultState().with(PART_X, x).with(PART_Y, y).with(PART_Z, z);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART_X, PART_Y, PART_Z);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return FluidTankBlockEntity::tick;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }


    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final Map<FluidInstance<?>, Layer> fluidLayers = new Object2ObjectOpenHashMap<>();
        private Layer topLayer;
        private Layer bottomLayer;
        private FluidInstance<?> fluidAbove;
        private FluidInstance<?> fluidBelow;
        private float position = 0;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryModels.FLUID_TANK.get(state));
            this.main.setScale(new Vector3f(2f));
            this.main.setYaw(180);

            this.addElement(this.main);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.main.setItem(FactoryModels.FLUID_TANK.get(this.blockState()));
            }
        }

        private static int textureId(float amount) {
            return MathHelper.clamp(Math.round(amount * 15), 0, 15);
        }

        private void setLayer(FluidInstance<?> instance, float amount) {
            var layer = this.fluidLayers.get(instance);

            if (amount < 0.01) {
                if (layer != null) {
                    layer.destroy(this);
                    this.fluidLayers.remove(instance);
                }
                return;
            }

            if (layer == null) {
                var modelTop = FactoryModels.FLAT_FULL.get(instance);
                var parts = new EnumMap<Direction, ItemDisplayElement>(Direction.class);
                for (var dir : Direction.values()) {
                    var model = ItemDisplayElementUtil.createSimple(modelTop);
                    model.setViewRange(0.5f);
                    parts.put(dir, model);
                    if (dir.getAxis() != Direction.Axis.Y) {
                        model.setPitch(90);
                        model.setYaw(dir.asRotation());
                    }
                }

                layer = new Layer(instance, parts);
                this.fluidLayers.put(instance, layer);
            }
            layer.setup(this.position, amount);
            this.position += amount + 0.001f;
        }

        public void setFluids(FluidContainer container) {
            this.position = 0;
            container.provideRender(this::setLayer);
            for (var key : List.copyOf(this.fluidLayers.keySet())) {
                if (container.doesNotContain(key)) {
                    this.fluidLayers.remove(key).destroy(this);
                }
            }
            this.topLayer = this.fluidLayers.get(container.topFluid());
            this.bottomLayer = this.fluidLayers.get(container.bottomFluid());

            for (var l : this.fluidLayers.values()) {
                l.add(this);
            }

            if (this.topLayer != null) {
                this.topLayer.updateTop(this);
            }
            if (this.bottomLayer != null) {
                this.bottomLayer.updateBottom(this);
            }
        }

        public void setFluidAbove(@Nullable FluidInstance<?> fluidInstance) {
            if ((this.fluidAbove == null && fluidInstance == null) || (this.fluidAbove != null && this.fluidAbove.equals(fluidInstance))) {
                return;
            }
            this.fluidAbove = fluidInstance;
        }

        public void setFluidBelow(@Nullable FluidInstance<?> fluidInstance) {
            if ((this.fluidBelow == null && fluidInstance == null) || (this.fluidBelow != null && this.fluidBelow.equals(fluidInstance))) {
                return;
            }
            this.fluidBelow = fluidInstance;
        }

        public record Layer(FluidInstance<?> instance, EnumMap<Direction, ItemDisplayElement> parts) {
            public void add(Model model) {
                if (model.bottomLayer == this && instance.equals(model.fluidAbove)) {
                    model.removeElement(parts.get(Direction.UP));
                } else {
                    model.addElement(parts.get(Direction.UP));
                }
                if (model.topLayer == this && instance.equals(model.fluidBelow)) {
                    model.removeElement(parts.get(Direction.DOWN));
                } else {
                    model.addElement(parts.get(Direction.DOWN));
                }
                var x = model.blockState().get(FluidTankBlock.PART_X);
                var z = model.blockState().get(FluidTankBlock.PART_Z);

                if (!z.middle()) {
                    var val = z.axisDirection();
                    if (val == null) {
                        model.addElement(parts.get(Direction.NORTH));
                        model.addElement(parts.get(Direction.SOUTH));
                    } else {
                        var dir = Direction.from(Direction.Axis.Z, val);
                        model.addElement(parts.get(dir));
                        model.removeElement(parts.get(dir.getOpposite()));
                    }
                } else {
                    model.removeElement(parts.get(Direction.NORTH));
                    model.removeElement(parts.get(Direction.SOUTH));
                }

                if (!x.middle()) {
                    var val = x.axisDirection();
                    if (val == null) {
                        model.addElement(parts.get(Direction.EAST));
                        model.addElement(parts.get(Direction.WEST));
                    } else {
                        var dir = Direction.from(Direction.Axis.X, val);
                        model.addElement(parts.get(dir));
                        model.removeElement(parts.get(dir.getOpposite()));
                    }
                } else {
                    model.removeElement(parts.get(Direction.EAST));
                    model.removeElement(parts.get(Direction.WEST));
                }
            }

            public void updateTop(Model model) {
                if (instance.equals(model.fluidAbove)) {
                    model.removeElement(parts.get(Direction.UP));
                } else {
                    model.addElement(parts.get(Direction.UP));
                }
            }

            public void updateBottom(Model model) {
                if (instance.equals(model.fluidBelow)) {
                    model.removeElement(parts.get(Direction.DOWN));
                } else {
                    model.addElement(parts.get(Direction.DOWN));
                }
            }

            public void destroy(Model model) {
                for (var part : parts.values()) {
                    model.removeElement(part);
                }
            }

            public void setup(float position, float amount) {
                this.parts.get(Direction.DOWN).setTranslation(new Vector3f(0, -8f / 16f + (position) * 15.9f / 16f + 0.001f, 0));
                this.parts.get(Direction.UP).setTranslation(new Vector3f(0, -8f / 16f + (position + amount) * 15.9f / 16f + 0.001f, 0));
                var side = FactoryModels.FLAT_SCALED[Model.textureId(amount)].get(instance);
                for (var dir : FactoryUtil.HORIZONTAL_DIRECTIONS) {
                    var part = this.parts.get(dir);
                    part.setTranslation(new Vector3f(0, 0.49f, -(-8f / 16f + (position + amount / 2) * 15.9f / 16f + 0.001f)));
                    part.setScale(new Vector3f(1, 1, amount));
                    part.setItem(side);

                }
            }
        }
    }
}
