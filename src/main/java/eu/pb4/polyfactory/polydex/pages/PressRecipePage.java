package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PressRecipePage extends PrioritizedRecipePage<GenericPressRecipe> {
    private static final ItemStack ICON = FactoryItems.PRESS.getDefaultStack();
    private static final ItemStack DRAIN_ICON = new GuiElementBuilder(FactoryItems.DRAIN).setName(Text.translatable("text.polyfactory.polydex.drain_required")).asStack();
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?>[] output;

    public PressRecipePage(RecipeEntry<GenericPressRecipe> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredients(this.recipe.inputA(), this.recipe.inputB());
        this.output = PolydexCompatImpl.createOutput(this.recipe.output(), this.recipe.outputFluids());
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return this.recipe.output().getFirst().stack().copy();
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return this.recipe.outputFluids().isEmpty() ? PolydexTextures.PRESS : Text.empty()
                .append(PolydexTextures.PRESS_FLUID)
                .append(Text.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.PRESS_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.PRESS_POLYDEX.render((a) -> {
                    for (var x : this.recipe.outputFluids()) {
                        a.accept(x.instance(), (float) (x.amount() / (double) DrainBlockEntity.CAPACITY));
                    }
                }))
                .append(Text.literal("" + GuiTextures.PRESS_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        for (var i : this.output) {
            if (entry.isPartOf(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.recipe.output().get(0).stack();
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(3, 1, this.ingredients.get(0));
        layer.setIngredient(5, 1, this.ingredients.get(1));
        layer.setOutput(4, 3, this.output);
        if (!this.recipe.outputFluids().isEmpty()) {
            layer.set(5, 2, DRAIN_ICON);

            var fluid = GuiTextures.EMPTY_BUILDER.get();
            fluid.setName(Text.translatable("text.polyfactory.polydex.required_fluids"));
            for (var stack : this.recipe.outputFluids()) {
                fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
            }

            layer.set(5, 3, fluid);
        }
    }
}
