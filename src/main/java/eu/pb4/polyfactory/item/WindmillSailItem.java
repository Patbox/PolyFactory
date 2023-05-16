package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.WindmillBlock;
import eu.pb4.polyfactory.item.util.ModeledItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;

public class WindmillSailItem extends ModeledItem {
    public WindmillSailItem(Settings settings) {
        super(Items.PAPER, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
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

            context.getWorld()
                    .setBlockState(context.getBlockPos(), FactoryBlocks.WINDMILL.getDefaultState()
                            .with(WindmillBlock.FACING, Direction.from(axis, val)).with(WindmillBlock.REVERSE, val == Direction.AxisDirection.NEGATIVE).with(WindmillBlock.SAIL_COUNT, 1));
            context.getStack().decrement(1);
            return ActionResult.SUCCESS;
        } else if (oldState.isOf(FactoryBlocks.WINDMILL)) {
            var count = oldState.get(WindmillBlock.SAIL_COUNT) + 1;
            if (count > WindmillBlock.MAX_SAILS) {
                return ActionResult.FAIL;
            } else {
                context.getWorld().setBlockState(context.getBlockPos(), oldState.with(WindmillBlock.SAIL_COUNT, count));
                context.getStack().decrement(1);
                return ActionResult.SUCCESS;
            }
        }

        return super.useOnBlock(context);
    }
}
