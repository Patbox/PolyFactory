package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.RegistryCallbackItem;
import eu.pb4.polyfactory.advancement.FluidShootsCriterion;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.shooting.EntityShooterContext;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerItem;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

public class PressureFluidGun extends Item implements PolymerItem {
    public PressureFluidGun(Properties settings) {
        super(settings);
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }


    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (entity instanceof LivingEntity livingEntity && livingEntity.getUseItem() != stack) {
            releaseUsing(stack, world, livingEntity, 0);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        //noinspection unchecked
        var fluid = (FluidInstance<Object>) stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid != null) {
            fluid.shootingBehavior().stopShooting(new EntityShooterContext(user), fluid);
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
        }
        return false;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        //noinspection unchecked
        var fluid = (FluidInstance<Object>) stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid == null) {
            return;
        }
        var ctx = new EntityShooterContext(user);

        var containers = findFluidContainer(user);
        if (containers.isEmpty() || !isUsable(stack)) {
            fluid.shootingBehavior().stopShooting(ctx, fluid);
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
            return;
        }

        for (var container : containers) {
            if (fluid.shootingBehavior().canShoot(ctx, fluid, container)) {
                fluid.shootingBehavior().continueShooting(ctx, fluid, -remainingUseTicks, container);
                if (remainingUseTicks % 20 == 0) {
                    stack.hurtAndBreak(1, user, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }

                var vec = ctx.rotation().scale(user.onGround() ? -0.002 : -0.05);
                FactoryUtil.addSafeVelocity(user, vec);
                if (user instanceof ServerPlayer player) {
                    FactoryUtil.sendVelocityDelta(player, vec);
                }
                return;
            }
        }
        fluid.shootingBehavior().stopShooting(ctx, fluid);
        stack.remove(FactoryDataComponents.CURRENT_FLUID);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if (stack.has(FactoryDataComponents.CURRENT_FLUID)) {
            return InteractionResult.CONSUME;
        }

        var containers = findFluidContainer(user);
        if (containers.isEmpty() || !isUsable(stack)) {
            return InteractionResult.FAIL;
        }

        var ctx = new EntityShooterContext(user);

        for (var container : containers) {
            //noinspection unchecked
            for (var f : ((List<FluidInstance<Object>>) (Object) container.fluids())) {
                if (f.shootingBehavior().canShoot(ctx, f, container)) {
                    stack.set(FactoryDataComponents.CURRENT_FLUID, f);
                    user.startUsingItem(hand);
                    f.shootingBehavior().startShooting(ctx, f, container);
                    var vec = ctx.rotation().scale(user.onGround() ? -0.01 : -0.07);
                    FactoryUtil.addSafeVelocity(user, vec);
                    if (user instanceof ServerPlayer player) {
                        FactoryUtil.sendVelocityDelta(player, vec);
                        FluidShootsCriterion.triggerFluidLauncher(player, stack, f);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.FAIL;
    }

    private List<FluidContainer> findFluidContainer(LivingEntity user) {
        var stacks = new ArrayList<FluidContainer>();
        for (var eq : EquipmentSlot.values()) {
            var stack = user.getItemBySlot(eq);
            var fluid = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            if (!fluid.isEmpty()) {
                stacks.add(FluidContainerFromComponent.of(SlotAccess.forEquipmentSlot(user, eq)));
            }
        }

        if (user instanceof Player player) {
            for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                var fluid = player.getInventory().getItem(i).getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
                if (!fluid.isEmpty()) {
                    stacks.add(FluidContainerFromComponent.of(player.getInventory().getSlot(i)));
                }
            }
        }

        return stacks;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        if (this.useCrossbowModel(itemStack)) {
            out.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(Items.ARROW.getDefaultInstance()));
            /*out.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, out.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                    .with(
                            EntityAttributes.GENERIC_ATTACK_SPEED,
                            new EntityAttributeModifier(id("packet_fluid_launcher"), -1000, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.ANY)
                    .withShowInTooltip(false)
            );*/
        }
        //out.set(DataComponentTypes.CONSUMABLE, new ConsumableComponent(99999f, UseAction.BOW, Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY), false, List.of()));
        out.set(DataComponents.DAMAGE, itemStack.get(DataComponents.DAMAGE));
        return out;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        if (this.useCrossbowModel(itemStack)) {
            return Items.CROSSBOW;
        }

        return Items.TRIAL_KEY;
    }

    private boolean useCrossbowModel(ItemStack itemStack) {
        return useActiveModel(itemStack) && PacketContext.get().getEncodedPacket() instanceof ClientboundSetEquipmentPacket;
    }

    private boolean useActiveModel(ItemStack itemStack) {
        return itemStack.has(FactoryDataComponents.CURRENT_FLUID);
    }
}
