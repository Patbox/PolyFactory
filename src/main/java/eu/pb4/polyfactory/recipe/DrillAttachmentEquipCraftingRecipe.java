package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.tool.BaseDrillItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class DrillAttachmentEquipCraftingRecipe extends CustomRecipe {
    public static final MapCodec<DrillAttachmentEquipCraftingRecipe> CODEC = MapCodec.unit(DrillAttachmentEquipCraftingRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack drill = null;
        ItemStack drillHead = null;

        for (var item : input.items()) {
            if (item.getItem() instanceof BaseDrillItem && drill == null) {
                drill = item;
            } else if (item.is(FactoryItemTags.DRILL_HEADS) && drillHead == null) {
                drillHead = item;
            } else if (!item.isEmpty()) {
                return false;
            }
        }

        return drill != null && (drill.has(FactoryDataComponents.DRILL_ATTACHMENT) || drillHead != null);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack drill = ItemStack.EMPTY;
        ItemStack drillHead = null;

        for (var item : input.items()) {
            if (item.getItem() instanceof BaseDrillItem) {
                drill = item.copy();
            } else if (item.is(FactoryItemTags.DRILL_HEADS)) {
                drillHead = item;
            }
        }

        if (drillHead != null) {
            BaseDrillItem.putItemIn(drill, drillHead);
        } else {
            BaseDrillItem.takeItemFrom(drill);
        }

        return drill;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < result.size(); ++slot) {
            var item = input.getItem(slot);
            if (item.getItem() instanceof BaseDrillItem && item.has(FactoryDataComponents.DRILL_ATTACHMENT)) {
                result.set(slot, BaseDrillItem.takeItemFrom(item.copy()));
                return result;
            }
        }

        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_DRILL_ATTACHMENT_EQUIP;
    }
}
