package eu.pb4.polyfactory.fluid;

import com.mojang.datafixers.types.templates.Tag;
import com.mojang.serialization.RecordBuilder;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.UniversalFluidContainerItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class FluidContainer {
    private final Object2LongMap<FluidInstance<?>> storedFluids = new Object2LongOpenHashMap<>();
    private final List<FluidInstance<?>> fluids = new ArrayList<>();
    private final long capacity;
    private final Runnable markDirty;
    private int updateId = 0;
    private final BiPredicate<FluidContainer, FluidInstance<?>> canInsert;
    private long stored = 0;

    public FluidContainer(long maxStorage, Runnable markDirty) {
        this(maxStorage, markDirty, (a, x) -> true);
    }
    public FluidContainer(long maxStorage, Runnable markDirty, BiPredicate<FluidContainer, FluidInstance<?>> canInsert) {
        this.capacity = maxStorage;
        this.canInsert = canInsert;
        this.markDirty = markDirty;
    }

    public static FluidContainer singleFluid(long maxStorage, Runnable markDirty) {
        return new FluidContainer(maxStorage, markDirty, (self, type) -> self.isEmpty() || self.contains(type));
    }

    public static FluidContainer onlyInTag(long maxStorage, TagKey<FluidType<?>> tag, Runnable markDirty) {
        return new FluidContainer(maxStorage, markDirty, (self, type) -> type.isIn(tag));
    }

    public boolean isEmpty() {
        return this.stored == 0;
    }

    public void tick(ServerWorld world, BlockPos pos, float temperature, Consumer<ItemStack> stack) {
        if (contains(FactoryFluids.WATER.defaultInstance()) && contains(FactoryFluids.LAVA.defaultInstance())) {
            this.extract(FactoryFluids.WATER.defaultInstance(), 5000, false);
            this.extract(FactoryFluids.LAVA.defaultInstance(), 2000, false);

            if (world.getRandom().nextFloat() > 0.5) {
                stack.accept(new ItemStack(Items.FLINT));
            }

            if (world.getTime() % 10 == 0) {
                world.spawnParticles(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.1);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.1);

                world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS);
            }
        } else if (temperature > 0.5) {
            this.extract(FactoryFluids.WATER.defaultInstance(), (long) (temperature * 10), false);
            if (world.getRandom().nextFloat() > 0.5 && world.getTime() % 5 == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }


    @Nullable
    public ItemStack interactWith(ServerPlayerEntity player, ItemStack stack) {
        if (stack.getItem() instanceof UniversalFluidContainerItem item) {
            var mode = stack.getOrDefault(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT);
            var fluids = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            var topFluid = this.topFluid();
            if (mode == FluidInteractionMode.EXTRACT && topFluid != null) {
                var maxAmount = item.capacity() - fluids.stored();
                var extract = this.extract(topFluid, maxAmount, false);
                stack.set(FactoryDataComponents.FLUID, fluids.insert(topFluid, extract));
            } else if (mode == FluidInteractionMode.INSERT) {
                for (var fluid : fluids.fluids()) {
                    var extract = fluids.get(fluid);
                    var leftover = this.insert(fluid, extract, false);
                    if (leftover != extract) {
                        stack.set(FactoryDataComponents.FLUID, fluids.with(fluid, leftover));
                        break;
                    }
                }
            }

            return ItemStack.EMPTY;
        }

        var copy = stack.copy();
        var input = DrainInput.of(copy, ItemStack.EMPTY, this, !(player instanceof FakePlayer));
        var optional = player.getWorld().getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, player.getWorld());
        if (optional.isEmpty()) {
            return null;
        }
        var recipe = optional.get().value();
        var itemOut = recipe.craft(input, player.getRegistryManager());
        for (var fluid : recipe.fluidInput(input)) {
            this.extract(fluid, false);
        }
        stack.decrementUnlessCreative(1, player);
        for (var fluid : recipe.fluidOutput(input)) {
            this.insert(fluid, false);
        }
        player.playSoundToPlayer(recipe.soundEvent().value(), SoundCategory.BLOCKS, 0.5f, 1f);
        return itemOut;
    }

    public boolean canInsert(FluidStack<?> stack, boolean strict) {
        return canInsert(stack.instance(), stack.amount(), strict);
    }

    public boolean canExtract(FluidStack<?> stack, boolean strict) {
        return canExtract(stack.instance(), stack.amount(), strict);
    }

    public void insertExact(FluidInstance<?> instance, long amount) {
        insert(instance, amount, true);
    }

    public long insert(FluidStack<?> stack, boolean strict) {
        return insert(stack.instance(), stack.amount(), strict);
    }

    public long extract(FluidStack<?> stack, boolean strict) {
        return extract(stack.instance(), stack.amount(), strict);
    }

    public long get(FluidInstance<?> type) {
        return this.storedFluids.getOrDefault(type, 0);
    }

    public long set(FluidInstance<?> type, long amount) {
        if (amount == 0) {
            this.fluids.remove(type);
            var current = this.storedFluids.removeLong(amount);
            this.stored -= current;
            this.markDirty.run();
            return current;
        }
        var current = this.storedFluids.put(type, amount);
        this.stored += amount - current;
        if (current == 0) {
            this.fluids.add(type);
            this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        }
        this.markDirty.run();
        return current;
    }

    public boolean doesNotContain(FluidInstance<?> type) {
        return !contains(type);
    }
    public boolean contains(FluidInstance<?> type) {
        return this.storedFluids.getOrDefault(type, 0) > 0;
    }

    public boolean canInsert(FluidInstance<?> type, long amount, boolean exact) {
        return this.canInsert.test(this, type) && (exact ? this.stored + amount <= this.capacity : this.stored != this.capacity);
    }

    public long insert(FluidInstance<?> type, long amount, boolean exact) {
        if (!canInsert(type, amount, exact) || amount == 0) {
            return amount;
        }

        var next = Math.min(this.stored + amount, this.capacity);
        var inserted = next - this.stored;

        var current = this.storedFluids.getOrDefault(type, 0);
        this.storedFluids.put(type, current + inserted);
        if (current == 0) {
            this.fluids.add(type);
            this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        }
        this.stored = next;
        this.updateId++;
        this.markDirty.run();
        return amount - inserted;
    }

    public boolean canExtract(FluidInstance<?> type, long amount, boolean exact) {
        return exact ? this.get(type) >= amount : this.get(type) != 0;
    }

    public long extract(FluidInstance<?> type, long amount, boolean exact) {
        if (!canExtract(type, amount, exact) || amount == 0) {
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
    public void provideRender(BiConsumer<FluidInstance<?>, Float> consumer) {
        forEachByDensity((a, b) -> consumer.accept(a, (float) (((double) b) / capacity)));
    }

    public void forEachByDensity(BiConsumer<FluidInstance<?>, Long> consumer) {
        for (var f : this.fluids) {
            consumer.accept(f, this.storedFluids.getOrDefault(f, 0));
        }
    }

    public void forEach(BiConsumer<FluidInstance<?>, Long> consumer) {
        this.storedFluids.forEach(consumer);
    }

    public long capacity() {
        return this.capacity;
    }

    public long stored() {
        return this.stored;
    }

    public NbtElement toNbt(RegistryWrapper.WrapperLookup lookup) {
        var nbt = new NbtList();
        var ops = lookup.getOps(NbtOps.INSTANCE);

        storedFluids.forEach((a, b) -> {
            var x = FluidInstance.CODEC.encode(a, ops, new RecordBuilder.MapBuilder<>(ops));
            x.add("amount", NbtLong.of(b));
            nbt.add(x.build(new NbtCompound()).getOrThrow());
        });
        return nbt;
    }

    public void fromNbt(RegistryWrapper.WrapperLookup lookup, NbtCompound base, String fluidKey) {
        var nbt = base.getList(fluidKey, NbtElement.COMPOUND_TYPE);
        var ops = lookup.getOps(NbtOps.INSTANCE);
        this.storedFluids.clear();
        this.fluids.clear();
        this.stored = 0;
        for (var t : nbt) {
            var cp = (NbtCompound) t;
            var type = FluidInstance.CODEC.decode(ops, ops.getMap(cp).getOrThrow()).getOrThrow();

            var value = cp.getLong("amount");
            if (value != 0) {
                this.storedFluids.put(type, value);
                this.stored += value;
            }
        }
        this.fluids.addAll(this.storedFluids.keySet());
        this.fluids.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        this.updateId++;
    }

    public int updateId() {
        return this.updateId;
    }

    public Object2LongMap<FluidInstance<?>> map() {
        return Object2LongMaps.unmodifiable(this.storedFluids);
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
    public FluidInstance<?> topFluid() {
        return this.fluids.isEmpty() ? null : this.fluids.getLast();
    }

    @Nullable
    public FluidInstance<?> bottomFluid() {
        return this.fluids.isEmpty() ? null : this.fluids.getFirst();
    }

    public float getFilledPercentage() {
        return (float) (((double) this.stored) / capacity);
    }

    public boolean isNotEmpty() {
        return !this.isEmpty();
    }

    public boolean isFull() {
        return this.stored >= this.capacity;
    }
    public boolean isNotFull() {
        return !isFull();
    }

    public long empty() {
        return Math.max(this.capacity - this.stored, 0);
    }

    public Object2LongMap<FluidInstance<?>> asMap() {
        return Object2LongMaps.unmodifiable(this.storedFluids);
    }

    public List<FluidInstance<?>> orderList() {
        return Collections.unmodifiableList(this.fluids);
    }

    public void clear() {
        this.storedFluids.clear();
        this.fluids.clear();
        this.stored = 0;
        this.markDirty.run();
    }
}
