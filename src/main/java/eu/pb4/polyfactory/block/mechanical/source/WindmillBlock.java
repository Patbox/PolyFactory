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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;

public class WindmillBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, ConfigurableBlock, EntityBlock, BarrierBasedWaterloggable {
    public static final int MAX_SAILS = 8;
    public static final BooleanProperty BIG = BooleanProperty.create("deco_big");
    public static final IntegerProperty SAIL_COUNT = IntegerProperty.create("sails", 1, MAX_SAILS);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WindmillBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(SAIL_COUNT, 4).setValue(BIG, false));
        Model.MODEL.getItem();
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING).add(SAIL_COUNT).add(BIG);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity be) {
            return be.getSails().isEmpty() ? FactoryItems.WINDMILL_SAIL.getDefaultInstance() : be.getSails().get(0).copyWithCount(1);
        }
        return FactoryItems.WINDMILL_SAIL.getDefaultInstance();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING)));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }


    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindmillBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity blockEntity) {
            blockEntity.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING_HORIZONTAL);
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack MODEL = Util.make(new ItemStack(Items.LEATHER_HORSE_ARMOR), x -> x.set(DataComponents.ITEM_MODEL, bridgeModel(FactoryUtil.id("block/windmill_sail"))));
        public static final ItemStack MODEL_FLIP = Util.make(new ItemStack(Items.LEATHER_HORSE_ARMOR), x -> x.set(DataComponents.ITEM_MODEL, bridgeModel(FactoryUtil.id("block/windmill_sail_flip"))));
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement center;
        private boolean big;
        private ItemDisplayElement[] sails;
        private WindmillBlockEntity blockEntity;

        private Model(ServerLevel world, BlockState state) {
            this.big = state.getValue(BIG);
            this.updateSails(state.getValue(SAIL_COUNT), state.getValue(FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE);

            this.center = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate());
            this.center.setDisplaySize(3, 3);
            this.addElement(this.center);
            this.updateAnimation(0, state.getValue(WindmillBlock.FACING), state.getValue(FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE);
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

            c.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(color)));
            return c;
        }

        private void updateAnimation(float speed, Direction direction, boolean reverse) {
            this.center.setYaw(direction.toYRot() - 90);
            mat.identity();
            mat.rotateX(reverse ? speed : -speed);

            mat.pushMatrix();
            mat.rotateZ(-Mth.HALF_PI);
            mat.scale(2);
            if (this.big) {
                mat.scale(2);
            }
            this.center.setTransformation(mat);
            mat.popMatrix();
            //var tmp = Math.max(sails.length / 2, 1);
            for (var i = 0; i < sails.length; i++) {
                mat.pushMatrix();
                mat.rotateX((Mth.TWO_PI / sails.length) * i);
                mat.rotateY(-Mth.HALF_PI);
                this.sails[i].setYaw(direction.toYRot() - 90);
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
            var tick = this.getAttachment().getWorld().getGameTime();
            if (this.blockEntity == null) {
                this.blockEntity = this.getAttachment().getWorld().getBlockEntity(this.blockPos()) instanceof WindmillBlockEntity be ? be : null;
                this.updateSailsBe();
            }


            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockState().getValue(WindmillBlock.FACING),
                        this.blockState().getValue(FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE);

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
                this.big = state.getValue(BIG);
                this.updateSailsBe();

                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos()).rotation(),
                        this.blockState().getValue(WindmillBlock.FACING),
                        this.blockState().getValue(FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE);
                for (var i = 0; i < this.sails.length; i++) {
                    this.sails[i].setInterpolationDuration(0);
                    this.sails[i].tick();
                    this.sails[i].setInterpolationDuration(5);
                }
            }
        }

        public void updateSailsBe() {
            var state = this.blockState();
            this.updateSails(state.getValue(SAIL_COUNT), state.getValue(FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE);
        }
    }
}
