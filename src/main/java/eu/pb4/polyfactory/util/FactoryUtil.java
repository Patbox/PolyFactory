package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.util.inventory.CustomInsertInventory;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.sgui.api.GuiHelpers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class FactoryUtil {
    public static final List<Direction> REORDERED_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    public static final GameProfile GENERIC_PROFILE = new GameProfile(Util.NIL_UUID, "[PolyFactory]");
    public static final Vec3d HALF_BELOW = new Vec3d(0, -0.5, 0);
    public static final List<Direction> HORIZONTAL_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    public static final List<EquipmentSlot> ARMOR_EQUIPMENT = Arrays.stream(EquipmentSlot.values()).filter(x -> x.getType() == EquipmentSlot.Type.HUMANOID_ARMOR).toList();

    private static final List<Runnable> RUN_NEXT_TICK = new ArrayList<>();
    private static final Item[] COLORED_MODEL_ITEMS = new Item[]{
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS,
            Items.LEATHER_HORSE_ARMOR
    };

    private static final Item[] TRANSPARENT_MODEL_ITEMS = new Item[] {
            Items.WHITE_STAINED_GLASS,
            Items.ORANGE_STAINED_GLASS,
            Items.MAGENTA_STAINED_GLASS,
            Items.LIGHT_BLUE_STAINED_GLASS,
            Items.YELLOW_STAINED_GLASS,
            Items.LIME_STAINED_GLASS,
            Items.PINK_STAINED_GLASS,
            Items.GRAY_STAINED_GLASS,
            Items.LIGHT_GRAY_STAINED_GLASS,
            Items.CYAN_STAINED_GLASS,
            Items.PURPLE_STAINED_GLASS,
            Items.BLUE_STAINED_GLASS,
            Items.BROWN_STAINED_GLASS,
            Items.GREEN_STAINED_GLASS,
            Items.RED_STAINED_GLASS,
            Items.BLACK_STAINED_GLASS,
            Items.WHITE_STAINED_GLASS_PANE,
            Items.ORANGE_STAINED_GLASS_PANE,
            Items.MAGENTA_STAINED_GLASS_PANE,
            Items.LIGHT_BLUE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE,
            Items.LIME_STAINED_GLASS_PANE,
            Items.PINK_STAINED_GLASS_PANE,
            Items.GRAY_STAINED_GLASS_PANE,
            Items.LIGHT_GRAY_STAINED_GLASS_PANE,
            Items.CYAN_STAINED_GLASS_PANE,
            Items.PURPLE_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE,
            Items.BROWN_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE,
            Items.RED_STAINED_GLASS_PANE,
            Items.BLACK_STAINED_GLASS_PANE
    };


    private static int coloredModelIndex = 0;
    private static int transparentModelIndex = 0;

    public static Item requestColoredItem() {
        return COLORED_MODEL_ITEMS[(coloredModelIndex++) % COLORED_MODEL_ITEMS.length];
    }

    public static Item requestTransparentItem() {
        return TRANSPARENT_MODEL_ITEMS[(transparentModelIndex++) % TRANSPARENT_MODEL_ITEMS.length];
    }

    public static Item requestModelBase(ModelRenderType type) {
        return switch (type) {
            case SOLID -> BaseItemProvider.requestModel();
            case TRANSPARENT -> requestTransparentItem();
            case COLORED -> requestColoredItem();
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
                    entity.dropStack(stack);
                }
            };
        }

        return entity::dropStack;
    }

    public static void sendSlotUpdate(Entity entity, Hand hand) {
        if (entity instanceof ServerPlayerEntity player ) {
            GuiHelpers.sendSlotUpdate(player, player.playerScreenHandler.syncId, hand == Hand.MAIN_HAND
                    ? PlayerScreenHandler.HOTBAR_START + player.getInventory().selectedSlot
                    : PlayerScreenHandler.OFFHAND_ID,
                    player.getStackInHand(hand), player.playerScreenHandler.nextRevision());
        }
    }

    public enum MovableResult {
        SUCCESS_MOVABLE,
        SUCCESS_REGULAR,
        FAILURE
    }
}
