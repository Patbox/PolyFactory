package eu.pb4.polyfactory.polydex;

import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.polydex.pages.GrindingRecipePage;
import eu.pb4.polyfactory.polydex.pages.MixerRecipePage;
import eu.pb4.polyfactory.polydex.pages.PressRecipePage;
import eu.pb4.polyfactory.recipe.CountedIngredient;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.OutputStack;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.StateNameProvider;
import eu.pb4.sgui.api.elements.GuiElement;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PolydexCompatImpl {
    public static void register() {
        PolydexPage.registerRecipeViewer(GenericPressRecipe.class, PressRecipePage::new);
        PolydexPage.registerRecipeViewer(GenericMixingRecipe.class, MixerRecipePage::new);
        PolydexPage.registerRecipeViewer(GrindingRecipe.class, GrindingRecipePage::new);

        HoverDisplayBuilder.register(PolydexCompatImpl::stateAccurateNames);
    }

    private static void stateAccurateNames(HoverDisplayBuilder hoverDisplayBuilder) {
        var target = hoverDisplayBuilder.getTarget();
        if (target.hasTarget() && target.entity() == null && target.blockState().getBlock() instanceof StateNameProvider provider) {
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, provider.getName(target.player().getServerWorld(), target.pos(), target.blockState(), target.blockEntity()));
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
            list.add(PolydexIngredient.of(x.ingredient(), Math.max(x.count(), 1)));
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
}
