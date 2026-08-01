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
import eu.pb4.polyfactory.ui.GuiModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.CustomModelDataFlagProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.numeric.CustomModelDataFloatProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.CustomModelDataStringProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        builder.addData(AssetPaths.itemAsset(id), new ItemAsset(new CompositeItemModel(
                List.of(new BasicItemModel(id.withPrefix("block/")),
                        new SelectItemModel<>(
                                new SelectItemModel.Switch<>(new CustomModelDataStringProperty(0), list),
                                Optional.of(EmptyItemModel.INSTANCE),
                                Optional.empty()
                        )/*,
                        new ConditionItemModel(new CustomModelDataFlagProperty(0),
                                GuiModels.createGuiOnly(
                                        GuiModels.createBottomGenericBar(new Matrix4f().translate(0, 0, 10), new CustomModelDataFloatProperty(0), new CustomModelDataTintSource(1, -1))
                                ),
                                EmptyItemModel.INSTANCE
                        )*/
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
        FluidContainerUtil.tick(FluidContainerFromComponent.of(stack), world, entity.position().add(0, entity.getBbHeight() / 2, 0), 0,
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
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        super.modifyBasePolymerItemStack(out, stack, context, lookup);

        if (stack.has(FactoryDataComponents.FLUID)) {
            var fluids = stack.get(FactoryDataComponents.FLUID);

            float progress = 0;
            int progressColor = -1;//ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);

            if (fluids != null && fluids.capacity() != -1 && fluids.capacity() < Integer.MAX_VALUE && !fluids.isEmpty()) {
                out.set(DataComponents.MAX_DAMAGE, (int) (fluids.capacity()));
                out.set(DataComponents.DAMAGE, (int) ((fluids.capacity() - fluids.stored() + 1)));
                //progress = (float) fluids.stored() / fluids.capacity();
            }

            var x = (FluidInstance<Object>) getMainFluid(stack);
            if (x != null && x.type().color().isPresent()) {
                out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(progress), List.of(true),
                        List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of(x.type().color().get().getColor(x.data()), progressColor)));
            } else if (x != null) {
                out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(progress), List.of(true),
                        List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of(-1, progressColor)));
            } else {
                out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(false),
                        List.of(), IntList.of()));
            }
        }
    }
}
