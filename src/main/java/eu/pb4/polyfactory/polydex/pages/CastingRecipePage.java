package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.casting.CastingRecipe;
import eu.pb4.polyfactory.recipe.spout.SpoutRecipe;
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

import java.util.List;

public abstract class CastingRecipePage<T extends CastingRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.CASTING_TABLE.getDefaultStack();
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?> output;

    public CastingRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredientsReg(List.of(getBaseIngredient()), List.of(getBaseFluid()));
        this.output = PolydexStack.of(getResultStack());
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return getResultStack();
    }

    protected abstract ItemStack getResultStack();
    protected abstract FluidStack<?> getBaseFluid();
    protected abstract CountedIngredient getBaseIngredient();
    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return Text.empty()
                .append(PolydexTextures.SPOUT)
                .append(Text.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.SPOUT_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    var x = getBaseFluid();
                    a.accept(x.instance(), (float) (x.amount() / (double) FluidConstants.BLOCK));
                }))
                .append(Text.literal("" + GuiTextures.SPOUT_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }
    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.isPartOf(this.output);
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.output.toTypeDisplayItemStack(player);
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
        layer.setIngredient(3, 2, this.ingredients.getFirst());
        layer.setOutput(6, 2, this.output);

        var fluid = GuiTextures.EMPTY_BUILDER.get();
        fluid.setName(Text.translatable("text.polyfactory.polydex.required_fluids"));
        var stack = getBaseFluid();
        fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));

        layer.set(2, 1, fluid);
        layer.set(2, 2, fluid);
        layer.set(2, 3, fluid);
    }
}
