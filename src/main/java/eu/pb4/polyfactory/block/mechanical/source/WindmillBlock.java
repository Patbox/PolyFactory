package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
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
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;

public class WindmillBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, ConfigurableBlock, BlockEntityProvider, BarrierBasedWaterloggable {
    public static final int MAX_SAILS = 8;
    public static final BooleanProperty BIG = BooleanProperty.of("deco_big");
    public static final IntProperty SAIL_COUNT = IntProperty.of("sails", 1, MAX_SAILS);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public WindmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(SAIL_COUNT, 4).with(BIG, false));
        Model.MODEL.getItem();
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING).add(SAIL_COUNT).add(BIG);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity be) {
            return be.getSails().isEmpty() ? FactoryItems.WINDMILL_SAIL.getDefaultStack() : be.getSails().get(0).copyWithCount(1);
        }
        return FactoryItems.WINDMILL_SAIL.getDefaultStack();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide().getOpposite()));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING)));
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
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }


    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WindmillBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity blockEntity) {
            blockEntity.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING_HORIZONTAL);
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack MODEL = Util.make(new ItemStack(Items.LEATHER_HORSE_ARMOR), x -> x.set(DataComponentTypes.ITEM_MODEL, bridgeModel(FactoryUtil.id("block/windmill_sail"))));
        public static final ItemStack MODEL_FLIP = Util.make(new ItemStack(Items.LEATHER_HORSE_ARMOR), x -> x.set(DataComponentTypes.ITEM_MODEL, bridgeModel(FactoryUtil.id("block/windmill_sail_flip"))));
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement center;
        private boolean big;
        private ItemDisplayElement[] sails;
        private WindmillBlockEntity blockEntity;

        private Model(ServerWorld world, BlockState state) {
            this.big = state.get(BIG);
            this.updateSails(state.get(SAIL_COUNT), state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);

            this.center = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate());
            this.center.setDisplaySize(3, 3);
            this.addElement(this.center);
            this.updateAnimation(0, state.get(WindmillBlock.FACING), state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
        }

        private void updateSails(int count, boolean reverse) {
            var sails = new ItemDisplayElement[count];

            var model = reverse ? MODEL_FLIP : MODEL;

            if (this.sails != null) {
                if (this.sails.length == count) {
                    for (var i = 0; i < count; i++) {
                        this.sails[i].setItem(colored(i, model));
                    }
                    return;
                } else if (this.sails.length > count) {
                    for (int i = 0; i < this.sails.length; i++) {
                        if (i < count) {
                            sails[i] = this.sails[i];
                        } else {
                            this.removeElement(this.sails[i]);
                        }
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        if (i < this.sails.length) {
                            sails[i] = this.sails[i];
                        } else {
                            var x = LodItemDisplayElement.createSimple(ItemStack.EMPTY, this.getUpdateRate());
                            x.setDisplaySize(0, 0);
                            sails[i] = x;
                            this.addElement(x);
                        }
                    }
                }
            } else {
                for (var i = 0; i < sails.length; i++) {
                    var x = LodItemDisplayElement.createSimple(ItemStack.EMPTY, this.getUpdateRate());
                    x.setDisplaySize(0, 0);
                    sails[i] = x;
                    this.addElement(x);
                }
            }

            this.sails = sails;
            for (var i = 0; i < count; i++) {
                this.sails[i].setItem(colored(i, model));
            }
        }

        private ItemStack colored(int i, ItemStack model) {
            var c = model.copy();
            int color = 0xFFFFFF;

            if (this.blockEntity != null) {
                color = this.blockEntity.getSailColor(i);
            }

            c.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(), IntList.of(color)));
            return c;
        }

        private void updateAnimation(float speed, Direction direction, boolean reverse) {
            this.center.setYaw(direction.getPositiveHorizontalDegrees() - 90);
            mat.identity();
            mat.rotateX(reverse ? speed : -speed);

            mat.pushMatrix();
            mat.rotateZ(-MathHelper.HALF_PI);
            mat.scale(2);
            if (this.big) {
                mat.scale(2);
            }
            this.center.setTransformation(mat);
            mat.popMatrix();
            //var tmp = Math.max(sails.length / 2, 1);
            for (var i = 0; i < sails.length; i++) {
                mat.pushMatrix();
                mat.rotateX((MathHelper.TAU / sails.length) * i);
                mat.rotateY(-MathHelper.HALF_PI);
                this.sails[i].setYaw(direction.getPositiveHorizontalDegrees() - 90);
                mat.translate(0, 0, 0.008f * i);
                if (this.big) {
                    mat.scale(2);
                }
                this.sails[i].setTransformation(mat);
                mat.popMatrix();
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();
            if (this.blockEntity == null) {
                this.blockEntity = this.getAttachment().getWorld().getBlockEntity(this.blockPos()) instanceof WindmillBlockEntity be ? be : null;
                this.updateSailsBe();
            }


            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockState().get(WindmillBlock.FACING),
                        this.blockState().get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);

                for (var i = 0; i < sails.length; i++) {
                    this.sails[i].startInterpolationIfDirty();
                }

                this.center.startInterpolationIfDirty();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.big = state.get(BIG);
                this.updateSailsBe();

                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos()).rotation(),
                        this.blockState().get(WindmillBlock.FACING),
                        this.blockState().get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
                for (var i = 0; i < this.sails.length; i++) {
                    this.sails[i].setInterpolationDuration(0);
                    this.sails[i].tick();
                    this.sails[i].setInterpolationDuration(5);
                }
            }
        }

        public void updateSailsBe() {
            var state = this.blockState();
            this.updateSails(state.get(SAIL_COUNT), state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
        }
    }
}
