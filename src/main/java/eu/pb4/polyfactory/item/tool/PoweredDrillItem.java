package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.fluid.FactoryFluidTags;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.component.MiningMode;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PoweredDrillItem extends BaseDrillItem {
    private static final long BASE_USED_FUEL = FluidConstants.BLOCK / 600;

    public PoweredDrillItem(Properties properties) {
        super(properties);
    }

    private static long getUsedFuel(ItemStack stack) {
        return BASE_USED_FUEL * switch (stack.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE)) {
            case SINGLE -> 1;
            case AREA_2X2X1 -> 3;
            case AREA_3X3X1 -> 5;
        };
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
    }

    public InteractionResult handleBlockAttack(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player.getItemInHand(hand).is(this) && !hasFuel(player, player.getItemInHand(hand))) {
            player.sendOverlayMessage(Component.translatable("text.polyfactory.no_fuel"));
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean hasFuel(LivingEntity user, ItemStack itemStack) {
        var required = getUsedFuel(itemStack);
        for (var container : findFluidContainer(user)) {
            for (var fluid : container.fluids()) {
                if (fluid.is(FactoryFluidTags.DIESEL_ENGINE_FUEL)) {
                    required -= container.get(fluid);

                    if (required <= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected long getUsesFromFuel(LivingEntity user, ItemStack itemStack) {
        long total = 0;
        for (var container : findFluidContainer(user)) {
            for (var fluid : container.fluids()) {
                if (fluid.is(FactoryFluidTags.DIESEL_ENGINE_FUEL)) {
                    total += container.get(fluid);
                }
            }
        }
        return total / getUsedFuel(itemStack);
    }

    @Override
    protected void drainFuel(LivingEntity user, ItemStack itemStack) {
        var required = getUsedFuel(itemStack);

        for (var container : findFluidContainer(user)) {
            for (var fluid : container.fluids()) {
                if (fluid.is(FactoryFluidTags.DIESEL_ENGINE_FUEL)) {
                    required -= container.extract(fluid, required, false);

                    if (required <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private List<FluidContainer> findFluidContainer(LivingEntity user) {
        var stacks = new ArrayList<FluidContainer>();
        for (var eq : EquipmentSlot.values()) {
            var stack = user.getItemBySlot(eq);
            if (stack.is(FactoryItemTags.INVENTORY_FLUID_SOURCES)) {
                var fluid = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
                if (!fluid.isEmpty() && fluid.contains(FactoryFluidTags.DIESEL_ENGINE_FUEL)) {
                    stacks.add(FluidContainerFromComponent.of(SlotAccess.forEquipmentSlot(user, eq)));
                }
            }
        }

        if (user instanceof Player player) {
            for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.is(FactoryItemTags.INVENTORY_FLUID_SOURCES)) {
                    var fluid = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
                    if (!fluid.isEmpty() && fluid.contains(FactoryFluidTags.DIESEL_ENGINE_FUEL)) {
                        stacks.add(FluidContainerFromComponent.of(player.getInventory().getSlot(i)));
                    }
                }
            }
        }

        return stacks;
    }
}
