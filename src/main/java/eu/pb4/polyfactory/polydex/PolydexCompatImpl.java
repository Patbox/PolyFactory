package eu.pb4.polyfactory.polydex;

import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.view.crafting.ShapelessCraftingRecipePage;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.polydex.pages.*;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.recipe.*;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.casting.SimpleCauldronCastingRecipe;
import eu.pb4.polyfactory.recipe.grinding.SimpleGrindingRecipe;
import eu.pb4.polyfactory.recipe.grinding.StrippingGrindingRecipe;
import eu.pb4.polyfactory.recipe.mixing.TransformMixingRecipe;
import eu.pb4.polyfactory.recipe.smeltery.SimpleSmelteryRecipe;
import eu.pb4.polyfactory.recipe.drain.SimpleDrainRecipe;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.BrewingMixingRecipe;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.DebugTextProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.sgui.api.elements.GuiElement;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static eu.pb4.polyfactory.ModInit.id;

public class PolydexCompatImpl {
    private static final HoverDisplayBuilder.ComponentType MACHINE_STATE = Util.make(() -> {
        //noinspection ResultOfMethodCallIgnored
        HoverDisplayBuilder.NAME.index();

        return HoverDisplayBuilder.ComponentType.of(id("machine_state"), true);
    });
    private static final HoverDisplayBuilder.ComponentType FILLED = HoverDisplayBuilder.ComponentType.of(id("filled_amount"), HoverDisplayBuilder.ComponentType.Visibility.ALWAYS);
    private static final HoverDisplayBuilder.ComponentType DEBUG_DATA = HoverDisplayBuilder.ComponentType.of(id("debug_data"), HoverDisplayBuilder.ComponentType.Visibility.NEVER);

    public static void register() {
        PolydexPage.registerRecipeViewer(GenericPressRecipe.class, PressRecipePage::new);
        PolydexPage.registerRecipeViewer(GenericMixingRecipe.class, GenericMixerRecipePage::new);
        PolydexPage.registerRecipeViewer(TransformMixingRecipe.class, TransformMixerRecipePage::new);
        PolydexPage.registerRecipeViewer(BrewingMixingRecipe.class, BrewingMixerRecipePage::new);
        PolydexPage.registerRecipeViewer(SimpleGrindingRecipe.class, SimpleGrindingRecipePage::new);
        PolydexPage.registerRecipeViewer(StrippingGrindingRecipe.class, StrippingGrindingRecipePage::new);
        PolydexPage.registerRecipeViewer(ColoringCraftingRecipe.class, ColoringCraftingRecipePage::new);
        //noinspection unchecked
        PolydexPage.registerRecipeViewer(ShapelessNbtCopyRecipe.class, (x) -> new ShapelessCraftingRecipePage((RecipeEntry<ShapelessRecipe>) (Object) x));
        PolydexPage.registerRecipeViewer(SimpleSpoutRecipe.class, SimpleSpoutRecipePage::new);
        PolydexPage.registerRecipeViewer(SimpleCastingRecipe.class, SimpleCastingRecipePage::new);
        PolydexPage.registerRecipeViewer(SimpleCauldronCastingRecipe.class, SimpleCauldronCastingRecipePage::new);
        PolydexPage.registerRecipeViewer(SimpleDrainRecipe.class, SimpleDrainRecipePage::new);
        PolydexPage.registerRecipeViewer(SimpleSmelteryRecipe.class, SimpleSmelteryRecipePage::new);
        //noinspection RedundantCast
        PolydexPage.registerRecipeViewer((Class<? extends Recipe<?>>) (Object) CraftingWithLeftoverRecipe.class,
                x -> PolydexImpl.RECIPE_VIEWS.get(((CraftingWithLeftoverRecipe) x.value()).backingRecipe().getClass()).apply(new RecipeEntry<Recipe<?>>(
                        (RegistryKey<Recipe<?>>) (Object) x.id(),
                        (Recipe<?>) (Object) ((CraftingWithLeftoverRecipe) x.value()).backingRecipe())));
        PolydexPage.register(PolydexCompatImpl::createPages);

        PolydexEntry.registerProvider(PolydexCompatImpl::createFluidEntries);
        PolydexEntry.registerEntryCreator(FactoryItems.SPRAY_CAN, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.CABLE, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.INVERTED_LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.FIXTURE_LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.INVERTED_FIXTURE_LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.CAGED_LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.INVERTED_CAGED_LAMP, PolydexCompatImpl::seperateColoredItems);
        PolydexEntry.registerEntryCreator(FactoryItems.BRITTLE_POTION, PolydexImpl::seperateCustomPotion);
        PolydexEntry.registerEntryCreator(FactoryItems.PORTABLE_FLUID_TANK, PolydexCompatImpl::seperateFluidItems);
        HoverDisplayBuilder.register(PolydexCompatImpl::stateAccurateNames);
    }

    private static void createFluidEntries(MinecraftServer server, PolydexEntry.EntryConsumer consumer) {
        /*for (var fluid : FactoryRegistries.FLUID_TYPES) {
            if (fluid.dataCodec() == Unit.CODEC) {
                consumer.accept(PolydexEntry.of(FactoryRegistries.FLUID_TYPES.getId(fluid), new PolydexFluidStack(fluid.defaultInstance(), 0, 1)));
            }
        }

        for (var potion : Registries.POTION.getIndexedEntries()) {
            consumer.accept(PolydexEntry.of(id("potion/" + potion.getKey().get().getValue().toUnderscoreSeparatedString()), new PolydexFluidStack(FactoryFluids.POTION.toInstance(PotionContentsComponent.DEFAULT.with(potion)), 0, 1)));
        }*/
    }

    private static void createPages(MinecraftServer server, Consumer<PolydexPage> polydexPageConsumer) {

    }

    private static PolydexEntry seperateColoredItems(ItemStack stack) {
        var color = ColoredItem.getColor(stack);
        var dye = DyeColorExtra.BY_COLOR.get(color);
        Identifier baseId = Registries.ITEM.getId(stack.getItem());
        if (dye != null) {
            baseId = baseId.withSuffixedPath("/" + dye.asString());
        }
        return PolydexEntry.of(baseId, stack, PolydexCompatImpl::isSameColoredObject);
    }

    private static boolean isSameColoredObject(PolydexEntry polydexEntry, PolydexStack<?> polydexStack) {
        if (polydexStack.getBacking() instanceof ItemStack stack) {
            var base = (ItemStack) polydexEntry.stack().getBacking();
            if (!base.isOf(stack.getItem())) {
                return false;
            }

            if (ColoredItem.hasColor(base) && !ColoredItem.hasColor(stack)) {
                return true;
            }

            return ColoredItem.getColor(base) == ColoredItem.getColor(stack);
        }

        return polydexEntry.stack().matches(polydexStack, true);
    }

    private static PolydexEntry seperateFluidItems(ItemStack stack) {
        var id = Registries.ITEM.getId(stack.getItem());
        var fluids = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
        if (fluids.isEmpty()) {
            return PolydexEntry.of(id, stack, PolydexCompatImpl::isSameFluidObject);
        }

        return PolydexEntry.of(id.withSuffixedPath("/" + fluids.fluids().stream().sorted(Comparator.comparing(x -> x.getName().getString())).map((instance) -> {
            var base = Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.getId(instance.type())).toUnderscoreSeparatedString();
            if (instance.type().defaultData() == Unit.INSTANCE) {
                return base;
            } else if (instance.data() instanceof PotionContentsComponent potion) {
                if (potion.potion().isPresent() && potion.potion().get().getKey().isPresent()) {
                    return base + "_" + potion.potion().get().getKey().get().getValue().toUnderscoreSeparatedString();
                }

                return base + "_" + potion.hashCode();
            }
            return base + "_" + instance.hashCode();
        }).collect(Collectors.joining("/"))), stack, PolydexCompatImpl::isSameFluidObject);
    }

    private static boolean isSameFluidObject(PolydexEntry polydexEntry, PolydexStack<?> polydexStack) {
        if (polydexStack.getBacking() instanceof ItemStack stack) {
            var base = (ItemStack) polydexEntry.stack().getBacking();
            if (!base.isOf(stack.getItem())) {
                return false;
            }

            return Objects.equals(stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT).fluids(),
                    base.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT).fluids());
        }

        return polydexEntry.stack().matches(polydexStack, true);
    }

    private static void stateAccurateNames(HoverDisplayBuilder hoverDisplayBuilder) {
        var target = hoverDisplayBuilder.getTarget();
        if (target.hasTarget() && target.entity() == null
                && target.blockState().getBlock() instanceof BlockStateNameProvider provider) {
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, provider.getName(target.player().getEntityWorld(), target.pos(), target.blockState(), target.blockEntity()));
        }

        if (target.blockState().getBlock() instanceof NetworkComponent.Rotational) {
            var rot = RotationUser.getRotation(target.player().getEntityWorld(), target.pos()).getStateText();
            if (rot != null) {
                hoverDisplayBuilder.setComponent(MACHINE_STATE, rot);
            }
        }

        BlockEntity entity = target.blockEntity();
        if (entity == null) {
            if (target.blockState().getBlock() instanceof MultiBlock multiBlock) {
                entity = target.player().getEntityWorld().getBlockEntity(multiBlock.getCenter(target.blockState(), target.pos()));
            } else if (target.blockState().getBlock() instanceof TallItemMachineBlock
                    && target.blockState().get(TallItemMachineBlock.PART) == TallItemMachineBlock.Part.TOP) {
                entity = target.player().getEntityWorld().getBlockEntity(target.pos().down());
            }
        }

        if (entity instanceof MachineInfoProvider provider) {
            var text = provider.getCurrentState();
            if (text != null) {
                hoverDisplayBuilder.setComponent(MACHINE_STATE, text);
            }
        }

        if (entity instanceof DebugTextProvider provider) {
            var text = provider.getDebugText();
            if (text != null) {
                hoverDisplayBuilder.setComponent(DEBUG_DATA, text);
            }
        }

        if (entity instanceof FilledStateProvider provider) {
            var text = provider.getFilledStateText();
            if (text != null) {
                hoverDisplayBuilder.setComponent(FILLED, Text.empty().append(text).withColor(0xd6d6d6));
            }
        }

    }

    public static GuiElement getButton(RecipeType<?> type) {
        var category = PolydexCategory.of(type);
        return GuiTextures.POLYDEX_BUTTON.get()
                .setName(Text.translatable("text.polyfactory.recipes"))
                .setCallback((index, type1, action, gui) -> {
                    PolydexPageUtils.openCategoryUi(gui.getPlayer(), category, gui::open);
                    GuiUtils.playClickSound(gui.getPlayer());
                }).build();
    }

    public static List<PolydexIngredient<?>> createIngredients(CountedIngredient... input) {
        return createIngredients(List.of(input));
    }
    public static List<PolydexIngredient<?>> createIngredients(List<CountedIngredient> input) {
        var list = new ArrayList<PolydexIngredient<?>>(input.size());
        for (var x : input) {
            list.add(PolydexIngredient.of(x.ingredient().orElse(null), Math.max(x.count(), 1), 1));
        }
        return list;
    }

    public static List<PolydexIngredient<?>> createIngredients(List<CountedIngredient> input, List<FluidInputStack> fluids) {
        var list = new ArrayList<PolydexIngredient<?>>(input.size());
        for (var x : input) {
            list.add(PolydexIngredient.of(x.ingredient().orElse(null), Math.max(x.count(), 1), 1));
        }
        for (var x : fluids) {
            list.add(new PolydexFluidStack(x.instance(), x.required(), 1));
        }
        return list;
    }

    public static List<PolydexIngredient<?>> createIngredientsReg(List<CountedIngredient> input, List<FluidStack<?>> fluids) {
        var list = new ArrayList<PolydexIngredient<?>>(input.size());
        for (var x : input) {
            list.add(PolydexIngredient.of(x.ingredient().orElse(null), Math.max(x.count(), 1), 1));
        }
        for (var x : fluids) {
            list.add(new PolydexFluidStack(x.instance(), x.amount(), 1));
        }
        return list;
    }

    public static List<PolydexStack<FluidInstance<?>>> createFluids(List<FluidStack<?>> fluids) {
        var list = new ArrayList<PolydexStack<FluidInstance<?>>>(fluids.size());
        for (var x : fluids) {
            list.add(new PolydexFluidStack(x.instance(), x.amount(), 1));
        }
        return list;
    }

    public static PolydexStack<?>[] createOutput(List<OutputStack> output) {
        var list = new ArrayList<PolydexStack<?>>(output.size());
        for (var x : output) {
            list.add(PolydexStack.of(x.stack().copyWithCount(x.stack().getCount() * x.roll()), x.chance()));
        }
        return list.toArray(new PolydexStack[0]);
    }

    public static PolydexStack<?>[] createOutput(List<OutputStack> output, List<FluidStack<?>> fluids) {
        var list = new ArrayList<PolydexStack<?>>(output.size());
        for (var x : output) {
            list.add(PolydexStack.of(x.stack().copyWithCount(x.stack().getCount() * x.roll()), x.chance()));
        }
        for (var x : fluids) {
            list.add(new PolydexFluidStack(x.instance(), x.amount(), 1));
        }
        return list.toArray(new PolydexStack[0]);
    }
}
