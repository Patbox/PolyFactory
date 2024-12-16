package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GearItem extends FactoryBlockItem {

    public GearItem(AxleWithGearBlock block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.polyfactory.steel_gear.tooltip", Text.keybind("key.use")).formatted(Formatting.GRAY));
    }

    @Nullable
    @Override
    public ItemPlacementContext getPlacementContext(ItemPlacementContext ctx) {
        if (ctx.shouldCancelInteraction()) {
            return ctx;
        }
        var otherPos = ctx.getBlockPos().offset(ctx.getSide().getOpposite());
        var otherState = ctx.getWorld().getBlockState(otherPos);
        if (otherState.getBlock() instanceof AxleWithGearBlock && !otherState.isOf(this.getBlock())) {
            var zeroAxis  = otherState.get(AxleWithGearBlock.AXIS);

            if (ctx.getSide().getAxis() == zeroAxis) {
                return ctx;
            }
            var offset = ctx.getHitPos().subtract(Vec3d.ofCenter(otherPos)).withAxis(zeroAxis, 0);


            ctx = ItemPlacementContext.offset(ctx, otherPos
                    .add(MathHelper.sign(offset.x), MathHelper.sign(offset.y), MathHelper.sign(offset.z)), zeroAxis.getPositiveDirection());
            return ctx.canReplaceExisting() ? ctx : null;
        } else if (otherState.isOf(this.getBlock()) && this.getBlock() instanceof AxleWithLargeGearBlock) {
            var zeroAxis = otherState.get(AxleWithGearBlock.AXIS);

            if (ctx.getSide().getAxis() == zeroAxis) {
                return ctx;
            }
            var offset = ctx.getHitPos().subtract(Vec3d.ofCenter(otherPos));

            ctx = ItemPlacementContext.offset(ctx, ctx.getBlockPos().offset(zeroAxis, MathHelper.sign(offset.getComponentAlongAxis(zeroAxis))), ctx.getSide());
            return ctx.canReplaceExisting() ? ctx : null;
        }
        return super.getPlacementContext(ctx);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var currentState = context.getWorld().getBlockState(context.getBlockPos());
        if (currentState.isOf(FactoryBlocks.AXLE) && !context.shouldCancelInteraction()) {
            var newState = this.getBlock().getStateWithProperties(currentState);
            context.getWorld().setBlockState(context.getBlockPos(), newState);

            this.postPlacement(context.getBlockPos(), context.getWorld(), context.getPlayer(), context.getStack(), newState);
            newState.getBlock().onPlaced(context.getWorld(), context.getBlockPos(), newState, context.getPlayer(), context.getStack());
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                Criteria.PLACED_BLOCK.trigger(player, context.getBlockPos(), context.getStack());
            }


            BlockSoundGroup blockSoundGroup = newState.getSoundGroup();
            context.getWorld().playSound(null, context.getBlockPos(), this.getPlaceSound(newState), SoundCategory.BLOCKS,
                    (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
            context.getWorld().emitGameEvent(GameEvent.BLOCK_PLACE, context.getBlockPos(), GameEvent.Emitter.of(context.getPlayer(), newState));
            context.getStack().decrementUnlessCreative(1, context.getPlayer());

            return ActionResult.SUCCESS;
        }

        if (context.getPlayer() == null || (!context.getPlayer().isCreative() && Inventories.remove(context.getPlayer().getInventory(), i -> i.isOf(FactoryItems.AXLE), 1, true) == 0)) {
            return ActionResult.FAIL;
        }

        var result = super.useOnBlock(context);
        if (result.isAccepted() && !context.getPlayer().isCreative()) {
            Inventories.remove(context.getPlayer().getInventory(), i -> i.isOf(FactoryItems.AXLE), 1, false);
        }
        return result;
    }
}