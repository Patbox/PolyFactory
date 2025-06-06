package eu.pb4.polyfactory.block.data.util;

import com.mojang.serialization.*;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.ConfigValue;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.WrenchModifyValue;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public abstract class GenericCabledDataBlock extends AbstractCableBlock implements FactoryBlock, ConfigurableBlock, BlockEntityProvider, CableConnectable, StatePropertiesCodecPatcher {
    public static final EnumProperty<Direction> FACING = Properties.FACING;

    public final BlockConfig<?> facingAction = BlockConfig.of("facing", Properties.FACING, (dir, world, pos, side, state) -> FactoryUtil.asText(dir), WrenchModifyValue.ofDirection(FACING),
            ConfigValue.ofPropertyCustom(FACING, (property, value, world, pos, side, state) -> {
                var oldDir = state.get(property);
                state = state.with(FACING, value).with(FACING_PROPERTIES.get(value), false);
                return state.get(HAS_CABLE) ? state.with(FACING_PROPERTIES.get(oldDir),
                        canConnectTo(world, getColor(world, pos), pos.offset(oldDir), world.getBlockState(pos.offset(oldDir)), oldDir.getOpposite())) : state;
            })).withAlt(WrenchModifyValue.ofAltDirection(FACING));
    public GenericCabledDataBlock(Settings settings) {
        super(settings);
    }

    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {
            var facing = state.get(FACING);
            if (input.get(facing.getOpposite().asString()) == null) {
                return state.with(FACING_PROPERTIES.get(facing.getOpposite()), true);
            }
            return state;
        });
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return this.asItem().getDefaultStack();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, HAS_CABLE);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return (context.getStack().isOf(FactoryItems.CABLE) && !state.get(HAS_CABLE)) || super.canReplace(state, context);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = ctx.getWorld().getBlockState(ctx.getBlockPos());

        var delta = ctx.getHitPos().subtract(ctx.getBlockPos().getX() + 0.5, ctx.getBlockPos().getY() + 0.5, ctx.getBlockPos().getZ() + 0.5);

        return super.getPlacementState(ctx).with(FACING, state.isOf(FactoryBlocks.CABLE)
                        && delta.getX() < 4 / 16f && delta.getX() > -4 / 16f
                        && delta.getY() < 4 / 16f && delta.getY() > -4 / 16f
                        && delta.getZ() < 4 / 16f && delta.getZ() > -4 / 16f
                        ? ctx.getSide() : ctx.getPlayerLookDirection())
                .with(HAS_CABLE, state.isOf(FactoryBlocks.CABLE))
                .with(FACING_PROPERTIES.get(ctx.getPlayerLookDirection()), false);
    }

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return state.get(FACING) == direction || !state.get(HAS_CABLE);
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING) != dir && state.get(HAS_CABLE) && super.canCableConnect(world, cableColor, pos, state, dir);
    }

    @Override
    public boolean hasCable(BlockState state) {
        return state.get(HAS_CABLE);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
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

    @Override
    public boolean setColor(BlockState state, World world, BlockPos pos, int color) {
        return state.get(HAS_CABLE) && super.setColor(state, world, pos, color);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    public static class Model extends BaseCableModel {
        protected final ItemDisplayElement base;

        protected Model(BlockState state) {
            super(state);
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        protected void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
        }

        @Override
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            updateStatePos(this.blockState());
            this.base.tick();
        }
    }
}
