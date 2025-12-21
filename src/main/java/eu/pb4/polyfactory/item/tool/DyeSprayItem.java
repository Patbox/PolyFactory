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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class DyeSprayItem extends Item implements PolymerItem, ColoredItem {
    public static final int MAX_USES = 128;

    public DyeSprayItem(Properties settings) {
        super(settings);
    }

    public static int getUses(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.USES_LEFT, 0);
    }

    public static void setUses(ItemStack stack, int count) {
        stack.set(FactoryDataComponents.USES_LEFT, count);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var stack = context.getItemInHand();
        var color = ColoredItem.getColor(stack);
        var uses = getUses(stack);

        if (color == -1 || uses == 0) {
            return InteractionResult.FAIL;
        }

        var state = context.getLevel().getBlockState(context.getClickedPos());
        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        boolean success = false;

        if (state.getBlock() instanceof AbstractCableBlock cableBlock && cableBlock.hasCable(state)) {
            success = cableBlock.setColor(state, context.getLevel(), context.getClickedPos(), color);
        } else if (state.getBlock() instanceof LampBlock lampBlock) {
            success = lampBlock.setColor(context.getLevel(), context.getClickedPos(), color);
        } else if (state.getBlock() instanceof SidedLampBlock lampBlock) {
            success = lampBlock.setColor(context.getLevel(), context.getClickedPos(), color);
        } else if (state.getBlock() instanceof WindmillBlock && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WindmillBlockEntity be) {
            var stacks = be.getSails();
            for (int i = 0; i < stacks.size(); i++) {
                var x = stacks.get(i);
                if (!x.isEmpty() && be.getSailColor(i) != color) {
                    x.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
                    be.addSail(i, x);
                    success = true;
                    break;
                }
            }
        } else if (state.getBlock() instanceof NixieTubeBlock && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof NixieTubeBlockEntity be) {
            success = be.setColor(color);
            if (success) {
                be.updateTextDisplay();
            }
        } else if (state.is(FactoryBlockTags.SPRAY_CAN_COLORABLE) && DyeColorExtra.BY_COLOR.get(color) != null
                && !(blockEntity instanceof Container)) {
            var dye = DyeColorExtra.BY_COLOR.get(color);
            var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            Identifier newId;
            if (id.getPath().equals("glass") || id.getPath().equals("glass_pane")) {
                newId = id.withPrefix(dye.getSerializedName() + "_stained_");
            } else {
                var x = id.getPath().split("_", 2);
                newId = DyeColor.byName(x[0], null) == null ? id.withPath(dye.getSerializedName() + "_" + id.getPath()) : id.withPath(dye.getSerializedName() + "_" + x[1]);
            }

            var block = BuiltInRegistries.BLOCK.getValue(newId);
            if (block != Blocks.AIR && block != state.getBlock()) {
                context.getLevel().setBlock(context.getClickedPos(), block.withPropertiesOf(state), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_ALL_IMMEDIATE);

                if (state.getBlock() instanceof BedBlock) {
                    var dir = state.getValue(BedBlock.FACING);
                    var offset = context.getClickedPos().relative(dir, state.getValue(BedBlock.PART) == BedPart.HEAD ? -1 : 1);
                    context.getLevel().setBlock(offset, block.withPropertiesOf(context.getLevel().getBlockState(offset)), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_ALL_IMMEDIATE);
                }
                success = true;
            }

        }

        if (success) {
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                setUses(stack, uses - 1);
            }

            ((ServerLevel) context.getLevel()).sendParticles(new DustParticleOptions(color, 1), context.getClickLocation().x, context.getClickLocation().y, context.getClickLocation().z, 10, 0.2f, 0.2f, 0.2f, 1);

            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getUses(stack) == 0) {
            return Component.translatable(getDescriptionId() + ".empty");
        }
        var color = ColoredItem.getColor(stack);
        if (!DyeColorExtra.hasLang(color)) {
            return Component.translatable(this.getDescriptionId() + ".colored.full",
                    ColoredItem.getColorName(color), ColoredItem.getHexName(color));
        } else {
            return Component.translatable(this.getDescriptionId() + ".colored", ColoredItem.getColorName(color));
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        var color = ColoredItem.getColor(stack);
        var uses = getUses(stack);

        if (otherStack.is(ConventionalItemTags.DYES) && uses + 8 <= MAX_USES) {
            var dyeColor = DyeColorExtra.getColor(otherStack);

            if (dyeColor == color || uses == 0) {
                ColoredItem.setColor(stack, dyeColor);
                setUses(stack, uses + 8);
                otherStack.shrink(1);
            }
        } else if (otherStack.is(Items.WATER_BUCKET)) {
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
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        var uses = getUses(itemStack);

        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(FloatList.of((float) uses / MAX_USES),
                BooleanList.of(uses != 0), List.of(), IntList.of(ColoredItem.getColor(itemStack))));
        out.set(DataComponents.MAX_DAMAGE, MAX_USES);
        if (uses != 0) {
            out.set(DataComponents.DAMAGE, MAX_USES - uses);
        }
        return out;
    }
}
