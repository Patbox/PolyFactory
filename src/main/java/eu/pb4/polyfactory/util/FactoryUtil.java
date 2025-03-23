package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MCrafterBlockEntity;
import eu.pb4.polyfactory.util.inventory.CustomInsertInventory;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.sgui.api.GuiHelpers;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class FactoryUtil {
    public static final List<Direction> REORDERED_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    public static final GameProfile GENERIC_PROFILE = new GameProfile(Util.NIL_UUID, "[PolyFactory]");
    public static final Vec3d HALF_BELOW = new Vec3d(0, -0.5, 0);
    public static final List<Direction> HORIZONTAL_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    public static final List<EquipmentSlot> ARMOR_EQUIPMENT = Arrays.stream(EquipmentSlot.values()).filter(x -> x.getType() == EquipmentSlot.Type.HUMANOID_ARMOR).toList();
    public static final List<DyeColor> COLORS_CREATIVE = List.of(DyeColor.WHITE,
            DyeColor.LIGHT_GRAY,
            DyeColor.GRAY,
            DyeColor.BLACK,
            DyeColor.BROWN,
            DyeColor.RED,
            DyeColor.ORANGE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.GREEN,
            DyeColor.CYAN,
            DyeColor.LIGHT_BLUE,
            DyeColor.BLUE,
            DyeColor.PURPLE,
            DyeColor.MAGENTA,
            DyeColor.PINK);
    private static final List<Runnable> RUN_NEXT_TICK = new ArrayList<>();

    public static Item requestModelBase(ModelRenderType type) {
        return switch (type) {
            case SOLID -> Items.STONE;
            case TRANSPARENT -> Items.FEATHER;
            case COLORED -> Items.LEATHER_HORSE_ARMOR;
        };
    }

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
        return Identifier.of(ModInit.ID, path);
    }

    public static Text fluidText(long amount) {
        if (amount >= FluidConstants.BLOCK) {
            long buckets = amount / (FluidConstants.BLOCK / 1000);
            return Text.literal((buckets / 1000) + "." + (buckets / 10 % 100) + "B");
        } else if (amount >= 81) {
            long buckets = amount / (FluidConstants.BLOCK / 1000);
            return Text.literal((buckets) + "mB");
        } else if (amount != 0) {
            return Text.literal((amount) + "d");
        } else {
            return Text.literal("0");
        }
    }

    public static void sendVelocityDelta(ServerPlayerEntity player, Vec3d delta) {
        player.networkHandler.sendPacket(new ExplosionS2CPacket(new Vec3d(player.getX(), player.getY() - 9999, player.getZ()), Optional.of(delta), ParticleTypes.BUBBLE, Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY)));
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

    public static ItemStack exchangeStack(ItemStack inputStack, int subtractedAmount, PlayerEntity player, ItemStack outputStack, boolean creativeOverride) {
        boolean bl = player.isInCreativeMode();
        if (creativeOverride && bl) {
            if (!player.getInventory().contains(outputStack)) {
                player.getInventory().insertStack(outputStack);
            }

            return inputStack;
        } else {
            inputStack.decrementUnlessCreative(subtractedAmount, player);
            if (inputStack.isEmpty()) {
                return outputStack;
            } else {
                if (!player.getInventory().insertStack(outputStack)) {
                    player.dropItem(outputStack, false);
                }

                return inputStack;
            }
        }
    }

    public static ItemStack exchangeStack(ItemStack inputStack, int subtractedAmount, PlayerEntity player, ItemStack outputStack) {
        return exchangeStack(inputStack, subtractedAmount, player, outputStack, true);
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


    public static int tryInsertingIntoSlot(World world, BlockPos pos, ItemStack itemStack, Direction direction, IntList slots) {
        var inv = HopperBlockEntity.getInventoryAt(world, pos);

        if (inv != null) {
            return FactoryUtil.tryInsertingInvIntoSlot(inv, itemStack, direction, slots);
        }

        var storage = ItemStorage.SIDED.find(world, pos, direction);
        if (storage instanceof SlottedStorage<ItemVariant> slottedStorage) {
            try (var t = Transaction.openOuter()) {
                int total = 0;
                for (var slot : slots) {
                    if (slot >= slottedStorage.getSlotCount()) {
                        continue;
                    }
                    var x = slottedStorage.getSlot(slot).insert(ItemVariant.of(itemStack), itemStack.getCount(), t);
                    itemStack.decrement((int) x);
                    total += x;
                    if (itemStack.isEmpty()) {
                        break;
                    }
                }
                t.commit();
                return total;
            }
        }

        return -1;
    }

    public static int tryInsertingInvIntoSlot(Inventory inventory, ItemStack itemStack, Direction direction, IntList slots) {
        if (inventory instanceof CustomInsertInventory customInsertInventory) {
            return customInsertInventory.insertStackSlots(itemStack, direction, slots);
        } else if (inventory instanceof SidedInventory sidedInventory) {
            return tryInsertingSidedIntoSlot(sidedInventory, itemStack, direction, slots);
        } else {
            return tryInsertingRegularIntoSlot(inventory, itemStack, slots);
        }
    }

    private static int tryInsertingSidedIntoSlot(SidedInventory inventory, ItemStack itemStack, Direction direction, IntList allowedSlots) {
        var slots = inventory.getAvailableSlots(direction);
        var init = itemStack.getCount();

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            if (!inventory.canInsert(slot, itemStack, direction) || !allowedSlots.contains(slot)) {
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

    public static int tryInsertingRegularIntoSlot(Inventory inventory, ItemStack itemStack, IntList slots) {
        var size = inventory.size();
        var init = itemStack.getCount();
        for (int i : slots) {
            if (i >= size) {
                continue;
            }

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

    public static Text asText(@Nullable Direction dir) {
        return Text.translatable("text.polyfactory.direction." + (dir != null ? dir.asString() : "none"));
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

    public static BlockPos findFurthestFluidBlockForRemoval(World world, BlockState target, BlockPos start) {
        return start;
    }

    public static BlockPos findFurthestFluidBlockForPlacement(BlockState target, BlockPos start) {
        return start;
    }

    public static Consumer<ItemStack> getItemConsumer(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return player.getInventory()::offerOrDrop;
        } else if (entity instanceof Inventory inventory) {
            return stack -> {
                tryInsertingRegular(inventory, stack);
                if (!stack.isEmpty()) {
                    entity.dropStack((ServerWorld) entity.getWorld(), stack);
                }
            };
        }

        return (stack) -> entity.dropStack((ServerWorld) entity.getWorld(), stack);
    }

    public static void sendSlotUpdate(Entity entity, Hand hand) {
        if (entity instanceof ServerPlayerEntity player) {
            GuiHelpers.sendSlotUpdate(player, player.playerScreenHandler.syncId, hand == Hand.MAIN_HAND
                            ? PlayerScreenHandler.HOTBAR_START + player.getInventory().getSelectedSlot()
                            : PlayerScreenHandler.OFFHAND_ID,
                    player.getStackInHand(hand), player.playerScreenHandler.nextRevision());
        }
    }

    public static BlockState rotateAxis(BlockState state, Property<Direction.Axis> axis, BlockRotation rotation) {
        var a = state.get(axis);

        if (a == Direction.Axis.Y || rotation == BlockRotation.NONE || rotation == BlockRotation.CLOCKWISE_180) {
            return state;
        }

        return state.with(axis, a == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    public static <T extends Comparable<T>> BlockState rotate(BlockState state, Property<T> north, Property<T> south, Property<T> east, Property<T> west, BlockRotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.with(north, state.get(south))
                    .with(east, state.get(west))
                    .with(south, state.get(north))
                    .with(west, state.get(east));
            case COUNTERCLOCKWISE_90 -> state.with(north, state.get(east))
                    .with(east, state.get(south))
                    .with(south, state.get(west))
                    .with(west, state.get(north));
            case CLOCKWISE_90 -> state.with(north, state.get(west))
                    .with(east, state.get(north))
                    .with(south, state.get(east))
                    .with(west, state.get(south));
            default -> state;
        };
    }

    public static <T extends Comparable<T>> BlockState mirror(BlockState state, Property<T> north, Property<T> south, Property<T> east, Property<T> west, BlockMirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.with(north, state.get(south)).with(south, state.get(north));
            case FRONT_BACK -> state.with(east, state.get(west)).with(west, state.get(east));
            default -> state;
        };
    }

    public static RegistryKey<Recipe<?>> recipeKey(String s) {
        return RegistryKey.of(RegistryKeys.RECIPE, id(s));
    }

    public static <T extends Enum<T>> T nextEnum(T activeMode, T[] values, boolean next) {
        return values[(values.length + activeMode.ordinal() + (next ? 1 : -1)) % values.length];
    }

    public static <T extends Comparable<T>> Codec<T> propertyCodec(Property<T> property) {
        return Codec.stringResolver(property::name, x -> property.parse(x).orElse(property.getValues().getFirst()));
    }


    public enum MovableResult {
        SUCCESS_MOVABLE,
        SUCCESS_REGULAR,
        FAILURE
    }
}
