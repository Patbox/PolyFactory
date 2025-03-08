package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;

public class WindmillBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, WrenchableBlock, BlockEntityProvider, BarrierBasedWaterloggable {
    public static final int MAX_SAILS = 8;
    public static final BooleanProperty BIG = BooleanProperty.of("deco_big");
    public static final IntProperty SAIL_COUNT = IntProperty.of("sails", 1, MAX_SAILS);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    public WindmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(SAIL_COUNT, 4).with(BIG, false));
        Model.MODEL.getItem();
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING).add(SAIL_COUNT).add(BIG);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
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
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof WindmillBlockEntity be) {
                ItemScatterer.spawn(world, pos, be.getSails());
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
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
    public BlockState getPolymerBlockState(BlockState state) {
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
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity blockEntity) {
            blockEntity.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(WrenchAction.FACING_HORIZONTAL);
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack MODEL = new ItemStack(Items.LEATHER_HORSE_ARMOR);
        public static final ItemStack MODEL_FLIP = new ItemStack(Items.LEATHER_HORSE_ARMOR);

        static {
            MODEL.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(PolymerResourcePackUtils.requestModel(MODEL.getItem(), FactoryUtil.id("block/windmill_sail")).value()));
            MODEL_FLIP.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(PolymerResourcePackUtils.requestModel(MODEL_FLIP.getItem(), FactoryUtil.id("block/windmill_sail_flip")).value()));
        }

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

            c.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false));
            return c;
        }

        private void updateAnimation(float speed, Direction direction, boolean reverse) {
            this.center.setYaw(direction.asRotation() - 90);
            mat.identity();
            mat.rotateX(((float) (reverse ? speed : -speed)));

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
                this.sails[i].setYaw(direction.asRotation() - 90);
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
                    if (this.sails[i].isDirty()) {
                        this.sails[i].startInterpolation();
                    }
                }

                if (this.center.isDirty()) {
                    this.center.startInterpolation();
                }
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
