package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.fluid.DrainRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class DrainRecipePage<T extends DrainRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.DRAIN.getDefaultStack();
    private final List<PolydexIngredient<?>> ingredients;
    private final List<PolydexStack<?>> output = new ArrayList<>();
    private final PolydexStack<ItemStack> displayResult;

    public DrainRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredientsReg(getBaseIngredient(), getBaseFluids());
        this.displayResult = PolydexStack.of(getResultStack());
        this.output.add(this.displayResult);
        this.output.addAll(PolydexCompatImpl.createFluids(getResultFluids()));
    }

    protected abstract ItemStack getResultStack();
    protected abstract List<FluidStack<?>> getBaseFluids();
    protected abstract List<FluidStack<?>> getResultFluids();
    protected abstract List<CountedIngredient> getBaseIngredient();
    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return Text.empty()
                .append(PolydexTextures.DRAIN)
                .append(Text.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.DRAIN_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    for (var x : getResultFluids()) {
                        a.accept(x.instance(), (float) (x.amount() / (double) FluidConstants.BLOCK));
                    }
                }))
                .append(Text.literal("" + GuiTextures.DRAIN_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
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
        return this.ingredients.getFirst().asStacks().getFirst().toTypeDisplayItemStack(player);
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
        layer.setIngredient(2, 1, this.ingredients.get(0));
        if (this.ingredients.size() > 1) {
            layer.setIngredient(2, 3, this.ingredients.get(1));
        }
        layer.setOutput(5, 2, this.displayResult);

        var fluid = GuiTextures.EMPTY_BUILDER.get();
        fluid.setName(Text.translatable("text.polyfactory.polydex.created_fluids"));
        for (var stack : getResultFluids()) {
            fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        }

        layer.set(6, 1, fluid);
        layer.set(6, 2, fluid);
        layer.set(6, 3, fluid);
    }
}
