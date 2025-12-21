package eu.pb4.polyfactory.loottable;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.fluids.FluidContainerOwner;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class CopyFluidsLootFunction implements LootItemFunction {
    public static final LootItemFunction INSTANCE = new CopyFluidsLootFunction();
    public static final LootItemFunctionType TYPE = new LootItemFunctionType(MapCodec.unit(INSTANCE));

    @Override
    public LootItemFunctionType getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        if (lootContext.hasParameter(LootContextParams.BLOCK_ENTITY)) {
            if (lootContext.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FluidContainerOwner container) {
                var main = container.getMainFluidContainer();
                if (main != null){
                    stack.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(main));
                }
            }
        }

        return stack;
    }
}
