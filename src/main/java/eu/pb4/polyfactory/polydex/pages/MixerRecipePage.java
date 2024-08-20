package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.MixingRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class MixerRecipePage<T extends MixingRecipe> extends PrioritizedRecipePage<T> {
    private static final Text REQUIRED_HEAT = Text.translatable("text.polyfactory.required_heat").styled(x -> x.withItalic(false));
    private static final ItemStack ICON = FactoryItems.MIXER.getDefaultStack();
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?>[] outputFluids;
    private final List<PolydexIngredient<?>> ingredientsVisual;

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return Text.empty()
                .append(PolydexTextures.MIXER)
                .append(Text.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_1).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    for (var x : getFluidInput()) {
                        a.accept(x.instance(), (float) (x.required() / (double) MixerBlockEntity.FLUID_CAPACITY));
                    }
                }))
                .append(Text.literal("" + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_2).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    for (var x : getFluidOutput()) {
                        a.accept(x.instance(), (float) (x.amount() / (double) MixerBlockEntity.FLUID_CAPACITY));
                    }
                }))
                .append(Text.literal("" + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }

    protected abstract List<FluidInputStack> getFluidInput();
    protected abstract List<CountedIngredient> getItemInput();
    protected abstract List<FluidStack<?>> getFluidOutput();
    protected abstract ItemStack getItemOutput();
    protected abstract float getMaxTemperature();
    protected abstract float getMinimumTemperature();

    public MixerRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredients(getItemInput(), getFluidInput());
        this.ingredientsVisual = PolydexCompatImpl.createIngredients(getItemInput());
        this.outputFluids = PolydexCompatImpl.createFluids(getFluidOutput()).toArray(new PolydexStack[0]);
    }
    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        var x = super.entryIcon(entry, player);
        if (x.isEmpty() && this.outputFluids.length != 0) {
            return this.outputFluids[0].toTypeDisplayItemStack(player);
        }

        return x;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        for (var x : this.outputFluids) {
            if (entry.isPartOf(x)) {
                return true;
            }
        }

        return super.isOwner(server, entry);
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        for (int i = 0; i < 6; i++) {
            if (i < this.ingredientsVisual.size()) {
                layer.setIngredient(2 + i % 2, 1 + i / 2, this.ingredientsVisual.get(i));
            } else {
                layer.setEmpty(2 + i % 2, 1 + i / 2);
            }
        }
        if (!getFluidInput().isEmpty()) {
            var fluid = GuiTextures.EMPTY_BUILDER.get();
            fluid.setName(Text.translatable("text.polyfactory.polydex.required_fluids"));
            for (var stack : getFluidInput()) {
                fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
            }

            layer.set(1, 1, fluid);
            layer.set(1, 2, fluid);
            layer.set(1, 3, fluid);
        }

        if (!getFluidOutput().isEmpty()) {
            var fluid = GuiTextures.EMPTY_BUILDER.get();
            fluid.setName(Text.translatable("text.polyfactory.polydex.created_fluids"));
            for (var stack : getFluidOutput()) {
                fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
            }

            layer.set(7, 1, fluid);
            layer.set(7, 2, fluid);
            layer.set(7, 3, fluid);
        }

        layer.setOutput(6, 2, getItemOutput());
        layer.set(5, 3, GuiElementBuilder.from(GuiTextures.EMPTY.getItemStack()).setName(REQUIRED_HEAT).asStack());
        layer.set(4, 3,
                GuiTextures.TEMPERATURE_OFFSET_RIGHT.getNamed(MathHelper.clamp(getMinimumTemperature(), -1, 1), REQUIRED_HEAT),
                GuiTextures.TEMPERATURE_OFFSET_RIGHT.getNamed(MathHelper.clamp(getMaxTemperature(), -1, 1),  REQUIRED_HEAT));

    }
}
