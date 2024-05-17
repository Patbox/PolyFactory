package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.util.inventory.CustomInsertInventory;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class FactoryUtil {

    public static final List<Direction> REORDERED_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    public static final GameProfile GENERIC_PROFILE = new GameProfile(Util.NIL_UUID, "[PolyFactory]");
    public static final Vec3d HALF_BELOW = new Vec3d(0, -0.5, 0);

    private static final List<Runnable> RUN_NEXT_TICK = new ArrayList<>();

    public static void runNextTick(Runnable runnable) {
        RUN_NEXT_TICK.add(runnable);
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(FactoryUtil::onTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(FactoryUtil::onServerStopped);
    }

    private static void onServerStopped(MinecraftServer server) {
        RUN_NEXT_TICK.clear();
    }

    private static void onTick(MinecraftServer server) {
        RUN_NEXT_TICK.forEach(Runnable::run);
        RUN_NEXT_TICK.clear();
    }

    public static Identifier id(String path) {
        return new Identifier(ModInit.ID, path);
    }

    public static void sendVelocityDelta(ServerPlayerEntity player, Vec3d delta) {
        player.networkHandler.sendPacket(new ExplosionS2CPacket(player.getX(),  player.getY() - 9999, player.getZ(), 0, List.of(), delta, Explosion.DestructionType.KEEP,
                ParticleTypes.BUBBLE, ParticleTypes.BUBBLE, Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY)));
    }

    public static float wrap(float value, float min, float max) {
        if (value > max) {
            return min;
        } else if (value < min) {
            return max;
        }

        return value;
    }

    public static int wrap(int value, int min, int max) {
        if (value > max) {
            return min;
        } else if (value < min) {
            return max;
        }

        return value;
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
        if (inventory instanceof CustomInsertInventory customInsertInventory) {
            return customInsertInventory.insertStack(itemStack, direction);
        } else if (inventory instanceof SidedInventory sidedInventory) {
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

        var pointer = new WorldPointer(world, targetPos);
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
            } else if (ItemStack.areItemsAndComponentsEqual(current, itemStack)) {
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


    public static int insertBetween(Inventory inventory, int start, int end, ItemStack itemStack) {
        var size = Math.min(inventory.size(), end);
        var init = itemStack.getCount();
        for (int i = start; i < size; i++) {
            var current = inventory.getStack(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxCountPerStack());
                inventory.setStack(i, itemStack.copyWithCount(maxMove));
                itemStack.decrement(maxMove);

            } else if (ItemStack.areItemsAndComponentsEqual(current, itemStack)) {
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

    public static int tryInsertingRegular(Inventory inventory, ItemStack itemStack) {
        var size = inventory.size();
        var init = itemStack.getCount();
        for (int i = 0; i < size; i++) {
            var current = inventory.getStack(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxCountPerStack());
                inventory.setStack(i, itemStack.copyWithCount(maxMove));
                itemStack.decrement(maxMove);

            } else if (ItemStack.areItemsAndComponentsEqual(current, itemStack)) {
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

    public static PlayerEntity getClosestPlayer(World world, BlockPos pos, double distance) {
        return world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, distance, false);
    }

    public static Text asText(Direction dir) {
        return Text.translatable("text.polyfactory.direction." + dir);
    }

    public static void setSafeVelocity(Entity entity, Vec3d vec) {
        entity.setVelocity(safeVelocity(vec));
    }

    public static Vec3d safeVelocity(Vec3d vec) {
        var l = vec.length();

        if (l > 1028) {
            return vec.multiply(1028 / l);
        } else {
            return vec;
        }
    }

    public static void addSafeVelocity(Entity entity, Vec3d vec) {
        setSafeVelocity(entity, entity.getVelocity().add(vec));
    }

    public enum MovableResult {
        SUCCESS_MOVABLE,
        SUCCESS_REGULAR,
        FAILURE
    }
}
