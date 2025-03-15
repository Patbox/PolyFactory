package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;

public class WindmillSailItem extends Item implements SimpleColoredItem {
    public WindmillSailItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && !context.getPlayer().canModifyBlocks()) {
            return ActionResult.FAIL;
        }
        var oldState = context.getWorld().getBlockState(context.getBlockPos());
        if (oldState.isOf(FactoryBlocks.AXLE)) {
            var axis = oldState.get(AxleBlock.AXIS);
            if (axis == Direction.Axis.Y) {
                return ActionResult.FAIL;
            }


            var val = Direction.AxisDirection.POSITIVE;

            if ((axis == Direction.Axis.X && context.getPlayerYaw() > 0 && context.getPlayerYaw() < 180)
                    || (axis == Direction.Axis.Z && (context.getPlayerYaw() < -90 || context.getPlayerYaw() > 90))) {
                val = Direction.AxisDirection.NEGATIVE;
            }

            var o = FactoryNodes.ROTATIONAL.getGraphWorld((ServerWorld) context.getWorld()).getNodesAt(context.getBlockPos()).filter(x -> x.getNode() instanceof SimpleAxisNode).findFirst();
            if (o.isPresent() && o.get().getConnections().size() == 1) {
                var conn = o.get().getConnections().iterator().next();
                var offset = conn.other(o.get()).getPos().pos().subtract(context.getBlockPos());
                val = switch (offset.getComponentAlongAxis(axis)) {
                    case 1 -> Direction.AxisDirection.POSITIVE;
                    case -1 -> Direction.AxisDirection.NEGATIVE;
                    default -> val;
                };
            }

            context.getWorld()
                    .setBlockState(context.getBlockPos(), FactoryBlocks.WINDMILL.getDefaultState()
                            .with(WindmillBlock.WATERLOGGED, oldState.get(AxleBlock.WATERLOGGED))
                            .with(WindmillBlock.FACING, Direction.from(axis, val)).with(WindmillBlock.SAIL_COUNT, 1));

            if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                be.addSail(0, context.getStack());
            }

            return ActionResult.SUCCESS_SERVER;
        } else if (oldState.isOf(FactoryBlocks.WINDMILL)) {
            var count = oldState.get(WindmillBlock.SAIL_COUNT) + 1;
            if (count > WindmillBlock.MAX_SAILS) {
                return ActionResult.FAIL;
            } else {
                var state =  oldState.with(WindmillBlock.SAIL_COUNT, count);
                context.getWorld().setBlockState(context.getBlockPos(), state);
                if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                    be.addSail(count, context.getStack());

                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        be.updateRotationalData(RotationData.State.SPECIAL, state, player.getServerWorld(), context.getBlockPos());
                        if (RotationData.State.SPECIAL.finalSpeed() > 0) {
                            TriggerCriterion.trigger(player, FactoryTriggers.CONSTRUCT_WORKING_WINDMILL);
                        }

                        RotationData.State.SPECIAL.clear();
                    }
                }
                return ActionResult.SUCCESS_SERVER;
            }
        }

        return super.useOnBlock(context);
    }
    @SuppressWarnings("DataFlowIssue")
    @Override
    public int getItemColor(ItemStack itemStack) {
        if (itemStack.contains(DataComponentTypes.DYED_COLOR)) {
            return itemStack.get(DataComponentTypes.DYED_COLOR).rgb();
        }

        return 0xFFFFFF;
    }
}
