package eu.pb4.polyfactory.block.data.util;

import com.mojang.serialization.*;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfigValue;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class DirectionalCabledDataBlock extends BaseCabledDataBlock implements StatePropertiesCodecPatcher {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public final BlockConfig<?> facingAction = BlockConfig.of("facing", BlockStateProperties.FACING, (dir, world, pos, side, state) -> FactoryUtil.asText(dir), WrenchModifyBlockValue.ofDirection(FACING),
            BlockConfigValue.ofPropertyCustom(FACING, (property, value, world, pos, side, state) -> {
                var oldDir = state.getValue(property);
                state = state.setValue(FACING, value).setValue(FACING_PROPERTIES.get(value), false);
                return state.getValue(HAS_CABLE) ? state.setValue(FACING_PROPERTIES.get(oldDir),
                        canConnectTo(world, getColor(world, pos), pos.relative(oldDir), world.getBlockState(pos.relative(oldDir)), oldDir.getOpposite())) : state;
            })).withAlt(WrenchModifyBlockValue.ofAltDirection(FACING));
    public DirectionalCabledDataBlock(Properties settings) {
        super(settings);
    }

    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {
            var facing = state.getValue(FACING);
            if (input.get(facing.getOpposite().getSerializedName()) == null) {
                return state.setValue(FACING_PROPERTIES.get(facing.getOpposite()), true);
            }
            return state;
        });
    }

    @Override
    protected Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = ctx.getLevel().getBlockState(ctx.getClickedPos());

        var delta = ctx.getClickLocation().subtract(ctx.getClickedPos().getX() + 0.5, ctx.getClickedPos().getY() + 0.5, ctx.getClickedPos().getZ() + 0.5);

        return super.getStateForPlacement(ctx).setValue(FACING, state.is(FactoryBlocks.CABLE)
                        && delta.x() < 4 / 16f && delta.x() > -4 / 16f
                        && delta.y() < 4 / 16f && delta.y() > -4 / 16f
                        && delta.z() < 4 / 16f && delta.z() > -4 / 16f
                        ? ctx.getClickedFace() : ctx.isSecondaryUseActive() ? ctx.getNearestLookingDirection().getOpposite() : ctx.getNearestLookingDirection())
                .setValue(HAS_CABLE, state.is(FactoryBlocks.CABLE))
                .setValue(FACING_PROPERTIES.get(ctx.getNearestLookingDirection()), false);
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
        return new eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock.Model(initialBlockState);
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    public static class Model extends BaseCabledDataBlock.Model {
        protected Model(BlockState state) {
            super(state);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
        }
    }
}
