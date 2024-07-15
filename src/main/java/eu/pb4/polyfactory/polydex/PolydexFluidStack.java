package eu.pb4.polyfactory.polydex;

import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PolydexFluidStack implements PolydexStack<FluidInstance<?>> {
    private final FluidInstance<?> fluid;
    private final float chance;
    private final long amount;

    public PolydexFluidStack(FluidInstance<?> fluid, long amount, float chance) {
        this.fluid = fluid;
        this.amount = amount;
        this.chance = chance;
    }

    public PolydexFluidStack(FluidStack<?> fluid) {
        this(fluid.instance(), fluid.amount(), 1);
    }

    public float chance() {
        return this.chance;
    }

    public long amount() {
        return this.amount;
    }

    @Override
    public boolean matchesDirect(PolydexStack<FluidInstance<?>> polydexStack, boolean strict) {
        return this.fluid.equals(polydexStack.getBacking());
    }

    public boolean isEmpty() {
        return this.amount != 0;
    }

    public Text getName() {
        return this.fluid.getName();
    }

    public Class<FluidInstance<?>> getBackingClass() {
        //noinspection unchecked
        return (Class<FluidInstance<?>>) this.fluid.getClass();
    }

    public ItemStack toItemStack(ServerPlayerEntity player) {
        return GuiElementBuilder.from(FactoryModels.FLAT_FULL.get(this.fluid)).hideDefaultTooltip().setName(this.fluid.getName()).asStack();
    }

    @Override
    public ItemStack toDisplayItemStack(ServerPlayerEntity player) {
        if (this.amount == 0) {
            return toItemStack(player);
        }
        return GuiElementBuilder.from(FactoryModels.FLAT_FULL.get(this.fluid)).hideDefaultTooltip().setName(this.fluid.toLabeledAmount(this.amount)).asStack();
    }

    public FluidInstance<?> getBacking() {
        return this.fluid;
    }

    @Override
    public int getSourceHashCode() {
        return this.fluid.hashCode();
    }
}
