package eu.pb4.polyfactory.fluid;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.mixin.RecipeManagerAccessor;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.drain.DrainRecipe;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.SimpleGuiElement;
import eu.pb4.sgui.api.gui.GuiLike;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public interface FluidContainerUtil {
    static void tick(FluidContainer container, ServerLevel world, BlockPos pos, float temperature, Consumer<ItemStack> stack) {
        tick(container, world, Vec3.atCenterOf(pos), temperature, stack);
    }

    static void tick(FluidContainer container, ServerLevel world, Vec3 pos, float temperature, Consumer<ItemStack> stackConsumer) {
        var input = FluidContainerInput.of(container, temperature);
        var random = world.getRandom();
        var list = new ArrayList<Pair<ResourceKey<Recipe<?>>, List<ItemStack>>>();
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

            int appliedTimes = 0;
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
                            var t = stack.stack().create();
                            item.add(t);
                            stackConsumer.accept(t.copy());
                        }
                    }
                }

                appliedTimes++;

                if (!recipe.matches(input, world)) {
                    break;
                }
            }

            list.add(new Pair<>(entry.id(), item));

            if (appliedTimes > 0) {
                recipe.applyEffects(world, input, appliedTimes, null, pos);
            }
        }

        if (!list.isEmpty() && FactoryUtil.getClosestPlayer(world, BlockPos.containing(pos), 16) instanceof ServerPlayer serverPlayer) {
            for (var entry : list) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, entry.getFirst(), entry.getSecond());
            }
        }
    }

    @Nullable
    static InteractionResult interactWithInWorld(FluidExchangeHandler container, Player player, ItemStack stack, InteractionHand hand, FluidInteractionMode mode) {
        return interactWithInWorld(container, player, stack, hand, mode, FluidInteractionMode.ANY);
    }

    @Nullable
    static InteractionResult interactWithInWorld(FluidExchangeHandler container, Player player, ItemStack stack, InteractionHand hand, FluidInteractionMode mode, FluidInteractionMode preferredMode) {
        return interactWithInWorld(container, player, stack, hand, mode, preferredMode, _ -> {});
    }

    @Nullable
    static InteractionResult interactWithInWorld(FluidExchangeHandler container, Player player, ItemStack stack, InteractionHand hand, FluidInteractionMode mode, FluidInteractionMode preferredMode,
                                                 Consumer<Optional<RecipeHolder<DrainRecipe>>> recipeConsumer) {
        var x = interactWith(container, player, stack, mode, preferredMode, recipeConsumer);
        if (x == null) {
            return null;
        }

        if (stack == x) {
            return InteractionResult.SUCCESS_SERVER;
        }

        if (stack.isEmpty()) {
            player.setItemInHand(hand, x);
        } else if (!x.isEmpty()) {
            if (player.isCreative()) {
                if (!player.getInventory().contains(x)) {
                    player.getInventory().add(x);
                }
            } else {
                player.getInventory().placeItemBackInInventory(x);
            }
        }

        return InteractionResult.SUCCESS_SERVER;

    }

    @Nullable
    static ItemStack interactWith(FluidExchangeHandler container, Player player, ItemStack stack) {
        return interactWith(container, player, stack, true, true);
    }

    @Nullable
    static ItemStack interactWith(FluidExchangeHandler container, Player player, ItemStack stack, boolean canInsert, boolean canExtract) {
        return interactWith(container, player, stack, FluidInteractionMode.get(canInsert, canExtract), FluidInteractionMode.ANY);
    }

    @Nullable
    static ItemStack interactWith(FluidExchangeHandler container, Player player, ItemStack stack, FluidInteractionMode interaction, FluidInteractionMode preferredInteraction) {
        return interactWith(container, player, stack, interaction, preferredInteraction, _ -> {});
    }

    static ItemStack interactWith(FluidExchangeHandler container, Player player, ItemStack stack, FluidInteractionMode interaction, FluidInteractionMode preferredInteraction,
                                  Consumer<Optional<RecipeHolder<DrainRecipe>>> recipeId) {
        if (interaction == FluidInteractionMode.NONE) {
            // Useless, isn't it?
            return null;
        }

        if (stack.is(FactoryItemTags.DYNAMIC_FLUID_INTERACTION)) {
            var fluids = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            var maxTransfer = stack.getOrDefault(FactoryDataComponents.CANISTER_TRANSFER_AMOUNT, Long.MAX_VALUE);

            interaction = interaction.and(preferredInteraction);

            if (interaction == FluidInteractionMode.NONE) {
                return null;
            }

            if (interaction == FluidInteractionMode.ANY) {
                interaction = fluids.isEmpty() ? FluidInteractionMode.EXTRACT : FluidInteractionMode.INSERT;
            }

            if (interaction == FluidInteractionMode.INSERT && container.isNotFull() && fluids.isNotEmpty()) {
                var fluid = Objects.requireNonNull(fluids.topFluid());
                var extract = Math.min(fluids.get(fluid), maxTransfer);
                var leftover = container.insert(fluid, extract, false);
                if (leftover != extract) {
                    if (!fluids.isInfinite()) {
                        stack.set(FactoryDataComponents.FLUID, fluids.with(fluid, leftover));
                    }
                    FactoryUtil.playSoundToPlayer(player, fluid.insertSoundEvent(), SoundSource.BLOCKS, 0.5f, 1f);
                    recipeId.accept(Optional.empty());
                }

                return stack;
            } else if (interaction == FluidInteractionMode.EXTRACT && container.isNotEmpty() && fluids.isNotFull()) {
                var topFluid = Objects.requireNonNull(container.topFluid());

                var maxAmount = Math.min(fluids.capacity() - fluids.stored(), maxTransfer);
                var extract = container.extract(topFluid, maxAmount, false);
                if (extract != 0) {
                    if (!fluids.isInfinite()) {
                        stack.set(FactoryDataComponents.FLUID, fluids.insert(topFluid, extract, false).component());
                    }

                    FactoryUtil.playSoundToPlayer(player, topFluid.extractSoundEvent(), SoundSource.BLOCKS, 0.5f, 1f);
                    recipeId.accept(Optional.empty());
                }

                return stack;
            }

            return null;
        }

        var copy = stack.copy();
        var input = DrainInput.of(copy, ItemStack.EMPTY, container, !(player instanceof FakePlayer));
        var optional = ((ServerLevel) player.level()).recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, player.level());
        if (optional.isEmpty()) {
            return null;
        }

        recipeId.accept(optional);

        var recipe = optional.get().value();

        var recipeInput = recipe.fluidInput(input);
        var recipeOutput = recipe.fluidOutput(input);

        if (!recipeInput.isEmpty() && !interaction.canExtract() || !recipeOutput.isEmpty() && !interaction.canInsert()) {
            return null;
        }

        var itemOut = recipe.assemble(input);
        for (var fluid : recipeInput) {
            container.extract(fluid, false);
        }
        stack.consume(1, player);
        for (var fluid : recipeOutput) {
            container.insert(fluid, false);
        }
        FactoryUtil.playSoundToPlayer(player, recipe.soundEvent().value(), SoundSource.BLOCKS, 0.5f, 1f);
        return itemOut;
    }

    static GuiElement guiElement(@Nullable FluidExchangeHandler container, boolean interactable) {
        return guiElement(container, interactable, interactable);
    }

    static GuiElement guiElement(@Nullable FluidExchangeHandler container, boolean canInsert, boolean canExtract) {
        if (container == null) {
            return SimpleGuiElement.EMPTY;
        }
        return new GuiElement() {
            //private int previousScrollValue = -1;
            //private boolean selected = false;

            @Override
            public ClickCallback getGuiCallback() {
                return canExtract || canInsert ? (index, type, action, gui) -> {
                    var handler = gui.getPlayer().containerMenu;
                    var carried = handler.getCarried();
                    var mode = switch (type) {
                        case MOUSE_LEFT, MOUSE_LEFT_SHIFT -> FluidInteractionMode.EXTRACT;
                        case MOUSE_RIGHT, MOUSE_RIGHT_SHIFT -> FluidInteractionMode.INSERT;
                        default -> null;
                    };

                    if (mode == null) {
                        return;
                    }

                    var out = interactWith(container, gui.getPlayer(), carried, FluidInteractionMode.get(canInsert, canExtract), mode);

                    if (out != null && carried != out) {
                        if (carried.isEmpty()) {
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
                    }
                } : GuiElement.EMPTY_CALLBACK;
            }

            @Override
            public ItemStack getItemStack() {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack getItemStackForDisplay(GuiLike gui) {
                var hasCanister = false;//gui.getPlayer().containerMenu.getCarried().getItem() instanceof CanisterItem;

                var b = (hasCanister ? GuiTextures.EMPTY_BUNDLE_BUILDER : GuiTextures.EMPTY_BUILDER).get()
                        .setComponent(DataComponents.BUNDLE_CONTENTS, hasCanister ? new BundleContents(List.of(
                                new ItemStackTemplate(Items.STONE),
                                new ItemStackTemplate(Items.STONE),
                                new ItemStackTemplate(Items.STONE, 61)
                        )) : BundleContents.EMPTY)
                        .hideDefaultTooltip()
                        .setName(Component.empty().append(FactoryUtil.fluidTextGeneric(container.stored())).append(" / ").append(FactoryUtil.fluidTextGeneric(container.capacity())));

                container.forEachReversed((type, amount) -> {
                    b.addLoreLine(type.toLabeledAmount(amount).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
                });
                return b.asStack();
            }

            /*@Override
            public boolean onSetSelectedBundleItemIndex(SlotBasedGui gui, int slotIndex, int selectedItemIndex) {
                var carried = gui.getPlayer().containerMenu.getCarried();
                var itemFluids = carried.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);

                var hasCanister = carried.getItem() instanceof CanisterItem;

                if (selectedItemIndex != -1) {
                    var diff = selectedItemIndex - this.previousScrollValue;
                    var dir = Mth.sign(diff) * (Math.abs(diff) != 1 ? 1 : -1);

                    if (hasCanister) {
                        if (dir > 0) {
                            if (itemFluids.isNotEmpty() && container.isNotFull()) {
                                var fluid = itemFluids.topFluid();
                                var extract = itemFluids.extract(fluid, Math.min(FluidConstants.BLOCK / 20, container.empty()), false);
                                container.insert(fluid, extract.fluidAmount(), false);
                                carried.set(FactoryDataComponents.FLUID, extract.component());
                            }
                        } else if (dir < 0) {
                            if (itemFluids.isNotFull() && container.isNotEmpty()) {
                                var fluid = container.topFluid();
                                var extract = container.extract(fluid, Math.min(FluidConstants.BLOCK / 20, itemFluids.empty()), false);
                                carried.set(FactoryDataComponents.FLUID, itemFluids.insert(fluid, extract, false).component());
                            }
                        }

                    }
                } else {
                    this.selected = !this.selected;
                }
                this.previousScrollValue = selectedItemIndex;
                return true;
            }*/
        };
    }
}
