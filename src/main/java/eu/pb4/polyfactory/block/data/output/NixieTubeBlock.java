package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class NixieTubeBlock extends Block implements FactoryBlock, EntityBlock, ConfigurableBlock, BarrierBasedWaterloggable {
    public static Property<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static BooleanProperty POSITIVE_CONNECTED = FactoryProperties.POSITIVE_CONNECTED;
    public static BooleanProperty NEGATIVE_CONNECTED = FactoryProperties.NEGATIVE_CONNECTED;
    public static EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public static final BlockConfig AXIS_ACTION = BlockConfig.of("axis", AXIS);


    public NixieTubeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, HALF, POSITIVE_CONNECTED, NEGATIVE_CONNECTED, WATERLOGGED);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(AXIS_ACTION, BlockConfig.HALF);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild()) {
            return InteractionResult.PASS;
        }
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (stack.is(Items.NAME_TAG)) {
            var name = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";

            if (world.getBlockEntity(pos) instanceof NixieTubeBlockEntity be) {
                be.pushText(name, ' ');
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        var color = DyeColorExtra.getColor(stack);

        if (color != -1) {
            if (world.getBlockEntity(pos) instanceof NixieTubeBlockEntity be) {
                if (be.setColor(color)) {
                    be.updateTextDisplay();
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(POSITIVE_CONNECTED, false).setValue(NEGATIVE_CONNECTED, false).setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis()).setValue(HALF,
                ((ctx.getClickedFace().getAxis() == Direction.Axis.Y && ctx.getClickedFace() == Direction.DOWN) || (ctx.getClickedFace().getAxis() != Direction.Axis.Y && ctx.getClickLocation().y - ctx.getClickedPos().getY() > 0.5)) ? Half.TOP : Half.BOTTOM));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        if (state.getValue(AXIS) != direction.getAxis()) {
            return state;
        }

        return state.setValue(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? POSITIVE_CONNECTED : NEGATIVE_CONNECTED, neighborState.is(FactoryBlocks.NIXIE_TUBE) && neighborState.getValue(AXIS) == state.getValue(AXIS) && neighborState.getValue(HALF) == state.getValue(HALF));
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (oldState.is(this) && world.getBlockEntity(pos) instanceof NixieTubeBlockEntity be) {
            be.updatePositions(world, pos, state);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(AXIS, rotation.rotate(Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))).getAxis());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NixieTubeBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.GLASS.defaultBlockState();
    }

    public static final class Model extends BlockModel {
        private static final Brightness MAX_BRIGHTNESS = new Brightness(15, 15);
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final TextDisplayElement[] display = new TextDisplayElement[4];
        private char positiveFirst = ' ';
        private char positiveSecond = ' ';
        private char negativeFirst = ' ';
        private char negativeSecond = ' ';
        private int color;

        private Model(ServerLevel world, BlockPos pos, BlockState state) {
            //this.world = world;
            //this.pos = pos;
            this.mainElement = new ItemDisplayElement(FactoryItems.NIXIE_TUBE);
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setItemDisplayContext(ItemDisplayContext.FIXED);
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
        }

        public void setColor(int color) {
            if (this.color == color) {
                return;
            }
            this.positiveFirst = 0;
            this.positiveSecond = 0;
            this.negativeFirst = 0;
            this.negativeSecond = 0;
            this.color = color;
        }

        public void setText(char positiveFirst, char positiveSecond, char negativeFirst, char negativeSecond) {
            boolean dirty = false;
            if (this.positiveFirst != positiveFirst) {
                this.display[2].setText(asText(Character.toString(positiveFirst)));
                this.positiveFirst = positiveFirst;
                dirty = true;
            }

            if (this.positiveSecond != positiveSecond) {
                this.display[0].setText(asText(Character.toString(positiveSecond)));
                this.positiveSecond = positiveSecond;
                dirty = true;
            }

            if (this.negativeFirst != negativeFirst) {
                this.display[3].setText(asText(Character.toString(negativeFirst)));
                this.negativeFirst = negativeFirst;
                dirty = true;
            }

            if (this.negativeSecond != negativeSecond) {
                this.display[1].setText(asText(Character.toString(negativeSecond)));
                this.negativeSecond = negativeSecond;
                dirty = true;
            }

            if (dirty) {
                this.tick();
            }
        }

        private Component asText(String text) {
            return Component.literal(text).setStyle(Style.EMPTY.withColor(this.color));
        }



        /*private boolean updateValues() {
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
        }*/

        @Override
        public boolean startWatching(ServerGamePacketListenerImpl player) {
            return super.startWatching(player);
        }

        public void update() {
           // if (updateValues()) {
                this.updateFacing(this.blockState());
                this.tick();
            //}
        }

        private void updateFacing(BlockState facing) {
            var rot = Direction.get(Direction.AxisDirection.POSITIVE, facing.getValue(AXIS)).getClockWise().getRotation().mul(Direction.NORTH.getRotation());
            var up = facing.getValue(HALF) == Half.TOP;
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(Mth.PI);
            if (up) {
                mat.rotateZ(Mth.PI);
            }
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            var yPos = up ? -0.2f : -0.4f;

            mat.pushMatrix();
            mat.translate(-3.5f / 16f, yPos, 0);
            mat.scale(1.7f);
            mat.rotateY(Mth.PI);
            this.display[0].setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(4.5f / 16f, yPos, 0);
            mat.scale(1.7f);
            mat.rotateY(Mth.PI);
            this.display[2].setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(-4.5f / 16f, yPos, 0);
            mat.scale(1.7f);
            this.display[3].setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(3.5f / 16f, yPos, 0);
            mat.scale(1.7f);
            this.display[1].setTransformation(mat);
            mat.popMatrix();
            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }
}
