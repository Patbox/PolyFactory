package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AxleItem extends FactoryBlockItem {
    public AxleItem(AxleBlock block, Settings settings) {
        super(block, settings);
    }

    @Nullable
    @Override
    public ItemPlacementContext getPlacementContext(ItemPlacementContext ctx) {
        if (ctx.shouldCancelInteraction()) {
            return ctx;
        }
        var otherPos = ctx.getBlockPos().offset(ctx.getSide().getOpposite());
        var otherState = ctx.getWorld().getBlockState(otherPos);
        int max = 4;
        if (otherState.getBlock() instanceof AxleBlock) {
            var axis = otherState.get(AxleBlock.AXIS);
            var dir = ctx.getPlayerLookDirection().getAxis() == axis ? ctx.getPlayerLookDirection()
                    : Direction.from(axis, ctx.getHitPos().subtract(Vec3d.ofCenter(otherPos)).getComponentAlongAxis(axis) > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            while (ctx.getWorld().getBlockState(otherPos.offset(dir)).getBlock() instanceof AxleBlock && ctx.getWorld().getBlockState(otherPos.offset(dir)).get(AxleBlock.AXIS) == axis && --max > 0) {
                otherPos = otherPos.offset(dir);
            }

            if (!ctx.getWorld().getBlockState(otherPos.offset(dir)).canReplace(ItemPlacementContext.offset(ctx, otherPos, dir))) {
                return ctx;
            }

            return ItemPlacementContext.offset(ctx, otherPos, dir);
        }

        return super.getPlacementContext(ctx);
    }
}