package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.UniversalFluidContainerItem;
import eu.pb4.polyfactory.mixin.RecipeManagerAccessor;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface FluidContainerUtil {
    static void tick(FluidContainer container, ServerLevel world, BlockPos pos, float temperature, Consumer<ItemStack> stack) {
        tick(container, world, Vec3.atCenterOf(pos), temperature, stack);
    }
    static void tick(FluidContainer container, ServerLevel world, Vec3 pos, float temperature, Consumer<ItemStack> stackConsumer) {
        var input = FluidContainerInput.of(container, temperature);
        var random = world.random;
        var list = new ArrayList<Tuple<ResourceKey<Recipe<?>>, List<ItemStack>>>();
        for (var entry : ((RecipeManagerAccessor) world.recipeAccess()).getRecipes().byType(FactoryRecipeTypes.FLUID_INTERACTION)) {
            var recipe = entry.value();
            if (!entry.value().matches(input, world)) {
                continue;
            }
            if (recipe.particleChance(input) < random.nextFloat()) {
                var particle = recipe.particle(input, random);
                if (particle != null) {
                    world.sendParticles(particle, pos.x(), pos.y(), pos.z(), 0, 0.1, 0.1, 0.1, 0.1);
                }
                var sound = recipe.soundEvent(input, random);
                if (sound != null) {
                    world.playSound(null, pos.x(), pos.y(), pos.z(), sound, SoundSource.BLOCKS, 1, 1);
                }
            }
            var inputFluids = recipe.fluidInput(input, world.registryAccess());
            var outputFluids = recipe.fluidOutput(input, world.registryAccess());
            var outputItems = recipe.itemOutput(input, world.registryAccess());

            var item = new ArrayList<ItemStack>();
            for (var i = 0; i < recipe.maxApplyPerTick(); i++) {
                for (var stack : inputFluids) {
                    container.extract(stack.instance(), stack.used(), false);
                }

                for (var stack : outputFluids) {
                    container.insert(stack, false);
                }

                for (var stack : outputItems) {
                    for (var r = 0; r < stack.roll(); r++) {
                        if (stack.chance() < random.nextFloat()) {
                            item.add(stack.stack());
                            stackConsumer.accept(stack.stack().copy());
                        }
                    }
                }


                if (!recipe.matches(input, world)) {
                    break;
                }
            }

            list.add(new Tuple<>(entry.id(), item));
        }

        if (!list.isEmpty() && FactoryUtil.getClosestPlayer(world, BlockPos.containing(pos), 16) instanceof ServerPlayer serverPlayer) {
            for (var entry : list) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, entry.getA(), entry.getB());
            }
        }
    }

    static ItemStack interactWith(FluidContainer container, ServerPlayer player, ItemStack stack) {
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
        var optional = player.level().recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, player.level());
        if (optional.isEmpty()) {
            return null;
        }
        var recipe = optional.get().value();
        var itemOut = recipe.assemble(input, player.registryAccess());
        for (var fluid : recipe.fluidInput(input)) {
            container.extract(fluid, false);
        }
        stack.consume(1, player);
        for (var fluid : recipe.fluidOutput(input)) {
            container.insert(fluid, false);
        }
        FactoryUtil.playSoundToPlayer(player,recipe.soundEvent().value(), SoundSource.BLOCKS, 0.5f, 1f);
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
                    var handler = gui.getPlayer().containerMenu;
                    var out = interactWith(container, gui.getPlayer(), handler.getCarried());
                    if (out == null) {
                        return;
                    }
                    if (handler.getCarried().isEmpty()) {
                        handler.setCarried(out);
                    } else if (!out.isEmpty()) {
                        if (gui.getPlayer().isCreative()) {
                            if (!gui.getPlayer().getInventory().contains(out)) {
                                gui.getPlayer().getInventory().add(out);
                            }
                        } else {
                            gui.getPlayer().getInventory().placeItemBackInInventory(out);
                        }
                    }
                } : GuiElementInterface.EMPTY_CALLBACK;
            }

            @Override
            public ItemStack getItemStack() {
                var b = GuiTextures.EMPTY_BUILDER.get()
                        .setName(Component.empty().append(FactoryUtil.fluidTextGeneric(container.stored())).append(" / ").append(FactoryUtil.fluidTextGeneric(container.capacity())));

                container.forEachReversed((type, amount) -> {
                    b.addLoreLine(type.toLabeledAmount(amount).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
                });
                return b.asStack();
            }
        };
    }
}
