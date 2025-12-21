package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.smeltery.SmelteryRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SmelteryRecipePage<T extends SmelteryRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = new GuiElementBuilder(FactoryItems.SMELTERY).setName(Component.translatable("polydex_category.polyfactory.polyfactory.smeltery")).asStack();
    private final PolydexIngredient<?> ingredient;
    private final PolydexStack<?>[] outputFluids;

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return Component.empty()
                .append(PolydexTextures.SMELTERY)
                .append(Component.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.SMELTERY_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.SMELTERY.render((a) -> {
                    for (var x : getFluidOutput()) {
                        //a.accept(x.instance(), (float) (x.amount() / (double) IndustrialSmelteryBlockEntity.FLUID_CAPACITY));
                        a.accept(x.instance(), (float) (x.amount() / (double) (FluidConstants.BLOCK * 8)));
                    }
                }))
                .append(Component.literal("" + GuiTextures.SMELTERY_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return ItemStack.EMPTY;
    }

    protected abstract Ingredient getInput();
    protected abstract List<FluidStack<?>> getFluidOutput();

    public SmelteryRecipePage(RecipeHolder<T> recipe) {
        super(recipe);
        this.ingredient = PolydexIngredient.of(getInput());
        this.outputFluids = PolydexCompatImpl.createFluids(getFluidOutput()).toArray(new PolydexStack[0]);
    }
    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        var b = GuiElementBuilder.from(this.outputFluids[0].toTypeDisplayItemStack(player));
        for (var x : this.ingredient.asStacks()) {
            b.addLoreLine(Component.literal("- ").append(x.getName()).withStyle(ChatFormatting.GRAY));
        }
        return b.asStack();
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of(ingredient);
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
    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder layer) {
        layer.setIngredient(2, 2, this.ingredient);

        var fluid = GuiTextures.EMPTY_BUILDER.get();
        fluid.setName(Component.translatable("text.polyfactory.polydex.created_fluids"));
        for (var stack : getFluidOutput()) {
            fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
        }

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 3; x++) {
                layer.set(x + 5, y, fluid);

            }
        }
    }
}
