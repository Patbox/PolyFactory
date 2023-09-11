package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;

public class WindmillBlock extends RotationalNetworkBlock implements PolymerBlock, RotationUser, BlockWithElementHolder, WrenchableBlock, BlockEntityProvider, VirtualDestroyStage.Marker {
    public static final int MAX_SAILS = 8;
    public static final BooleanProperty BIG = BooleanProperty.of("deco_big");
    public static final IntProperty SAIL_COUNT = IntProperty.of("sails", 1, MAX_SAILS);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public WindmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(SAIL_COUNT, 4).with(BIG, false));
        Model.MODEL.getItem();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(SAIL_COUNT).add(BIG);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof WindmillBlockEntity be) {
            return be.getSails().isEmpty() ? FactoryItems.WINDMILL_SAIL.getDefaultStack() : be.getSails().get(0).copyWithCount(1);
        }
        return FactoryItems.WINDMILL_SAIL.getDefaultStack();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
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
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
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
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING_HORIZONTAL);
    }

    public final class Model extends BaseModel {
        public static final ItemStack MODEL = new ItemStack(Items.LEATHER_HORSE_ARMOR);
        public static final ItemStack MODEL_FLIP = new ItemStack(Items.LEATHER_HORSE_ARMOR);

        static {
            MODEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL.getItem(), FactoryUtil.id("block/windmill_sail")).value());
            MODEL_FLIP.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_FLIP.getItem(), FactoryUtil.id("block/windmill_sail_flip")).value());
        }

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement center;
        private boolean big;
        private ItemDisplayElement[] sails;
        private WindmillBlockEntity blockEntity;

        private Model(ServerWorld world, BlockState state) {
            this.big = state.get(BIG);
            this.updateSails(state.get(SAIL_COUNT), state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);

            this.center = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, 4);
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
                            var x = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 4);
                            x.setDisplaySize(0, 0);
                            sails[i] = x;
                            this.addElement(x);
                        }
                    }
                }
            } else {
                for (var i = 0; i < sails.length; i++) {
                    var x = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 4);
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
            var d = new NbtCompound();
            int color = 0xFFFFFF;

            if (this.blockEntity != null) {
                color = this.blockEntity.getSailColor(i);
            }

            d.putInt("color", color);

            c.getOrCreateNbt().put("display", d);
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
                var attach = BlockBoundAttachment.get(this);
                this.blockEntity = attach.getWorld().getBlockEntity(attach.getBlockPos()) instanceof WindmillBlockEntity be ? be : null;
                this.updateSailsBe();
            }


            if (tick % 4 == 0) {
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.FACING),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);

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
                var state = BlockBoundAttachment.get(this).getBlockState();
                this.big = state.get(BIG);
                this.updateSailsBe();

                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.FACING),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
                for (var i = 0; i < this.sails.length; i++) {
                    this.sails[i].setInterpolationDuration(0);
                    this.sails[i].tick();
                    this.sails[i].setInterpolationDuration(5);
                }
            }
        }

        public void updateSailsBe() {
            var state = BlockBoundAttachment.get(this).getBlockState();
            this.updateSails(state.get(SAIL_COUNT), state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
        }
    }
}
