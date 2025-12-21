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
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;

public class WindmillSailItem extends Item implements SimpleColoredItem {
    public WindmillSailItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getPlayer().mayBuild()) {
            return InteractionResult.FAIL;
        }
        var oldState = context.getLevel().getBlockState(context.getClickedPos());
        if (oldState.is(FactoryBlocks.AXLE)) {
            var axis = oldState.getValue(AxleBlock.AXIS);
            if (axis == Direction.Axis.Y) {
                return InteractionResult.FAIL;
            }


            var val = Direction.AxisDirection.POSITIVE;

            if ((axis == Direction.Axis.X && context.getRotation() > 0 && context.getRotation() < 180)
                    || (axis == Direction.Axis.Z && (context.getRotation() < -90 || context.getRotation() > 90))) {
                val = Direction.AxisDirection.NEGATIVE;
            }

            var o = FactoryNodes.ROTATIONAL.getGraphWorld((ServerLevel) context.getLevel()).getNodesAt(context.getClickedPos()).filter(x -> x.getNode() instanceof SimpleAxisNode).findFirst();
            if (o.isPresent() && o.get().getConnections().size() == 1) {
                var conn = o.get().getConnections().iterator().next();
                var offset = conn.other(o.get()).getPos().pos().subtract(context.getClickedPos());
                val = switch (offset.get(axis)) {
                    case 1 -> Direction.AxisDirection.POSITIVE;
                    case -1 -> Direction.AxisDirection.NEGATIVE;
                    default -> val;
                };
            }

            context.getLevel()
                    .setBlockAndUpdate(context.getClickedPos(), FactoryBlocks.WINDMILL.defaultBlockState()
                            .setValue(WindmillBlock.WATERLOGGED, oldState.getValue(AxleBlock.WATERLOGGED))
                            .setValue(WindmillBlock.FACING, Direction.fromAxisAndDirection(axis, val)).setValue(WindmillBlock.SAIL_COUNT, 1));

            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WindmillBlockEntity be) {
                be.addSail(0, context.getItemInHand());
            }

            return InteractionResult.SUCCESS_SERVER;
        } else if (oldState.is(FactoryBlocks.WINDMILL)) {
            var count = oldState.getValue(WindmillBlock.SAIL_COUNT) + 1;
            if (count > WindmillBlock.MAX_SAILS) {
                return InteractionResult.FAIL;
            } else {
                var state =  oldState.setValue(WindmillBlock.SAIL_COUNT, count);
                context.getLevel().setBlockAndUpdate(context.getClickedPos(), state);
                if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WindmillBlockEntity be) {
                    be.addSail(count, context.getItemInHand());

                    if (context.getPlayer() instanceof ServerPlayer player) {
                        be.updateRotationalData(RotationData.State.SPECIAL, state, player.level(), context.getClickedPos());
                        if (RotationData.State.SPECIAL.finalSpeed() > 0) {
                            TriggerCriterion.trigger(player, FactoryTriggers.CONSTRUCT_WORKING_WINDMILL);
                        }

                        RotationData.State.SPECIAL.clear();
                    }
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useOn(context);
    }
    @SuppressWarnings("DataFlowIssue")
    @Override
    public int getItemColor(ItemStack itemStack) {
        if (itemStack.has(DataComponents.DYED_COLOR)) {
            return itemStack.get(DataComponents.DYED_COLOR).rgb();
        }

        return 0xFFFFFF;
    }
}
