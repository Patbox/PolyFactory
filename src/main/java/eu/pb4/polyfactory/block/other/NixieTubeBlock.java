package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;

public class NixieTubeBlock extends Block implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public static DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
    private static final List<Model> UPDATE_NEXT_TICK = new ArrayList<>();


    public NixieTubeBlock(Settings settings) {
        super(settings);
        ServerTickEvents.START_SERVER_TICK.register((server -> {
            for (var x : UPDATE_NEXT_TICK) {
                x.updateValues();
            }
            UPDATE_NEXT_TICK.clear();
        }));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(HALF,
                ((ctx.getSide().getAxis() == Direction.Axis.Y && ctx.getSide() == Direction.DOWN) || (ctx.getSide().getAxis() != Direction.Axis.Y && ctx.getHitPos().y - ctx.getBlockPos().getY() > 0.5)) ? BlockHalf.TOP : BlockHalf.BOTTOM);
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 4);
        }
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        var model = BlockBoundAttachment.get(world, pos);

        if (model != null) {
            ((Model) model.holder()).update();
        }
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
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    private static int getDisplayValue(ServerWorld world, BlockPos pos) {
        int i = 0;

        for(var direction : Direction.values()) {
            i += world.getEmittedRedstonePower(pos.offset(direction), direction);
        }

        return i;
    }

    public final class Model extends BaseModel {
        private static final Brightness MAX_BRIGHTNESS = new Brightness(15, 15);
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final TextDisplayElement[] display = new TextDisplayElement[4];
        private final ServerWorld world;
        private final BlockPos pos;
        private int value = Integer.MIN_VALUE;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.world = world;
            this.pos = pos;
            this.mainElement = new ItemDisplayElement(FactoryItems.NIXIE_TUBE);
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setInvisible(true);
            this.mainElement.setViewRange(0.8f);

            for (int i = 0; i < 4; i++) {
                var e = new TextDisplayElement();
                e.setBackground(0);
                e.setShadow(true);
                e.setDisplaySize(1, 1);
                e.setInvisible(true);
                e.setViewRange(0.4f);
                e.setBrightness(MAX_BRIGHTNESS);

                display[i] = e;
            }

            this.updateFacing(state);
            this.addElement(this.mainElement);
            for (int i = 0; i < 4; i++) {
                this.addElement(this.display[i]);
            }

            UPDATE_NEXT_TICK.add(this);
        }

        private boolean updateValues() {
            var x = Math.min(getDisplayValue(world, pos), 99);

            if (x != this.value) {
                this.value = x;

                for (int i = 0; i < 4; i += 2) {
                    var t = Text.literal("" + (x % 10)).formatted(Formatting.GOLD);
                    this.display[i].setText(t);
                    this.display[i + 1].setText(t);
                    x /= 10;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean startWatching(ServerPlayNetworkHandler player) {
            return super.startWatching(player);
        }

        public void update() {
            if (updateValues()) {
                this.updateFacing(BlockBoundAttachment.get(this).getBlockState());
                this.tick();
            }
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            var up = facing.get(HALF) == BlockHalf.TOP;
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(MathHelper.PI);
            if (up) {
                mat.rotateZ(MathHelper.PI);
            }
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            var yPos = up ? -0.1f : -0.4f;

            mat.pushMatrix();
            mat.translate(-0.25f, yPos, 0);
            mat.scale(1.5f);
            mat.rotateY(MathHelper.PI);
            this.display[0].setTransformation(mat);
            mat.rotateY(MathHelper.PI);
            this.display[3].setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(0.25f, yPos, 0);
            mat.scale(1.5f);

            mat.rotateY(MathHelper.PI);
            this.display[2].setTransformation(mat);
            mat.rotateY(MathHelper.PI);
            this.display[1].setTransformation(mat);
            mat.popMatrix();


            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(BlockBoundAttachment.get(this).getBlockState());
            }
        }
    }
}
