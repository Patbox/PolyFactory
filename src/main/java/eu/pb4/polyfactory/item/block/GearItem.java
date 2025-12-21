package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock;
import eu.pb4.polyfactory.block.mechanical.GearPlacementAligner;
import eu.pb4.polyfactory.item.FactoryItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class GearItem extends FactoryBlockItem {

    public GearItem(AxleWithGearBlock block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable("item.polyfactory.steel_gear.tooltip", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext ctx) {
        if (ctx.isSecondaryUseActive()) {
            return ctx;
        }
        var otherPos = ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite());
        var otherState = ctx.getLevel().getBlockState(otherPos);
        if (!(otherState.getBlock() instanceof GearPlacementAligner gearPlacementAligner)) {
            return super.updatePlacementContext(ctx);
        }
        var isSelfLarge = ((GearPlacementAligner) this.getBlock()).isLargeGear(this.getBlock().defaultBlockState());
        var isOtherLarge = gearPlacementAligner.isLargeGear(otherState);
        if (isSelfLarge != isOtherLarge) {
            var zeroAxis = gearPlacementAligner.getGearAxis(otherState);

            if (ctx.getClickedFace().getAxis() == zeroAxis) {
                return ctx;
            }
            var offset = ctx.getClickLocation().subtract(Vec3.atCenterOf(otherPos)).with(zeroAxis, 0);


            ctx = BlockPlaceContext.at(ctx, otherPos
                    .offset(Mth.sign(offset.x), Mth.sign(offset.y), Mth.sign(offset.z)), zeroAxis.getPositive());
            return ctx.replacingClickedOnBlock() ? ctx : null;
        } else if (isOtherLarge) {
            var zeroAxis = gearPlacementAligner.getGearAxis(otherState);

            if (ctx.getClickedFace().getAxis() == zeroAxis) {
                return ctx;
            }
            var offset = ctx.getClickLocation().subtract(Vec3.atCenterOf(otherPos));

            ctx = BlockPlaceContext.at(ctx, ctx.getClickedPos().relative(zeroAxis, Mth.sign(offset.get(zeroAxis))), ctx.getClickedFace());
            return ctx.replacingClickedOnBlock() ? ctx : null;
        }
        return super.updatePlacementContext(ctx);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getPlayer().mayBuild()) {
            return InteractionResult.FAIL;
        }
        var currentState = context.getLevel().getBlockState(context.getClickedPos());
        if (currentState.is(FactoryBlocks.AXLE) && !context.isSecondaryUseActive()) {
            var newState = this.getBlock().withPropertiesOf(currentState);
            context.getLevel().setBlockAndUpdate(context.getClickedPos(), newState);

            this.updateCustomBlockEntityTag(context.getClickedPos(), context.getLevel(), context.getPlayer(), context.getItemInHand(), newState);
            newState.getBlock().setPlacedBy(context.getLevel(), context.getClickedPos(), newState, context.getPlayer(), context.getItemInHand());
            if (context.getPlayer() instanceof ServerPlayer player) {
                CriteriaTriggers.PLACED_BLOCK.trigger(player, context.getClickedPos(), context.getItemInHand());
            }


            SoundType blockSoundGroup = newState.getSoundType();
            context.getLevel().playSound(null, context.getClickedPos(), this.getPlaceSound(newState), SoundSource.BLOCKS,
                    (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
            context.getLevel().gameEvent(GameEvent.BLOCK_PLACE, context.getClickedPos(), GameEvent.Context.of(context.getPlayer(), newState));
            context.getItemInHand().consume(1, context.getPlayer());

            return InteractionResult.SUCCESS_SERVER;
        }

        if (context.getPlayer() == null || (!context.getPlayer().isCreative() && ContainerHelper.clearOrCountMatchingItems(context.getPlayer().getInventory(), i -> i.is(FactoryItems.AXLE), 1, true) == 0)) {
            return InteractionResult.FAIL;
        }

        var result = super.useOn(context);
        if (result.consumesAction() && !context.getPlayer().isCreative()) {
            ContainerHelper.clearOrCountMatchingItems(context.getPlayer().getInventory(), i -> i.is(FactoryItems.AXLE), 1, false);
        }
        return result;
    }
}