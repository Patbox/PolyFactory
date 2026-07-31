package eu.pb4.polyfactory.item.tool;

import com.mojang.math.Transformation;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.CustomModelDataStringProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public class CanisterItem extends SimplePolymerItem {

    public CanisterItem(Properties settings) {
        super(settings);
        var identifier = settings.itemIdOrThrow().identifier();
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> this.createItemAsset(builder, identifier));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (rotateFluids(player.getItemInHand(hand), player)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.use(level, player, hand);
    }

    private boolean rotateFluids(ItemStack stack, Player player) {
        var container = stack.get(FactoryDataComponents.FLUID);
        if (container != null && container.fluids().size() > 1) {
            stack.set(FactoryDataComponents.FLUID, container.rotateFluids(player.isShiftKeyDown()));
            player.level().playSound(null, player, FactorySoundEvents.ITEM_CANISTER_SHAKE_UP, SoundSource.PLAYERS, 0.5f, 1f);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        if (other.isEmpty() && clickAction == ClickAction.SECONDARY) {
            return rotateFluids(self, player);
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        FluidContainerUtil.tick(FluidContainerFromComponent.of(stack), world, entity.position().add(0, entity.getY() / 2, 0), 0,
                FactoryUtil.getItemConsumer(entity));
    }

    @Override
    public Component getName(ItemStack stack) {
        var container = stack.get(FactoryDataComponents.FLUID);

        if (container == null) {
            return super.getName(stack);
        } else if (container.isEmpty()) {
            return Component.translatable(this.getDescriptionId() + ".empty");
        } else if (container.fluids().size() == 1) {
            return Component.translatable(this.getDescriptionId() + ".typed", container.topFluid().getName());
        } else {
            return Component.translatable(this.getDescriptionId() + ".typed.more", container.topFluid().getName(), container.fluids().size() - 1);
        }
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        var fluids = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);

        if (fluids.capacity() < Integer.MAX_VALUE) {
            out.set(DataComponents.MAX_DAMAGE, (int) fluids.capacity());
            out.set(DataComponents.DAMAGE, (int) (fluids.capacity() - fluids.stored()));
        }

        //noinspection unchecked
        var x = (FluidInstance<Object>) fluids.topFluid();
        if (x != null && x.type().color().isPresent()) {
            out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true),
                    List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of(x.type().color().get().getColor(x.data()))));
        } else if (x != null) {
            out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true),
                    List.of(FactoryRegistries.FLUID_TYPES.getKey(x.type()).toString()), IntList.of(-1)));
        } else {
            out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(false),
                    List.of(), IntList.of()));
        }

        super.modifyBasePolymerItemStack(out, stack, context, lookup);
    }

    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState targetBlock, BlockPos pos, ServerPlayer player) {
        return true;
    }

    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (user instanceof Player) {
            Player player = (Player)user;
            if (player.getAbilities().instabuild) {
                return false;
            }
        }
        return super.canDestroyBlock(itemStack, state, level, pos, user);
    }

    private void createItemAsset(ResourcePackBuilder builder, Identifier identifier) {

        var fluidModelSwitch = SelectItemModel.builder(new CustomModelDataStringProperty(0))
                .fallback(EmptyItemModel.INSTANCE);

        for (var fluidType : FactoryRegistries.FLUID_TYPES.keySet()) {
            ItemModel model;
            var modelId = FactoryModels.CANISTER_ITEM.getModelId(fluidType);
            if (FactoryRegistries.FLUID_TYPES.getValue(fluidType).color().isPresent()) {
                model = new BasicItemModel(modelId, List.of(new CustomModelDataTintSource(0, -1)));
            } else {
                model = new BasicItemModel(modelId);
            }

            fluidModelSwitch.withCase(fluidType.toString(), model);
        }

        builder.addData(AssetPaths.itemAsset(identifier), new ItemAsset(
                new CompositeItemModel(List.of(
                        new BasicItemModel(identifier.withPrefix("item/").withSuffix("_inner"), Optional.of(new Transformation(new Matrix4f()
                                .translate(0.5f, 0.5f, 0.5f)
                                .scale(1, 1, 0.99f)
                                .translate(-0.5f, -0.5f, -0.5f)
                        )), List.of()),
                        new BasicItemModel(identifier.withPrefix("item/").withSuffix("_outer")),
                        fluidModelSwitch.transformation(new Matrix4f()
                                .translate(0.5f, 0.5f, 0.5f)
                                .scale(1, 1, 0.995f)
                                .translate(-0.5f, -0.5f, -0.5f)
                        ).build()
                )), ItemAsset.Properties.DEFAULT));
    }
}
