package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.graph.BlockNode;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.SimpleModeledPolymerItem;
import eu.pb4.polyfactory.nodes.mechanical.RotationalSourceNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Collection;
import java.util.List;

public class WindmillBlock extends NetworkBlock implements PolymerBlock, RotationalSource, BlockWithElementHolder {
    public static final int MAX_SAILS = 8;
    public static final IntProperty SAIL_COUNT = IntProperty.of("sails", 1, MAX_SAILS);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty REVERSE = BooleanProperty.of("reverse");

    public WindmillBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(SAIL_COUNT, 4).with(REVERSE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(SAIL_COUNT).add(REVERSE);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    public double getSpeed(BlockState state, ServerWorld world, BlockPos pos) {
        var sails = state.get(SAIL_COUNT);

        if (sails < 2) {
            return 0;
        }

        var sailMult = MathHelper.lerp(MathHelper.clamp((sails - 2) / 4f, 0, 1), 0.8, 1);

        return sailMult * MathHelper.clamp((pos.getY() - 60) * 0.005, 0, 0.6);
    }

    @Override
    public Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new RotationalSourceNode(state.get(FACING)));
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

    public final class Model extends LodElementHolder {
        public static final ItemStack MODEL = new ItemStack(Items.STICK);
        public static final ItemStack MODEL_FLIP = new ItemStack(Items.STICK);
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private ItemDisplayElement[] sails;
        private final ItemDisplayElement center;

        private Model(ServerWorld world, BlockState state) {
            this.updateSails(state.get(SAIL_COUNT), state.get(REVERSE));

            this.center = new LodItemDisplayElement(AxleBlock.Model.ITEM_MODEL_SHORT);
            this.center.setDisplaySize(3, 3);
            this.center.setModelTransformation(ModelTransformationMode.FIXED);
            this.center.setInterpolationDuration(5);
            this.addElement(this.center);
            this.updateAnimation(0, 0, state.get(WindmillBlock.FACING), state.get(REVERSE));
        }

        private void updateSails(int count, boolean reverse) {
            var sails = new ItemDisplayElement[count];

            var model = reverse ? MODEL_FLIP : MODEL;

            if (this.sails != null) {
                if (this.sails.length == count) {
                    for (var i = 0 ; i < count; i++) {
                        this.sails[i].setItem(model);
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
                            var x = new LodItemDisplayElement();
                            //x.setDisplaySize(3, 3);
                            x.setModelTransformation(ModelTransformationMode.FIXED);
                            x.setInterpolationDuration(5);
                            sails[i] = x;
                            this.addElement(x);
                        }
                    }
                }
            } else {
                for (var i = 0; i < sails.length; i++) {
                    var x = new LodItemDisplayElement();
                    //x.setDisplaySize(3, 3);
                    x.setModelTransformation(ModelTransformationMode.FIXED);
                    x.setInterpolationDuration(5);
                    sails[i] = x;
                    this.addElement(x);
                }
            }

            this.sails = sails;
            for (var i = 0 ; i < count; i++) {
                this.sails[i].setItem(model);
            }
        }

        private void updateAnimation(double speed, long worldTick, Direction direction, boolean reverse) {
            mat.identity();
            mat.rotateY(MathHelper.HALF_PI - direction.asRotation() * MathHelper.RADIANS_PER_DEGREE);
            mat.rotateX(((float) (reverse ? speed : -speed) * worldTick));

            mat.pushMatrix();
            mat.rotateZ(-MathHelper.HALF_PI);
            mat.scale(2);
            this.center.setTransformation(mat);
            mat.popMatrix();
            var tmp = Math.max(sails.length / 2, 1);
            for (var i = 0; i < sails.length; i++) {
                mat.pushMatrix();
                mat.rotateX((MathHelper.TAU / sails.length) * i);
                mat.rotateY(MathHelper.HALF_PI);
                mat.translate(0, 0, 0.01f * (i % tmp));
                this.sails[i].setTransformation(mat);
                mat.popMatrix();
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                this.updateAnimation(RotationalSource.getNetworkSpeed(this.getAttachment().getWorld(),
                                ((BlockBoundAttachment) this.getAttachment()).getBlockPos()), tick,
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.FACING),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.REVERSE));

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
                this.updateSails(BlockBoundAttachment.get(this).getBlockState().get(SAIL_COUNT), BlockBoundAttachment.get(this).getBlockState().get(REVERSE));

                this.updateAnimation(RotationalSource.getNetworkSpeed(this.getAttachment().getWorld(),
                                ((BlockBoundAttachment) this.getAttachment()).getBlockPos()), this.getAttachment().getWorld().getTime(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.FACING),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(WindmillBlock.REVERSE));
                for (var i = 0; i < this.sails.length; i++) {
                    this.sails[i].setInterpolationDuration(0);
                    this.sails[i].tick();
                    this.sails[i].setInterpolationDuration(5);
                }
            }
        }

        static {
            MODEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(Items.STICK, FactoryUtil.id("block/windmill_sail")).value());
            MODEL_FLIP.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(Items.STICK, FactoryUtil.id("block/windmill_sail_flip")).value());
        }
    }
}
