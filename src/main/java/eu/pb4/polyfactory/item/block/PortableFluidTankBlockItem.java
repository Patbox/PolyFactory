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
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PortableFluidTankBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PortableFluidTankBlockItem(T block, Settings settings) {
        super(block, settings);
        DispenserBlock.registerBehavior(this, new BlockPlacementDispenserBehavior());
    }

    public static void createItemAsset(ResourcePackBuilder builder) {
        var id = Registries.ITEM.getId(FactoryItems.PORTABLE_FLUID_TANK);
        var list = new ArrayList<SelectItemModel.Case<String>>();

        for (var fluidType : FactoryRegistries.FLUID_TYPES.getIds()) {
            ItemModel model;
            var modelId = FactoryModels.FLUID_PORTABLE_FLUID_TANK_ITEM.getModelId(fluidType);
            if (FactoryRegistries.FLUID_TYPES.get(fluidType).color().isPresent()) {
                model = new BasicItemModel(modelId, List.of(new CustomModelDataTintSource(0, -1)));
            } else {
                model = new BasicItemModel(modelId);
            }

            list.add(new SelectItemModel.Case<>(List.of(fluidType.toString()), model));
        }

        builder.addData(AssetPaths.itemAsset(id), new ItemAsset(new CompositeItemModel(List.of(new BasicItemModel(id.withPrefixedPath("block/")),
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
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getPlayer().getStackInHand(context.getHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND).isOf(FactoryItems.PRESSURE_FLUID_GUN)) {
            return ActionResult.PASS;
        }
        return super.useOnBlock(context);
    }


    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
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
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var base = super.getPolymerItemStack(itemStack, tooltipType, context);
        if (itemStack.contains(FactoryDataComponents.FLUID)) {
            var fluids = itemStack.get(FactoryDataComponents.FLUID);
            if (fluids != null && fluids.capacity() != -1) {
                base.set(DataComponentTypes.MAX_DAMAGE, (int) (fluids.capacity() / 100));
                base.set(DataComponentTypes.DAMAGE, (int) ((fluids.capacity() - fluids.stored()) / 100));
            }

            var x = (FluidInstance<Object>) getMainFluid(itemStack);
            if (x != null && x.type().color().isPresent()) {
                base.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(),
                        List.of(FactoryRegistries.FLUID_TYPES.getId(x.type()).toString()), IntList.of(x.type().color().get().getColor(x.data()))));
            } else if (x != null) {
                base.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(),
                        List.of(FactoryRegistries.FLUID_TYPES.getId(x.type()).toString()), IntList.of()));
            } else {
                base.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(),
                        List.of(), IntList.of()));
            }
        }
        return base;
    }
}
