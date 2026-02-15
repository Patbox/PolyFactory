package eu.pb4.polyfactory.block.data.util;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockConfigValue;
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class OrientableCabledDataBlock extends BaseCabledDataBlock implements StatePropertiesCodecPatcher {
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

    public final BlockConfig<?> facingAction = BlockConfig.ORIENTATION.withValue(BlockConfigValue.ofPropertyCustom(ORIENTATION, (property, value, world, pos, side, state) -> {
                var oldDir = state.getValue(property);
                state = state.setValue(ORIENTATION, value).setValue(FACING_PROPERTIES.get(value.front()), false);
                return state.getValue(HAS_CABLE) ? state.setValue(FACING_PROPERTIES.get(oldDir.front()),
                        canConnectTo(world, getColor(world, pos), pos.relative(oldDir.front()), world.getBlockState(pos.relative(oldDir.front())), oldDir.front().getOpposite())) : state;
            })).withAlt(WrenchModifyBlockValue.ofAltOrientation(ORIENTATION));
    public OrientableCabledDataBlock(Properties settings) {
        super(settings);
    }

    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {
            var oldFacing = input.get("facing");
            if (oldFacing != null) {
                var dir = Direction.CODEC.decode(ops, oldFacing).getOrThrow().getFirst();

                state = state.setValue(ORIENTATION, Objects.requireNonNullElse(FrontAndTop.fromFrontAndTop(dir, dir.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP), FrontAndTop.SOUTH_UP));
            }

            var facing = state.getValue(ORIENTATION).front();
            if (input.get(facing.getOpposite().getSerializedName()) == null) {
                return state.setValue(FACING_PROPERTIES.get(facing.getOpposite()), true);
            }
            return state;
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ORIENTATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = ctx.getLevel().getBlockState(ctx.getClickedPos());

        var delta = ctx.getClickLocation().subtract(ctx.getClickedPos().getX() + 0.5, ctx.getClickedPos().getY() + 0.5, ctx.getClickedPos().getZ() + 0.5);

        var facing = state.is(FactoryBlocks.CABLE)
                && delta.x() < 4 / 16f && delta.x() > -4 / 16f
                && delta.y() < 4 / 16f && delta.y() > -4 / 16f
                && delta.z() < 4 / 16f && delta.z() > -4 / 16f
                ? ctx.getClickedFace() : ctx.isSecondaryUseActive() ? ctx.getNearestLookingDirection().getOpposite() : ctx.getNearestLookingDirection();

        var dir2 = switch (facing) {
            case DOWN -> ctx.getHorizontalDirection();
            case UP -> ctx.getHorizontalDirection().getOpposite();
            default -> Direction.UP;
        };

        return super.getStateForPlacement(ctx).setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(facing, dir2))
                .setValue(HAS_CABLE, state.is(FactoryBlocks.CABLE))
                .setValue(FACING_PROPERTIES.get(ctx.getNearestLookingDirection()), false);
    }

    @Override
    protected Direction getFacing(BlockState state) {
        return state.getValue(ORIENTATION).front();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL,
                this.facingAction
        );
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock.Model(initialBlockState);
    }

    public static class Model extends BaseCabledDataBlock.Model {

        protected Model(BlockState state) {
            super(state);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            var orientation = state.getValue(ORIENTATION);
            var dir = orientation.front();
            float p = -90;
            float y;

            if (dir.getAxis() == Direction.Axis.Y) {
                if (dir == Direction.DOWN) {
                    p = 90;
                }
                y = orientation.top().toYRot();
            } else {
                p = 0;
                y = dir.toYRot();
            }

            this.base.setYaw(y);
            this.base.setPitch(p);
        }
    }
}
