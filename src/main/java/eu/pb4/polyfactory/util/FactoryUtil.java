package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class FactoryUtil {
    public static final GameProfile GENERIC_PROFILE = new GameProfile(Util.NIL_UUID, "[PolyFactory]");
    public static final Vec3d HALF_BELOW = new Vec3d(0, -0.5, 0);

    public static Identifier id(String path) {
        return new Identifier(ModInit.ID, path);
    }

    public static int tryInserting(World world, BlockPos pos, ItemStack itemStack, Direction direction) {
        var inv = HopperBlockEntity.getInventoryAt(world, pos);

        if (inv != null) {
            return FactoryUtil.tryInsertingInv(inv, itemStack, direction);
        }

        var storage = ItemStorage.SIDED.find(world, pos, direction);
        if (storage != null) {
            try (var t = Transaction.openOuter()) {
                var x = storage.insert(ItemVariant.of(itemStack), itemStack.getCount(), t);
                t.commit();
                itemStack.decrement((int) x);
                return (int) x;
            }
        }

        return -1;
    }


    public static int tryInsertingInv(Inventory inventory, ItemStack itemStack, Direction direction) {
        if (inventory instanceof SidedInventory sidedInventory) {
            return tryInsertingSided(sidedInventory, itemStack, direction);
        } else {
            return tryInsertingRegular(inventory, itemStack);
        }
    }

    public static MovableResult tryInsertingMovable(ContainerHolder conveyor, World world, BlockPos conveyorPos, BlockPos targetPos, Direction dir, Direction selfDir, @Nullable TagKey<Block> requiredTag) {
        var holdStack = conveyor.getContainer();
        if (holdStack == null || holdStack.get().isEmpty()) {
            return MovableResult.FAILURE;
        }

        var pointer = new CachedBlockPointer(world, targetPos);
        if (requiredTag != null && !pointer.getBlockState().isIn(requiredTag)) {
            return MovableResult.FAILURE;
        }

        if (pointer.getBlockState().getBlock() instanceof MovingItemConsumer conveyorInteracting) {
            if (conveyorInteracting.pushItemTo(pointer, selfDir, dir, conveyorPos, conveyor)) {
                return MovableResult.SUCCESS_MOVABLE;
            }
        } else if (tryInserting(pointer.getWorld(), pointer.getPos(), holdStack.get(), dir) != -1) {
            if (holdStack.get().isEmpty()) {
                conveyor.clearContainer();
                return MovableResult.SUCCESS_REGULAR;
            }

            return MovableResult.SUCCESS_REGULAR;
        }
        return MovableResult.FAILURE;
    }

    private static int tryInsertingSided(SidedInventory inventory, ItemStack itemStack, Direction direction) {
        var slots = inventory.getAvailableSlots(direction);
        var init = itemStack.getCount();

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            if (!inventory.canInsert(slot, itemStack, direction)) {
                continue;
            }

            var current = inventory.getStack(slot);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxCountPerStack());
                inventory.setStack(slot, itemStack.copyWithCount(maxMove));
                itemStack.decrement(maxMove);
            } else if (ItemStack.canCombine(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxCount() - current.getCount(), itemStack.getCount()), inventory.getMaxCountPerStack());

                if (maxMove > 0) {
                    current.increment(maxMove);
                    itemStack.decrement(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }


    private static int tryInsertingRegular(Inventory inventory, ItemStack itemStack) {
        var size = inventory.size();
        var init = itemStack.getCount();
        for (int i = 0; i < size; i++) {
            var current = inventory.getStack(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxCountPerStack());
                inventory.setStack(i, itemStack.copyWithCount(maxMove));
                itemStack.decrement(maxMove);

            } else if (ItemStack.canCombine(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxCount() - current.getCount(), itemStack.getCount()), inventory.getMaxCountPerStack());

                if (maxMove > 0) {
                    current.increment(maxMove);
                    itemStack.decrement(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }

    public static <T extends Comparable<T>> BlockState transform(BlockState input, Function<T, T> transform, Property<T> property) {
        return input.withIfExists(property, transform.apply(input.get(property)));
    }

    public enum MovableResult {
        SUCCESS_MOVABLE,
        SUCCESS_REGULAR,
        FAILURE
    }
}
