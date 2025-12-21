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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class CastingRecipePage<T extends CastingRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.CASTING_TABLE.getDefaultInstance();
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?> output;

    public CastingRecipePage(RecipeHolder<T> recipe) {
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
    public @Nullable Component texture(ServerPlayer player) {
        return Component.empty()
                .append(PolydexTextures.SPOUT)
                .append(Component.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.SPOUT_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    var x = getBaseFluid();
                    a.accept(x.instance(), (float) (x.amount() / (double) FluidConstants.BLOCK));
                }))
                .append(Component.literal("" + GuiTextures.SPOUT_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }
    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.isPartOf(this.output);
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        return this.output.toTypeDisplayItemStack(player);
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return ICON;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder layer) {
        layer.setIngredient(3, 2, this.ingredients.getFirst());
        layer.setOutput(6, 2, this.output);

        var fluid = GuiTextures.EMPTY_BUILDER.get();
        fluid.setName(Component.translatable("text.polyfactory.polydex.required_fluids"));
        var stack = getBaseFluid();
        fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));

        layer.set(2, 1, fluid);
        layer.set(2, 2, fluid);
        layer.set(2, 3, fluid);
    }
}
