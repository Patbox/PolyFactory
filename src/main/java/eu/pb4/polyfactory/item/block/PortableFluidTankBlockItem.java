package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.CustomModelDataStringProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;

public class PortableFluidTankBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PortableFluidTankBlockItem(T block, Properties settings) {
        super(block, settings);
        DispenserBlock.registerBehavior(this, new ShulkerBoxDispenseBehavior());
    }

    public static void createItemAsset(ResourcePackBuilder builder) {
        var id = BuiltInRegistries.ITEM.getKey(FactoryItems.PORTABLE_FLUID_TANK);
        var list = new ArrayList<SelectItemModel.Case<String>>();

        for (var fluidType : FactoryRegistries.FLUID_TYPES.keySet()) {
            ItemModel model;
            var modelId = FactoryModels.FLUID_PORTABLE_FLUID_TANK_ITEM.getModelId(fluidType);
            if (FactoryRegistries.FLUID_TYPES.getValue(fluidType).color().isPresent()) {
                model = new BasicItemModel(modelId, List.of(new CustomModelDataTintSource(0, -1)));
            } else {
                model = new BasicItemModel(modelId);
            }

            list.add(new SelectItemModel.Case<>(List.of(fluidType.toString()), model));
        }

        builder.addData(AssetPaths.itemAsset(id), new ItemAsset(new CompositeItemModel(List.of(new BasicItemModel(id.withPrefix("block/")),
                new SelectItemModel<>(
                        new SelectItemModel.Switch<>(new CustomModelDataStringProperty(0), list),
                        Optional.of(EmptyItemModel.INSTANCE)
                )
        )), ItemAsset.Properties.DEFAULT).toJson().getBytes(StandardCharsets.UTF_8));
    }

    private FluidInstance<?> getMainFluid(ItemStack itemStack) {
        var fluids = itemStack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT).fluids();
        return fluids.isEmpty() ? null : fluids.getFirst();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().getItemInHand(context.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).is(FactoryItems.PRESSURE_FLUID_GUN)) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }


    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        FluidContainerUtil.tick(FluidContainerFromComponent.of(stack), (ServerLevel) world, entity.position().add(0, entity.getY() / 2, 0), 0,
                FactoryUtil.getItemConsumer(entity));
    }

    @Override
    public Component getName(ItemStack stack) {
        var container = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
        if (container.isEmpty()) {
            return Component.translatable(this.getDescriptionId() + ".empty");
        } else if (container.fluids().size() == 1) {
            return Component.translatable(this.getDescriptionId() + ".typed", container.fluids().getFirst().getName());
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var base = super.getPolymerItemStack(itemStack, tooltipType, context);
        if (itemStack.has(FactoryDataComponents.FLUID)) {
            var fluids = itemStack.get(FactoryDataComponents.FLUID);
            if (fluids != null && fluids.capacity() != -1) {
                base.set(DataComponents.MAX_DAMAGE, (int) (fluids.capacity() / 100));
                base.set(DataComponents.DAMAGE, (int) ((fluids.capacity() - fluids.stored()) / 100));
            }

            var x = (FluidInstance<Object>) getMainFluid(itemStack);
            if (x != null && x.type().color().isPresent()) {
                base.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(),
                        List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of(x.type().color().get().getColor(x.data()))));
            } else if (x != null) {
                base.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(),
                        List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of()));
            } else {
                base.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(),
                        List.of(), IntList.of()));
            }
        }
        return base;
    }
}
