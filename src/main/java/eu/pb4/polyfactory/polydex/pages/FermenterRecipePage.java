package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.fermenting.FermentingRecipe;
import eu.pb4.polyfactory.ui.fluid.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class FermenterRecipePage<T extends FermentingRecipe> extends PrioritizedRecipePage<T> {
    private static final Component REQUIRED_HEAT = Component.translatable("text.polyfactory.required_heat").withStyle(x -> x.withItalic(false));
    private final ItemStack ICON = FactoryItems.FERMENTER.getDefaultInstance();
    private final PolydexIngredient<?> ingredient;
    private final PolydexStack<?>[] outputFluids;
    private final float fluidVisualMult;

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return Component.empty()
                .append(PolydexTextures.FERMENTER)
                .append(Component.literal("" + GuiTextures.POLYDEX_OFFSET_N + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_1 + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_2).setStyle(UiResourceCreator.STYLE))
                .append(FluidTextures.MIXER_POLYDEX.render((a) -> {
                    for (var x : getFluidOutput()) {
                        a.accept(x.instance(), (float) (x.amount() / (double) MixerBlockEntity.FLUID_CAPACITY) * this.fluidVisualMult);
                    }
                }))
                .append(Component.literal("" + GuiTextures.MIXER_POLYDEX_FLUID_OFFSET_N + GuiTextures.POLYDEX_OFFSET).setStyle(UiResourceCreator.STYLE));
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        var out = getItemOutput();
        return out.isEmpty() ? ItemStack.EMPTY : out.getFirst().getBacking().copy();
    }

    protected abstract Ingredient getItemInput();

    protected abstract List<FluidStack<?>> getFluidOutput();

    protected abstract List<PolydexStack<ItemStack>> getItemOutput();

    protected abstract float getMaxTemperature();

    protected abstract float getMinimumTemperature();

    public FermenterRecipePage(RecipeHolder<T> recipe) {
        super(recipe);
        this.ingredient = PolydexIngredient.of(getItemInput());
        this.outputFluids = PolydexCompatImpl.createFluids(getFluidOutput()).toArray(new PolydexStack[0]);

        var fluidsTotalA = 0;
        var fluidsTotalB = 0;

        for (var fluid : getFluidOutput()) {
            fluidsTotalA += (int) fluid.amount();
        }

        var fluidsTotal = Math.max(fluidsTotalA, fluidsTotalB);

        if (fluidsTotal < FluidConstants.BLOCK / 2) {
            this.fluidVisualMult = (float) (FluidConstants.BLOCK / 2) / fluidsTotal;
        } else {
            this.fluidVisualMult = 1;
        }
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayer player) {
        if (this.outputFluids.length != 0) {
            return this.outputFluids[0].toTypeDisplayItemStack(player);
        }
        return super.entryIcon(entry, player);
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of(this.ingredient);
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

        if (!getFluidOutput().isEmpty()) {
            var fluid = GuiTextures.EMPTY_BUILDER.get();
            fluid.setName(Component.translatable("text.polyfactory.polydex.created_fluids"));
            for (var stack : getFluidOutput()) {
                fluid.addLoreLine(stack.toTextRequired().setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
            }

            layer.set(7, 1, fluid);
            layer.set(7, 2, fluid);
            layer.set(7, 3, fluid);
        }

        var out = this.getItemOutput();
        for (var i = 0; i < Math.min(out.size(), 6); i++) {
            layer.setOutput(6 + i % 2, 2 + i / 2, out.get(i));
        }

        //layer.set(8, 2, PolydexCompatImpl.requiredRotation(this.getMinimumSpeed(), this.getOptimalSpeed(), FermenterBlockEntity::getActiveStress));
        layer.set(2, 3,
                GuiTextures.TEMPERATURE.getNamed(Mth.clamp(getMinimumTemperature(), -1, 1), REQUIRED_HEAT),
                GuiTextures.TEMPERATURE.getNamed(Mth.clamp(getMaxTemperature(), -1, 1), REQUIRED_HEAT));

    }
}
