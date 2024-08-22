package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PortableFluidTankBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PortableFluidTankBlockItem(T block, Settings settings) {
        super(block, settings);
        DispenserBlock.registerBehavior(this, new BlockPlacementDispenserBehavior());
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var x = getMainFluid(itemStack);
        if (x != null) {
            return FactoryModels.ITEM_PORTABLE_FLUID_TANK.getRaw(x).getItem();
        }

        return super.getPolymerItem(itemStack, player);
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var x = getMainFluid(itemStack);
        if (x != null) {
            //noinspection DataFlowIssue
            return FactoryModels.ITEM_PORTABLE_FLUID_TANK.getRaw(x).get(DataComponentTypes.CUSTOM_MODEL_DATA).value();
        }

        return super.getPolymerCustomModelData(itemStack, player);
    }

    private FluidInstance<?> getMainFluid(ItemStack itemStack) {
        var fluids = itemStack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT).fluids();
        return fluids.isEmpty() ? null : fluids.getFirst();
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getPlayer().getStackInHand(context.getHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND).isOf(FactoryItems.PRESSURE_FLUID_GUN)) {
            return ActionResult.PASS;
        }
        return super.useOnBlock(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        FluidContainerUtil.tick(FluidContainerFromComponent.of(stack), (ServerWorld) world, entity.getPos().add(0, entity.getY() / 2, 0), 0,
                FactoryUtil.getItemConsumer(entity));
    }

    @Override
    public Text getName(ItemStack stack) {
        var container = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
        if (container.isEmpty()) {
            return Text.translatable(this.getTranslationKey() + ".empty");
        } else if (container.fluids().size() == 1) {
            return Text.translatable(this.getTranslationKey() + ".typed", container.fluids().getFirst().getName());
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var base = super.getPolymerItemStack(itemStack, tooltipType, lookup, player);
        if (itemStack.contains(FactoryDataComponents.FLUID)) {
            var fluids = itemStack.get(FactoryDataComponents.FLUID);
            if (fluids != null && fluids.capacity() != -1) {
                base.set(DataComponentTypes.MAX_DAMAGE, (int) (fluids.capacity() / 100));
                base.set(DataComponentTypes.DAMAGE, (int) ((fluids.capacity() - fluids.stored()) / 100));
            }

            var x = (FluidInstance<Object>) getMainFluid(itemStack);
            if (x != null && x.type().color().isPresent()) {
                base.set(DataComponentTypes.FIREWORK_EXPLOSION, new FireworkExplosionComponent(FireworkExplosionComponent.Type.BURST,
                        IntList.of(x.type().color().get().getColor(x.data())), IntList.of(), false, false));
            }
        }
        return base;
    }
}
