package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlock;
import eu.pb4.polyfactory.block.data.output.NixieTubeBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlockEntity;
import eu.pb4.polyfactory.block.other.LampBlock;
import eu.pb4.polyfactory.block.other.SidedLampBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class DyeSprayItem extends Item implements PolymerItem, ColoredItem {
    public static final int MAX_USES = 128;

    public DyeSprayItem(Settings settings) {
        super(settings);
    }

    public static int getUses(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.USES_LEFT, 0);
    }

    public static void setUses(ItemStack stack, int count) {
        stack.set(FactoryDataComponents.USES_LEFT, count);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var stack = context.getStack();
        var color = ColoredItem.getColor(stack);
        var uses = getUses(stack);

        if (color == -1 || uses == 0) {
            return ActionResult.FAIL;
        }

        var state = context.getWorld().getBlockState(context.getBlockPos());
        var blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());
        boolean success = false;

        if (state.getBlock() instanceof AbstractCableBlock cableBlock && cableBlock.hasCable(state)) {
            success = cableBlock.setColor(state, context.getWorld(), context.getBlockPos(), color);
        } else if (state.getBlock() instanceof LampBlock lampBlock) {
            success = lampBlock.setColor(context.getWorld(), context.getBlockPos(), color);
        } else if (state.getBlock() instanceof SidedLampBlock lampBlock) {
            success = lampBlock.setColor(context.getWorld(), context.getBlockPos(), color);
        } else if (state.getBlock() instanceof WindmillBlock && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof WindmillBlockEntity be) {
            var stacks = be.getSails();
            for (int i = 0; i < stacks.size(); i++) {
                var x = stacks.get(i);
                if (!x.isEmpty() && be.getSailColor(i) != color) {
                    x.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, true));
                    be.addSail(i, x);
                    success = true;
                    break;
                }
            }
        } else if (state.getBlock() instanceof NixieTubeBlock && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof NixieTubeBlockEntity be) {
            success = be.setColor(color);
            if (success) {
                be.updateTextDisplay();
            }
        } else if (state.isIn(FactoryBlockTags.SPRAY_CAN_COLORABLE) && DyeColorExtra.BY_COLOR.get(color) != null
                && !(blockEntity instanceof Inventory)) {
            var dye = DyeColorExtra.BY_COLOR.get(color);
            var id = Registries.BLOCK.getId(state.getBlock());
            Identifier newId;
            if (id.getPath().equals("glass") || id.getPath().equals("glass_pane")) {
                newId = id.withPrefixedPath(dye.asString() + "_stained_");
            } else {
                var x = id.getPath().split("_", 2);
                newId = DyeColor.byName(x[0], null) == null ? id.withPath(dye.asString() + "_" + id.getPath()) : id.withPath(dye.asString() + "_" + x[1]);
            }

            var block = Registries.BLOCK.get(newId);
            if (block != Blocks.AIR && block != state.getBlock()) {
                context.getWorld().setBlockState(context.getBlockPos(), block.getStateWithProperties(state), Block.FORCE_STATE | Block.NOTIFY_ALL_AND_REDRAW);

                if (state.getBlock() instanceof BedBlock) {
                    var dir = state.get(BedBlock.FACING);
                    var offset = context.getBlockPos().offset(dir, state.get(BedBlock.PART) == BedPart.HEAD ? -1 : 1);
                    context.getWorld().setBlockState(offset, block.getStateWithProperties(context.getWorld().getBlockState(offset)), Block.FORCE_STATE | Block.NOTIFY_ALL_AND_REDRAW);
                }
                success = true;
            }

        }

        if (success) {
            context.getWorld().playSound(null, context.getBlockPos(), SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                setUses(stack, uses - 1);
            }

            ((ServerWorld) context.getWorld()).spawnParticles(new DustParticleEffect(color, 1), context.getHitPos().x, context.getHitPos().y, context.getHitPos().z, 10, 0.2f, 0.2f, 0.2f, 1);

            return ActionResult.SUCCESS_SERVER;
        }
        return ActionResult.FAIL;
    }

    @Override
    public Text getName(ItemStack stack) {
        if (getUses(stack) == 0) {
            return Text.translatable(getTranslationKey() + ".empty");
        }
        var color = ColoredItem.getColor(stack);
        if (!DyeColorExtra.hasLang(color)) {
            return Text.translatable(this.getTranslationKey() + ".colored.full",
                    ColoredItem.getColorName(color), ColoredItem.getHexName(color));
        } else {
            return Text.translatable(this.getTranslationKey() + ".colored", ColoredItem.getColorName(color));
        }
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        var color = ColoredItem.getColor(stack);
        var uses = getUses(stack);

        if (otherStack.isIn(ConventionalItemTags.DYES) && uses + 8 <= MAX_USES) {
            var dyeColor = DyeColorExtra.getColor(otherStack);

            if (dyeColor == color || uses == 0) {
                ColoredItem.setColor(stack, dyeColor);
                setUses(stack, uses + 8);
                otherStack.decrement(1);
            }
        } else if (otherStack.isOf(Items.WATER_BUCKET)) {
            setUses(stack, 0);
        } else {
            return false;
        }

        return true;
    }

    @Override
    public int downSampleColor(int color, boolean isVanilla) {
        return color;
    }

    @Override
    public int getDefaultColor() {
        return -1;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        var uses = getUses(itemStack);

        out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(FloatList.of((float) uses / MAX_USES),
                BooleanList.of(uses != 0), List.of(), IntList.of(ColoredItem.getColor(itemStack))));
        out.set(DataComponentTypes.MAX_DAMAGE, MAX_USES);
        if (uses != 0) {
            out.set(DataComponentTypes.DAMAGE, MAX_USES - uses);
        }
        return out;
    }
}
