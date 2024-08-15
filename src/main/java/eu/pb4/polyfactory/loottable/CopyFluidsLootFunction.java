package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.fluids.FluidContainerOwner;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class CopyFluidsLootFunction implements LootFunction {
    public static final LootFunction INSTANCE = new CopyFluidsLootFunction();
    public static final LootFunctionType TYPE = new LootFunctionType(MapCodec.unit(INSTANCE));

    @Override
    public LootFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParameters.BLOCK_ENTITY)) {
            if (lootContext.get(LootContextParameters.BLOCK_ENTITY) instanceof FluidContainerOwner container) {
                var main = container.getMainFluidContainer();
                if (main != null){
                    stack.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(main));
                }
            }
        }

        return stack;
    }
}
