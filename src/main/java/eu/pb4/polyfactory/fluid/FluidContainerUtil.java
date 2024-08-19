package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.UniversalFluidContainerItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface FluidContainerUtil {
    static void tick(FluidContainer container, ServerWorld world, BlockPos pos, float temperature, Consumer<ItemStack> stack) {
        tick(container, world, Vec3d.ofCenter(pos), temperature, stack);
    }
    static void tick(FluidContainer container, ServerWorld world, Vec3d pos, float temperature, Consumer<ItemStack> stack) {
        if (container.contains(FactoryFluids.WATER.defaultInstance()) && container.contains(FactoryFluids.LAVA.defaultInstance())) {
            container.extract(FactoryFluids.WATER.defaultInstance(), 5000, false);
            container.extract(FactoryFluids.LAVA.defaultInstance(), 2000, false);

            if (world.getRandom().nextFloat() > 0.5) {
                stack.accept(new ItemStack(Items.FLINT));
            }

            if (world.getTime() % 10 == 0) {
                world.spawnParticles(ParticleTypes.WHITE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 5, 0.1, 0.1, 0.1, 0.1);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX(), pos.getY(), pos.getZ(), 5, 0.1, 0.1, 0.1, 0.1);

                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
            }
        } else if (temperature > 0.5) {
            var x = container.extract(FactoryFluids.WATER.defaultInstance(), (long) (temperature * 10), false);
            if (x != 0 && world.getRandom().nextFloat() > 0.5 && world.getTime() % 5 == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX(), pos.getY(), pos.getZ(), 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
    }

    static ItemStack interactWith(FluidContainer container, ServerPlayerEntity player, ItemStack stack) {
        if (stack.getItem() instanceof UniversalFluidContainerItem item) {
            var mode = stack.getOrDefault(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT);
            var fluids = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            var topFluid = container.topFluid();
            if (mode == FluidInteractionMode.EXTRACT && topFluid != null) {
                var maxAmount = item.capacity() - fluids.stored();
                var extract = container.extract(topFluid, maxAmount, false);
                stack.set(FactoryDataComponents.FLUID, fluids.insert(topFluid, extract, false).component());
            } else if (mode == FluidInteractionMode.INSERT) {
                for (var fluid : fluids.fluids()) {
                    var extract = fluids.get(fluid);
                    var leftover = container.insert(fluid, extract, false);
                    if (leftover != extract) {
                        stack.set(FactoryDataComponents.FLUID, fluids.with(fluid, leftover));
                        break;
                    }
                }
            }

            return ItemStack.EMPTY;
        }

        var copy = stack.copy();
        var input = DrainInput.of(copy, ItemStack.EMPTY, container, !(player instanceof FakePlayer));
        var optional = player.getWorld().getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, player.getWorld());
        if (optional.isEmpty()) {
            return null;
        }
        var recipe = optional.get().value();
        var itemOut = recipe.craft(input, player.getRegistryManager());
        for (var fluid : recipe.fluidInput(input)) {
            container.extract(fluid, false);
        }
        stack.decrementUnlessCreative(1, player);
        for (var fluid : recipe.fluidOutput(input)) {
            container.insert(fluid, false);
        }
        player.playSoundToPlayer(recipe.soundEvent().value(), SoundCategory.BLOCKS, 0.5f, 1f);
        return itemOut;
    }

    static GuiElementInterface guiElement(@Nullable FluidContainer container, boolean interactable) {
        if (container == null) {
            return GuiElement.EMPTY;
        }
        return new GuiElementInterface() {
            @Override
            public ClickCallback getGuiCallback() {
                return interactable ? (index, type, action, gui) -> {
                    var handler = gui.getPlayer().currentScreenHandler;
                    var out = interactWith(container, gui.getPlayer(), handler.getCursorStack());
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
                        .setName(Text.empty().append(FactoryUtil.fluidText(container.stored())).append(" / ").append(FactoryUtil.fluidText(container.capacity())));

                container.forEach((type, amount) -> {
                    b.addLoreLine(type.toLabeledAmount(amount).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
                });
                return b.asStack();
            }
        };
    }
}
