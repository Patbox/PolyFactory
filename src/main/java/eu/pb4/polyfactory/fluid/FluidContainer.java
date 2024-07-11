package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.mixin.util.BucketItemAccessor;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMaps;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FluidContainer {
    private final Reference2LongMap<FluidType> storedFluids = new Reference2LongOpenHashMap<>();
    private final List<FluidType> fluids = new ReferenceArrayList<>();
    private final long capacity;
    private final Runnable markDirty;
    private int updateId = 0;
    private final BiPredicate<FluidContainer, FluidType> canInsert;
    private long stored = 0;

    public FluidContainer(long maxStorage, Runnable markDirty) {
        this(maxStorage, markDirty, (a, x) -> true);
    }
    public FluidContainer(long maxStorage, Runnable markDirty, BiPredicate<FluidContainer, FluidType> canInsert) {
        this.capacity = maxStorage;
        this.canInsert = canInsert;
        this.markDirty = markDirty;
    }

    public static FluidContainer singleFluid(long maxStorage, Runnable markDirty) {
        return new FluidContainer(maxStorage, markDirty, (self, type) -> self.isEmpty() || self.contains(type));
    }

    public boolean isEmpty() {
        return this.stored == 0;
    }

    public void tick(ServerWorld world, BlockPos pos, float temperature, Consumer<ItemStack> stack) {
        if (contains(FactoryFluids.WATER) && contains(FactoryFluids.LAVA)) {
            this.extract(FactoryFluids.WATER, 5000, false);
            this.extract(FactoryFluids.LAVA, 2000, false);

            if (world.getRandom().nextFloat() > 0.5) {
                stack.accept(new ItemStack(Items.FLINT));
            }

            if (world.getTime() % 10 == 0) {
                world.spawnParticles(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.1);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.1);

                world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS);
            }
        } else if (temperature > 0.5) {
            this.extract(FactoryFluids.WATER, (long) (temperature * 10), false);
            if (world.getRandom().nextFloat() > 0.5 && world.getTime() % 5 == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }


    @Nullable
    public ItemStack interactWith(ServerPlayerEntity player, ItemStack stack) {
        var inserts = FluidItemBehaviours.FLUID_INSERT.get(stack.getItem());
        if (inserts != null) {
            for (var x : inserts) {
                if (x.predicate().test(stack)) {
                    if (this.canInsert(x.fluids(), true)) {
                        stack.decrementUnlessCreative(1, player);
                        this.insert(x.fluids(), true);
                        player.playSoundToPlayer(x.sound(), SoundCategory.BLOCKS, 1, 1);
                        return x.result().copy();
                    }
                    return ItemStack.EMPTY;
                }
            }
        }

        var extractMap = FluidItemBehaviours.FLUID_EXTRACT.get(stack.getItem());
        if (extractMap != null) {
            for (var f : this.storedFluids.keySet()) {
                var extracts = extractMap.get(f);
                if (extracts != null) {
                    for (var x : extracts) {
                        if (x.predicate().test(stack)) {
                            if (this.canExtract(x.fluids(), true)) {
                                stack.decrementUnlessCreative(1, player);
                                this.extract(x.fluids(), true);
                                player.playSoundToPlayer(x.sound(), SoundCategory.BLOCKS, 1, 1);
                                return x.result().copy();
                            }
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
        }



        return null;
    }

    public boolean canInsert(FluidStack stack, boolean strict) {
        return canInsert(stack.type(), stack.amount(), strict);
    }

    public boolean canExtract(FluidStack stack, boolean strict) {
        return canExtract(stack.type(), stack.amount(), strict);
    }

    public long insert(FluidStack stack, boolean strict) {
        return insert(stack.type(), stack.amount(), strict);
    }

    public long extract(FluidStack stack, boolean strict) {
        return extract(stack.type(), stack.amount(), strict);
    }

    public long get(FluidType type) {
        return this.storedFluids.getOrDefault(type, 0);
    }

    public long set(FluidType type, long amount) {
        return amount == 0 ? this.storedFluids.removeLong(amount) : this.storedFluids.put(type, amount);
    }

    public boolean contains(FluidType type) {
        return this.storedFluids.getOrDefault(type, 0) > 0;
    }

    public boolean canInsert(FluidType type, long amount, boolean exact) {
        return this.canInsert.test(this, type) && (exact ? this.stored + amount <= this.capacity : this.stored != this.capacity);
    }

    public long insert(FluidType type, long amount, boolean exact) {
        if (!canInsert(type, amount, exact)) {
            return amount;
        }

        var next = Math.min(this.stored + amount, this.capacity);
        var inserted = next - this.stored;

        var current = this.storedFluids.getOrDefault(type, 0);
        this.storedFluids.put(type, current + inserted);
        if (current == 0) {
            this.fluids.add(type);
            this.fluids.sort(FluidType.DENSITY_COMPARATOR_REVERSED);
        }
        this.stored = next;
        this.updateId++;
        this.markDirty.run();
        return amount - inserted;
    }

    public boolean canExtract(FluidType type, long amount, boolean exact) {
        return exact ? this.get(type) >= amount : this.get(type) != 0;
    }

    public long extract(FluidType type, long amount, boolean exact) {
        if (!canExtract(type, amount, exact)) {
            return 0;
        }

        var current = this.get(type);
        var extracted = Math.min(current, amount);

        if (current == extracted) {
            this.storedFluids.removeLong(type);
            this.fluids.remove(type);
        } else {
            this.storedFluids.put(type, current - extracted);
        }
        this.stored -= extracted;
        this.updateId++;
        this.markDirty.run();
        return extracted;
    }
    public void provideRender(BiConsumer<FluidType, Float> consumer) {
        forEachByDensity((a, b) -> consumer.accept(a, (float) (((double) b) / capacity)));
    }

    public void forEachByDensity(BiConsumer<FluidType, Long> consumer) {
        for (var f : this.fluids) {
            consumer.accept(f, this.storedFluids.getOrDefault(f, 0));
        }
    }

    public void forEach(BiConsumer<FluidType, Long> consumer) {
        this.storedFluids.forEach(consumer);
    }

    public long capacity() {
        return this.capacity;
    }

    public long stored() {
        return this.stored;
    }

    public NbtCompound toNbt() {
        var nbt = new NbtCompound();
        storedFluids.forEach((a, b) -> nbt.putLong(Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.getId(a)).toString(), b));
        return nbt;
    }

    public void fromNbt(NbtCompound nbt) {
        this.storedFluids.clear();
        this.fluids.clear();
        this.stored = 0;
        for (var key : nbt.getKeys()) {
            var value = nbt.getLong(key);
            var type = FactoryRegistries.FLUID_TYPES.get(Identifier.tryParse(key));
            if (type != null && value != 0) {
                this.storedFluids.put(type, value);
                this.stored += value;
            }
        }
        this.fluids.addAll(this.storedFluids.keySet());
        this.fluids.sort(FluidType.DENSITY_COMPARATOR_REVERSED);
        this.updateId++;
    }

    public int updateId() {
        return this.updateId;
    }

    public Reference2LongMap<FluidType> map() {
        return Reference2LongMaps.unmodifiable(this.storedFluids);
    }

    public GuiElementInterface guiElement(boolean interactable) {
        return new GuiElementInterface() {
            @Override
            public ClickCallback getGuiCallback() {
                return interactable ? (index, type, action, gui) -> {
                    var handler = gui.getPlayer().currentScreenHandler;
                    var out = interactWith(gui.getPlayer(), handler.getCursorStack());
                    if (out == null) {
                        return;
                    }
                    if (handler.getCursorStack().isEmpty()) {
                        handler.setCursorStack(out);
                    } else if (!out.isEmpty()) {
                        if (gui.getPlayer().isCreative()) {
                            if (!gui.getPlayer().getInventory().contains(out)) {
                                gui.getPlayer().getInventory().insertStack(out);
                            }
                        } else {
                            gui.getPlayer().getInventory().offerOrDrop(out);
                        }
                    }
                } : GuiElementInterface.EMPTY_CALLBACK;
            }

            @Override
            public ItemStack getItemStack() {
                var b = GuiTextures.EMPTY_BUILDER.get()
                        .setName(Text.empty().append(FactoryUtil.fluidText(stored)).append(" / ").append(FactoryUtil.fluidText(capacity)));

                forEach((type, amount) -> {
                    b.addLoreLine(type.toLabeledAmount(amount).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
                });
                return b.asStack();
            }
        };
    }

    @Nullable
    public FluidType topFluid() {
        return this.fluids.isEmpty() ? null : this.fluids.get(0);
    }

    public float getFilledPercentage() {
        return (float) (((double) this.stored) / capacity);
    }

    public boolean isNotEmpty() {
        return !this.isEmpty();
    }
}
