package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.item.util.ModeledItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class WindmillSailItem extends ModeledItem implements DyeableItem {
    public WindmillSailItem(Settings settings) {
        super(Items.LEATHER_HORSE_ARMOR, settings);
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
                            .with(WindmillBlock.FACING, Direction.from(axis, val)).with(WindmillBlock.SAIL_COUNT, 1));

            if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                be.addSail(0, context.getStack());
            }

            return ActionResult.SUCCESS;
        } else if (oldState.isOf(FactoryBlocks.WINDMILL)) {
            var count = oldState.get(WindmillBlock.SAIL_COUNT) + 1;
            if (count > WindmillBlock.MAX_SAILS) {
                return ActionResult.FAIL;
            } else {
                context.getWorld().setBlockState(context.getBlockPos(), oldState.with(WindmillBlock.SAIL_COUNT, count));
                if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
                    be.addSail(count, context.getStack());
                }
                return ActionResult.SUCCESS;
            }
        }

        return super.useOnBlock(context);
    }

    @Override
    public int getPolymerArmorColor(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        if (itemStack.hasNbt() && itemStack.getNbt().contains("display", NbtElement.COMPOUND_TYPE)) {
            var d = itemStack.getNbt().getCompound("display");

            if (d.contains("color", NbtElement.NUMBER_TYPE)) {
                return d.getInt("color");
            }
        }

        return 0xFFFFFF;
    }
}
