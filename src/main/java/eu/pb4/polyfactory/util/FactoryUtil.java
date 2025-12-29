package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.util.inventory.CustomInsertContainer;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.sgui.api.GuiHelpers;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.*;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class FactoryUtil {
    public static final List<Direction> REORDERED_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);
    public static final GameProfile GENERIC_PROFILE = new GameProfile(Util.NIL_UUID, "[PolyFactory]");
    public static final Vec3 HALF_BELOW = new Vec3(0, -0.5, 0);
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

    public static final Map<Direction, BlockState> TRAPDOOR_REGULAR = Util.makeEnumMap(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf(switch (x) {
        case UP -> "BOTTOM";
        case DOWN -> "TOP";
        default -> x.getSerializedName().toUpperCase(Locale.ROOT);
    } + "_TRAPDOOR")));

    public static final Map<Direction, BlockState> TRAPDOOR_WATERLOGGED = Util.makeEnumMap(Direction.class, x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf(switch (x) {
        case UP -> "BOTTOM";
        case DOWN -> "TOP";
        default -> x.getSerializedName().toUpperCase(Locale.ROOT);
    } + "_TRAPDOOR_WATERLOGGED")));


    public static final Map<Direction.Axis, BlockState> LIGHTNING_ROD_REGULAR = Util.makeEnumMap(Direction.Axis.class,
            x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf( "LIGHTNING_ROD_" + x.name())));

    public static final Map<Direction.Axis, BlockState> LIGHTNING_ROD_WATERLOGGED = Util.makeEnumMap(Direction.Axis.class,
            x -> PolymerBlockResourceUtils.requestEmpty(BlockModelType.valueOf( "LIGHTNING_ROD_" + x.name() + "_WATERLOGGED")));

    private static final List<Runnable> RUN_NEXT_TICK = new ArrayList<>();

    public static Item requestModelBase(ModelRenderType type) {
        return switch (type) {
            case SOLID -> Items.STONE;
            case TRANSPARENT -> Items.FEATHER;
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
        return Identifier.fromNamespaceAndPath(ModInit.ID, path);
    }

    public static void playSoundToPlayer(Player player, SoundEvent soundEvent, SoundSource category, float volume, float pitch) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent), category, player, volume, pitch, player.getRandom().nextLong()));
        }
    }

    public static MutableComponent fluidTextIngots(long amount) {
        if (amount >= FluidConstants.BLOCK) {
            long buckets = amount / (FluidConstants.BLOCK / 1000);
            return Component.literal((buckets / 1000) + "." + (buckets / 10 % 100) + " ").append(Component.translatable("text.polyfactory.amount.block"));
        } else if (amount >= FluidConstants.INGOT) {
            long buckets = amount / (FluidConstants.INGOT / 1000);
            return Component.literal((buckets / 1000) + "." + (buckets / 10 % 100) + " ").append(Component.translatable("text.polyfactory.amount.ingot"));
        } else if (amount >= FluidConstants.NUGGET) {
            //noinspection PointlessArithmeticExpression
            long buckets = amount / (FluidConstants.NUGGET / 1000);
            return Component.literal((buckets / 1000) + "." + (buckets / 10 % 100) + " ").append(Component.translatable("text.polyfactory.amount.nuggets"));
        } else if (amount != 0) {
            return Component.literal((amount) + "d");
        } else {
            return Component.literal("0");
        }
    }

    public static MutableComponent fluidTextGeneric(long amount) {
        if (amount >= FluidConstants.BLOCK) {
            long buckets = amount / (FluidConstants.BLOCK / 1000);
            return Component.literal((buckets / 1000) + "." + (buckets / 10 % 100) + "B");
        } else if (amount >= 81) {
            long buckets = amount / (FluidConstants.BLOCK / 1000);
            return Component.literal((buckets) + "mB");
        } else if (amount != 0) {
            return Component.literal((amount) + "d");
        } else {
            return Component.literal("0");
        }
    }

    public static void sendVelocityDelta(ServerPlayer player, Vec3 delta) {
        player.connection.send(new ClientboundExplodePacket(new Vec3(player.getX(), player.getY() - 9999, player.getZ()), 0, 0, Optional.of(delta),
                ParticleTypes.BUBBLE, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), WeightedList.of()));
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

    public static ItemStack exchangeStack(ItemStack inputStack, int subtractedAmount, Player player, ItemStack outputStack, boolean creativeOverride) {
        boolean bl = player.hasInfiniteMaterials();
        if (creativeOverride && bl) {
            if (!player.getInventory().contains(outputStack)) {
                player.getInventory().add(outputStack);
            }

            return inputStack;
        } else {
            inputStack.consume(subtractedAmount, player);
            if (inputStack.isEmpty()) {
                return outputStack;
            } else {
                if (!player.getInventory().add(outputStack)) {
                    player.drop(outputStack, false);
                }

                return inputStack;
            }
        }
    }

    public static ItemStack exchangeStack(ItemStack inputStack, int subtractedAmount, Player player, ItemStack outputStack) {
        return exchangeStack(inputStack, subtractedAmount, player, outputStack, true);
    }

    public static int tryInserting(Level world, BlockPos pos, ItemStack itemStack, Direction direction) {
        var inv = HopperBlockEntity.getContainerAt(world, pos);

        if (inv != null) {
            return FactoryUtil.tryInsertingInv(inv, itemStack, direction);
        }

        var storage = ItemStorage.SIDED.find(world, pos, direction);
        if (storage != null) {
            try (var t = Transaction.openOuter()) {
                var x = storage.insert(ItemVariant.of(itemStack), itemStack.getCount(), t);
                t.commit();
                itemStack.shrink((int) x);
                return (int) x;
            }
        }

        return -1;
    }

    public static int tryInsertingInv(Container inventory, ItemStack itemStack, Direction direction) {
        if (inventory instanceof CustomInsertContainer customInsertContainer) {
            return customInsertContainer.insertStack(itemStack, direction);
        } else if (inventory instanceof WorldlyContainer sidedInventory) {
            return tryInsertingSided(sidedInventory, itemStack, direction);
        } else {
            return tryInsertingRegular(inventory, itemStack);
        }
    }

    public static MovableResult tryInsertingMovable(MovingItemContainerHolder conveyor, Level world, BlockPos conveyorPos, BlockPos targetPos, Direction dir, Direction selfDir, @Nullable TagKey<Block> requiredTag) {
        var holdStack = conveyor.getContainer();
        if (holdStack == null || holdStack.get().isEmpty()) {
            return MovableResult.FAILURE;
        }

        var pointer = new WorldPointer(world, targetPos);
        if (requiredTag != null && !pointer.getBlockState().is(requiredTag)) {
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

    private static int tryInsertingSided(WorldlyContainer inventory, ItemStack itemStack, Direction direction) {
        var slots = inventory.getSlotsForFace(direction);
        var init = itemStack.getCount();

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            if (!inventory.canPlaceItemThroughFace(slot, itemStack, direction)) {
                continue;
            }

            var current = inventory.getItem(slot);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxStackSize());
                inventory.setItem(slot, itemStack.copyWithCount(maxMove));
                itemStack.shrink(maxMove);
            } else if (ItemStack.isSameItemSameComponents(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), itemStack.getCount()), inventory.getMaxStackSize());

                if (maxMove > 0) {
                    current.grow(maxMove);
                    itemStack.shrink(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }


    public static int insertBetween(Container inventory, int start, int end, ItemStack itemStack) {
        var size = Math.min(inventory.getContainerSize(), end);
        var init = itemStack.getCount();
        for (int i = start; i < size; i++) {
            var current = inventory.getItem(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxStackSize());
                inventory.setItem(i, itemStack.copyWithCount(maxMove));
                itemStack.shrink(maxMove);

            } else if (ItemStack.isSameItemSameComponents(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), itemStack.getCount()), inventory.getMaxStackSize());

                if (maxMove > 0) {
                    current.grow(maxMove);
                    itemStack.shrink(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }

    public static int tryInsertingRegular(Container inventory, ItemStack itemStack) {
        var size = inventory.getContainerSize();
        var init = itemStack.getCount();
        for (int i = 0; i < size; i++) {
            var current = inventory.getItem(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxStackSize());
                inventory.setItem(i, itemStack.copyWithCount(maxMove));
                itemStack.shrink(maxMove);

            } else if (ItemStack.isSameItemSameComponents(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), itemStack.getCount()), inventory.getMaxStackSize());

                if (maxMove > 0) {
                    current.grow(maxMove);
                    itemStack.shrink(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }


    public static int tryInsertingIntoSlot(Level world, BlockPos pos, ItemStack itemStack, Direction direction, IntList slots) {
        var inv = HopperBlockEntity.getContainerAt(world, pos);

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
                    itemStack.shrink((int) x);
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

    public static int tryInsertingInvIntoSlot(Container inventory, ItemStack itemStack, Direction direction, IntList slots) {
        if (inventory instanceof CustomInsertContainer customInsertContainer) {
            return customInsertContainer.insertStackSlots(itemStack, direction, slots);
        } else if (inventory instanceof WorldlyContainer sidedInventory) {
            return tryInsertingSidedIntoSlot(sidedInventory, itemStack, direction, slots);
        } else {
            return tryInsertingRegularIntoSlot(inventory, itemStack, slots);
        }
    }

    private static int tryInsertingSidedIntoSlot(WorldlyContainer inventory, ItemStack itemStack, Direction direction, IntList allowedSlots) {
        var slots = inventory.getSlotsForFace(direction);
        var init = itemStack.getCount();

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            if (!inventory.canPlaceItemThroughFace(slot, itemStack, direction) || !allowedSlots.contains(slot)) {
                continue;
            }

            var current = inventory.getItem(slot);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxStackSize());
                inventory.setItem(slot, itemStack.copyWithCount(maxMove));
                itemStack.shrink(maxMove);
            } else if (ItemStack.isSameItemSameComponents(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), itemStack.getCount()), inventory.getMaxStackSize());

                if (maxMove > 0) {
                    current.grow(maxMove);
                    itemStack.shrink(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }

    public static int tryInsertingRegularIntoSlot(Container inventory, ItemStack itemStack, IntList slots) {
        var size = inventory.getContainerSize();
        var init = itemStack.getCount();
        for (int i : slots) {
            if (i >= size) {
                continue;
            }

            var current = inventory.getItem(i);

            if (current.isEmpty()) {
                var maxMove = Math.min(itemStack.getCount(), inventory.getMaxStackSize());
                inventory.setItem(i, itemStack.copyWithCount(maxMove));
                itemStack.shrink(maxMove);

            } else if (ItemStack.isSameItemSameComponents(current, itemStack)) {
                var maxMove = Math.min(Math.min(current.getMaxStackSize() - current.getCount(), itemStack.getCount()), inventory.getMaxStackSize());

                if (maxMove > 0) {
                    current.grow(maxMove);
                    itemStack.shrink(maxMove);
                }
            }

            if (itemStack.isEmpty()) {
                return init;
            }
        }

        return init - itemStack.getCount();
    }

    public static <T extends Comparable<T>> BlockState transform(BlockState input, Function<T, T> transform, Property<T> property) {
        return input.trySetValue(property, transform.apply(input.getValue(property)));
    }

    public static Player getClosestPlayer(Level world, BlockPos pos, double distance) {
        return world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, distance, false);
    }

    public static Component asText(@Nullable Direction dir) {
        return Component.translatable("text.polyfactory.direction." + (dir != null ? dir.getSerializedName() : "none"));
    }

    public static void setSafeVelocity(Entity entity, Vec3 vec) {
        entity.setDeltaMovement(safeVelocity(vec));
    }

    public static Vec3 safeVelocity(Vec3 vec) {
        var l = vec.length();

        if (l > 1028) {
            return vec.scale(1028 / l);
        } else {
            return vec;
        }
    }

    public static void addSafeVelocity(Entity entity, Vec3 vec) {
        setSafeVelocity(entity, entity.getDeltaMovement().add(vec));
    }

    public static BlockPos findFurthestFluidBlockForRemoval(Level world, BlockState target, BlockPos start) {
        return start;
    }

    public static BlockPos findFurthestFluidBlockForPlacement(BlockState target, BlockPos start) {
        return start;
    }

    public static Consumer<ItemStack> getItemConsumer(Entity entity) {
        if (entity instanceof Player player) {
            return player.getInventory()::placeItemBackInInventory;
        } else if (entity instanceof Container inventory) {
            return stack -> {
                tryInsertingRegular(inventory, stack);
                if (!stack.isEmpty()) {
                    entity.spawnAtLocation((ServerLevel) entity.level(), stack);
                }
            };
        }

        return (stack) -> entity.spawnAtLocation((ServerLevel) entity.level(), stack);
    }

    public static void sendSlotUpdate(Entity entity, InteractionHand hand) {
        if (entity instanceof ServerPlayer player) {
            GuiHelpers.sendSlotUpdate(player, player.inventoryMenu.containerId, hand == InteractionHand.MAIN_HAND
                            ? InventoryMenu.USE_ROW_SLOT_START + player.getInventory().getSelectedSlot()
                            : InventoryMenu.SHIELD_SLOT,
                    player.getItemInHand(hand), player.inventoryMenu.incrementStateId());
        }
    }

    public static BlockState rotateAxis(BlockState state, Property<Direction.Axis> axis, Rotation rotation) {
        var a = state.getValue(axis);

        if (a == Direction.Axis.Y || rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180) {
            return state;
        }

        return state.setValue(axis, a == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    public static <T extends Comparable<T>> BlockState rotate(BlockState state, Property<T> north, Property<T> south, Property<T> east, Property<T> west, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(north, state.getValue(south))
                    .setValue(east, state.getValue(west))
                    .setValue(south, state.getValue(north))
                    .setValue(west, state.getValue(east));
            case COUNTERCLOCKWISE_90 -> state.setValue(north, state.getValue(east))
                    .setValue(east, state.getValue(south))
                    .setValue(south, state.getValue(west))
                    .setValue(west, state.getValue(north));
            case CLOCKWISE_90 -> state.setValue(north, state.getValue(west))
                    .setValue(east, state.getValue(north))
                    .setValue(south, state.getValue(east))
                    .setValue(west, state.getValue(south));
            default -> state;
        };
    }

    public static <T extends Comparable<T>> BlockState mirror(BlockState state, Property<T> north, Property<T> south, Property<T> east, Property<T> west, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(north, state.getValue(south)).setValue(south, state.getValue(north));
            case FRONT_BACK -> state.setValue(east, state.getValue(west)).setValue(west, state.getValue(east));
            default -> state;
        };
    }


    public static String recipeKeyNamespace = "polyfactory";
    public static ResourceKey<Recipe<?>> recipeKey(String s) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(recipeKeyNamespace, s));
    }

    public static <T extends Enum<T>> T nextEnum(T activeMode, T[] values, boolean next) {
        return values[(values.length + activeMode.ordinal() + (next ? 1 : -1)) % values.length];
    }

    public static <T extends Comparable<T>> Codec<T> propertyCodec(Property<T> property) {
        return Codec.stringResolver(property::getName, x -> property.getValue(x).orElse(property.getPossibleValues().getFirst()));
    }

    /*public static ItemStack fromNbtStack(RegistryWrapper.WrapperLookup lookup, NbtElement stack) {
        return stack instanceof NbtCompound compound && compound.isEmpty() ? ItemStack.EMPTY : ItemStack.fromNbt(lookup, stack).orElse(ItemStack.EMPTY);
    }*/

    public static <T> HolderSet<T> fakeTagList(TagKey<T> tag) {
        return new HolderSet<T>() {
            @Override
            public Stream<Holder<T>> stream() {
                return Stream.empty();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isBound() {
                return true;
            }

            @Override
            public Either<TagKey<T>, List<Holder<T>>> unwrap() {
                return Either.left(tag);
            }

            @Override
            public Optional<Holder<T>> getRandomElement(RandomSource random) {
                return Optional.empty();
            }

            @Override
            public Holder<T> get(int index) {
                return null;
            }

            @Override
            public boolean contains(Holder<T> entry) {
                return false;
            }

            @Override
            public boolean canSerializeIn(HolderOwner<T> owner) {
                return true;
            }

            @Override
            public Optional<TagKey<T>> unwrapKey() {
                return Optional.of(tag);
            }

            @NotNull
            @Override
            public Iterator<Holder<T>> iterator() {
                return Collections.emptyIterator();
            }
        };
    }

    public static boolean insertItemIntoSlots(ItemStack stack, List<Slot> slots, boolean fromLast) {
        boolean modified = false;

        if (fromLast) {
            slots = slots.reversed();
        }

        if (stack.isStackable()) {
            for (var slot : slots) {
                if (stack.isEmpty()) {
                    break;
                }

                if (slot.mayPlace(stack)) {
                    var stackInSlot = slot.getItem();
                    if (!stackInSlot.isEmpty() && ItemStack.isSameItemSameComponents(stack, stackInSlot)) {
                        var totalCount = stackInSlot.getCount() + stack.getCount();
                        var maxSize = slot.getMaxStackSize(stackInSlot);
                        if (totalCount <= maxSize) {
                            stack.setCount(0);
                            stackInSlot.setCount(totalCount);
                            slot.setChanged();
                            modified = true;
                        } else if (stackInSlot.getCount() < maxSize) {
                            stack.shrink(maxSize - stackInSlot.getCount());
                            stackInSlot.setCount(maxSize);
                            slot.setChanged();
                            modified = true;
                        }
                    }
                }
            }
        }

        if (!stack.isEmpty()) {
            for (var slot : slots) {
                if (slot.mayPlace(stack)) {
                    var stackInSlot = slot.getItem();
                    if (stackInSlot.isEmpty() && slot.mayPlace(stack)) {
                        var maxSize = slot.getMaxStackSize(stack);
                        slot.setByPlayer(stack.split(Math.min(stack.getCount(), maxSize)));
                        slot.setChanged();
                        modified = true;
                        break;
                    }
                }
            }
        }

        return modified;
    }

    public static <T> List<T> collect(Iterable<T> iterable) {
        var list = new ArrayList<T>();
        iterable.forEach(list::add);
        return list;
    }


    public enum MovableResult {
        SUCCESS_MOVABLE,
        SUCCESS_REGULAR,
        FAILURE
    }
}
