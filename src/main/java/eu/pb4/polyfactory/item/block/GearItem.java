package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.List;

public class GearItem extends SimplePolymerItem {
    private final AxleWithGearBlock block;

    public GearItem(AxleWithGearBlock block, Settings settings) {
        super(settings);
        this.block = block;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.polyfactory.steel_gear.tooltip", Text.keybind("key.use")).formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.isOf(FactoryBlocks.AXLE)) {
            var axis = oldState.get(AxleBlock.AXIS);
            context.getWorld().setBlockState(context.getBlockPos(), this.block.getStateWithProperties(oldState));
            NetworkComponent.RotationalConnector.updateRotationalConnectorAt(context.getWorld(), context.getBlockPos());
            context.getStack().decrement(1);
            var mut = new BlockPos.Mutable();

            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                for (var dir : Direction.values()) {
                    if (dir.getAxis() != axis) {
                        var state = context.getWorld().getBlockState(mut.set(context.getBlockPos()).move(dir).move(dir.rotateClockwise(axis)));

                        if (state.getBlock() instanceof AxleWithGearBlock && state.getBlock() != this.block && state.get(AxleBlock.AXIS) == axis) {
                            TriggerCriterion.trigger(player, FactoryTriggers.CONNECT_DIFFERENT_GEARS);
                            break;
                        }
                    }
                }
            }


            return ActionResult.SUCCESS_SERVER;
        }

        return super.useOnBlock(context);
    }

}
