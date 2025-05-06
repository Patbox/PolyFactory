package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.block.fluids.SmelteryBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.fluid.FluidModel;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.smeltery.SmelteryRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SmelteryRecipePage<T extends SmelteryRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.SMELTERY.getDefaultStack();
    private final PolydexIngredient<?> ingredient;
    private final PolydexStack<?>[] outputFluids;

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return Text.empty()
                .append(PolydexTextures.SMELTERY)
                .append(Text.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.SMELTERY_POLYDEX_FLUID_OFFSET).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.SMELTERY.render((a) -> {
                    for (var x : getFluidOutput()) {
                        //a.accept(x.instance(), (float) (x.amount() / (double) SmelteryBlockEntity.FLUID_CAPACITY));
                        a.accept(x.instance(), (float) (x.amount() / (double) (FluidConstants.BLOCK * 8)));
                    }
                }))
                .append(Text.literal("" + GuiTextures.SMELTERY_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return ItemStack.EMPTY;
    }

    protected abstract Ingredient getInput();
    protected abstract List<FluidStack<?>> getFluidOutput();

    public SmelteryRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
        this.ingredient = PolydexIngredient.of(getInput());
        this.outputFluids = PolydexCompatImpl.createFluids(getFluidOutput()).toArray(new PolydexStack[0]);
    }
    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        var b = GuiElementBuilder.from(this.outputFluids[0].toTypeDisplayItemStack(player));
        for (var x : this.ingredient.asStacks()) {
            b.addLoreLine(Text.literal("- ").append(x.getName()).formatted(Formatting.GRAY));
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
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(2, 2, this.ingredient);

        var fluid = GuiTextures.EMPTY_BUILDER.get();
        fluid.setName(Text.translatable("text.polyfactory.polydex.created_fluids"));
        for (var stack : getFluidOutput()) {
            fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
        }

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 3; x++) {
                layer.set(x + 5, y, fluid);

            }
        }
    }
}
