package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;

public class AxleItem extends FactoryBlockItem {
    public AxleItem(AxleBlock block, Properties settings) {
        super(block, settings);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext ctx) {
        if (ctx.isSecondaryUseActive()) {
            return ctx;
        }
        var otherPos = ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite());
        var otherState = ctx.getLevel().getBlockState(otherPos);
        int max = 4;
        if (otherState.getBlock() instanceof AxleBlock) {
            var axis = otherState.getValue(AxleBlock.AXIS);
            var dir = ctx.getNearestLookingDirection().getAxis() == axis ? ctx.getNearestLookingDirection()
                    : Direction.fromAxisAndDirection(axis, ctx.getClickLocation().subtract(Vec3.atCenterOf(otherPos)).get(axis) > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            while (ctx.getLevel().getBlockState(otherPos.relative(dir)).getBlock() instanceof AxleBlock && ctx.getLevel().getBlockState(otherPos.relative(dir)).getValue(AxleBlock.AXIS) == axis && --max > 0) {
                otherPos = otherPos.relative(dir);
            }

            if (!ctx.getLevel().getBlockState(otherPos.relative(dir)).canBeReplaced(BlockPlaceContext.at(ctx, otherPos, dir))) {
                return ctx;
            }

            return BlockPlaceContext.at(ctx, otherPos, dir);
        }

        return super.updatePlacementContext(ctx);
    }
}