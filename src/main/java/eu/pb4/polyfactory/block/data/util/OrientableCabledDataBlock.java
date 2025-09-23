package eu.pb4.polyfactory.block.data.util;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockConfigValue;
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Orientation;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class OrientableCabledDataBlock extends BaseCabledDataBlock implements StatePropertiesCodecPatcher {
    public static final EnumProperty<Orientation> ORIENTATION = Properties.ORIENTATION;

    public final BlockConfig<?> facingAction = BlockConfig.of("orientation", ORIENTATION, (dir, world, pos, side, state) ->
                    Text.empty().append(FactoryUtil.asText(dir.getFacing())).append(" / ").append(FactoryUtil.asText(dir.getRotation())),
            WrenchModifyBlockValue.ofProperty(ORIENTATION),
            BlockConfigValue.ofPropertyCustom(ORIENTATION, (property, value, world, pos, side, state) -> {
                var oldDir = state.get(property);
                state = state.with(ORIENTATION, value).with(FACING_PROPERTIES.get(value.getFacing()), false);
                return state.get(HAS_CABLE) ? state.with(FACING_PROPERTIES.get(oldDir.getFacing()),
                        canConnectTo(world, getColor(world, pos), pos.offset(oldDir.getFacing()), world.getBlockState(pos.offset(oldDir.getFacing())), oldDir.getFacing().getOpposite())) : state;
            })).withAlt(WrenchModifyBlockValue.ofAltOrientation(ORIENTATION));
    public OrientableCabledDataBlock(Settings settings) {
        super(settings);
    }

    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {
            var oldFacing = input.get("facing");
            if (oldFacing != null) {
                var dir = Direction.CODEC.decode(ops, oldFacing).getOrThrow().getFirst();

                state = state.with(ORIENTATION, Objects.requireNonNullElse(Orientation.byDirections(dir, dir.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP), Orientation.SOUTH_UP));
            }

            var facing = state.get(ORIENTATION).getFacing();
            if (input.get(facing.getOpposite().asString()) == null) {
                return state.with(FACING_PROPERTIES.get(facing.getOpposite()), true);
            }
            return state;
        });
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ORIENTATION);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = ctx.getWorld().getBlockState(ctx.getBlockPos());

        var delta = ctx.getHitPos().subtract(ctx.getBlockPos().getX() + 0.5, ctx.getBlockPos().getY() + 0.5, ctx.getBlockPos().getZ() + 0.5);

        var facing = state.isOf(FactoryBlocks.CABLE)
                && delta.getX() < 4 / 16f && delta.getX() > -4 / 16f
                && delta.getY() < 4 / 16f && delta.getY() > -4 / 16f
                && delta.getZ() < 4 / 16f && delta.getZ() > -4 / 16f
                ? ctx.getSide() : ctx.getPlayerLookDirection();

        var dir2 = switch (facing) {
            case DOWN -> ctx.getHorizontalPlayerFacing();
            case UP -> ctx.getHorizontalPlayerFacing().getOpposite();
            default -> Direction.UP;
        };

        return super.getPlacementState(ctx).with(ORIENTATION, Orientation.byDirections(facing, dir2))
                .with(HAS_CABLE, state.isOf(FactoryBlocks.CABLE))
                .with(FACING_PROPERTIES.get(ctx.getPlayerLookDirection()), false);
    }

    @Override
    protected Direction getFacing(BlockState state) {
        return state.get(ORIENTATION).getFacing();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL,
                this.facingAction
        );
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BaseCabledDataBlock.Model {

        protected Model(BlockState state) {
            super(state);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            var orientation = state.get(ORIENTATION);
            var dir = orientation.getFacing();
            float p = -90;
            float y;

            if (dir.getAxis() == Direction.Axis.Y) {
                if (dir == Direction.DOWN) {
                    p = 90;
                }
                y = orientation.getRotation().getPositiveHorizontalDegrees();
            } else {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
            }

            this.base.setYaw(y);
            this.base.setPitch(p);
        }
    }
}
